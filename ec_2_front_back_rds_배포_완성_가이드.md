# EC2 FRONT/BACK 두대 + RDS 완성 배포 가이드 (Ubuntu)

구성: **EC2 2대 (Front/Back)** + **RDS(MariaDB, Public access: Yes)** + **도메인 + TLS(Let’s Encrypt)** + **Nginx(정적/프록시)** + **Spring Boot JAR(app.jar)**

---

## 0) 아키텍처 & 준비물

* **도메인**: 예) `hwiyeong.shop` (A 레코드를 Front EC2 공인 IP로 지정)
* **리전/VPC**: EC2 2대와 RDS는 **같은 VPC/리전**
* **키페어**: SSH 접속용 `.pem`
* **아티팩트**

  * 프론트: `dist/` (빌드 산출물)
  * 백엔드: **`app.jar`** (Spring Boot 빌드 산출물 파일명 고정)
* **환경변수(.env)** (백엔드 서버에 둘 값, 아래 예시 사용)

---

## 1) 보안그룹 만들기 (SG-frontend, SG-backend, SG-rds)

### 1-1. SG-frontend (프론트 EC2용)

**Inbound**

* 80/TCP: `0.0.0.0/0`
* 443/TCP: `0.0.0.0/0`
* 22/TCP: `0.0.0.0/0`
  **Outbound**
* All (기본)

### 1-2. SG-backend (백엔드 EC2용)

**Inbound**

* 8888/TCP: **소스 = SG-frontend** (보안그룹 참조)
* 22/TCP: `0.0.0.0/0`
  **Outbound**
* All (기본)

### 1-3. SG-rds (RDS용)

**Inbound**

* 3306/TCP: **소스 = SG-backend** (보안그룹 참조)
  **Outbound**
* All (기본)

> 포인트: **백엔드 포트(8888)는 외부 전체 허용 X**, 프론트 SG만 허용.

---

## 2) RDS(MariaDB) 만들기

* RDS 콘솔 → **Create database** 클릭
* **데이터베이스 생성 방식**
  * **손쉬운 생성** 선택 
* **엔진 유형**: **MariaDB** 선택
* **DB 인스턴스 크기**: **프리 티어** 선택
* **DB 인스턴스 식별자**: `hotel` (원하는 이름 입력)
* **마스터 사용자 이름**: `admin` (기본값 사용 권장)
* **마스터 암호**: 강력한 비밀번호 설정 → **반드시 따로 저장**
* **마스터 암호 확인**: 동일한 비밀번호 재입력
* **자격 증명 관리**
  * **자체 관리** 선택 (Self managed)
* **VPC**: EC2와 동일한 VPC 선택 (기본 VPC 사용 시 default)
* **퍼블릭 액세스**: **예** 선택
* **기존 VPC 보안 그룹**: `SG-rds` 선택 (추가로 선택, 기본 그룹은 제거 가능)
* **Create database** 클릭

> **손쉬운 생성**을 사용하면 AWS가 자동으로 최적 설정을 적용합니다.
> **자체 관리**를 선택하면 AWS Secrets Manager 없이 직접 암호를 관리합니다.

생성 후 **엔드포인트** 확인 (몇 분 소요):

* RDS 콘솔 → Databases → `hotel` 클릭 → **Connectivity & security** 탭
* **엔드포인트**: 예) `hotel.xxxx.ap-northeast-2.rds.amazonaws.com`
* **포트**: `3306` 확인

초기 데이터베이스 생성 및 스키마 적용:

```bash
sudo apt-get update -y
sudo apt-get install -y mariadb-client

# 데이터베이스 생성
mariadb -h <RDS_ENDPOINT> -u <DB_USERNAME> -p
CREATE DATABASE IF NOT EXISTS hotel CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
\q

# (선택사항) db.md 파일로 전체 스키마 및 초기 데이터 적용
# 프로젝트의 db.md 파일을 서버에 업로드한 후:
# mariadb -h <RDS_ENDPOINT> -u <DB_USERNAME> -p hotel < db.md

# 참고: application.yml에 ddl-auto: update 설정 시
# Spring Boot가 엔티티 기반으로 테이블을 자동 생성하므로
# db.md 적용은 샘플 데이터가 필요한 경우에만 사용
```

---

## 3) EC2 2대 생성 (Front, Back)

공통: Ubuntu 22.04 LTS, **t2.micro** (프리 티어)

### 3-1. Front EC2

* 퍼블릭 IP 할당(또는 EIP 바인딩 권장)
* **보안그룹**: SG-frontend
* 키페어: 기존/신규

### 3-2. Back EC2

* 퍼블릭 IP 할당(선택) — 운영 접근 편의용
* **보안그룹**: SG-backend

---

## 4) 도메인 → Front EC2 연결

DNS A 레코드 설정:

* `hwiyeong.shop` → Front EC2 **공인 IP**

전파 확인:

```bash
dig +short hwiyeong.shop
```

---

## 5) Front EC2 설정 (Nginx + TLS + 정적 배포 + 프록시)

SSH 접속:

```bash
ssh -i ~/Downloads/my-key.pem ubuntu@<FRONT_PUBLIC_IP>
```

### 5-1. 기본 패키지

```bash
sudo apt-get update -y
sudo apt-get install -y nginx curl git unzip rsync
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw --force enable
sudo systemctl enable nginx --now
```

### 5-2. Nginx 설정 (HTTP 먼저 구성)

**먼저 백엔드 EC2의 Private IP 확인:**

```bash
# AWS CLI로 확인 (로컬 또는 프론트 EC2에서)
BACK_PRIVATE_IP=$(aws ec2 describe-instances \
  --instance-ids <BACK_INSTANCE_ID> \
  --query 'Reservations[0].Instances[0].PrivateIpAddress' \
  --output text)
echo "백엔드 Private IP: $BACK_PRIVATE_IP"

# 또는 백엔드 EC2에 SSH 접속하여 확인
# ssh -i my-key.pem ubuntu@<BACK_PUBLIC_IP>
# hostname -I | awk '{print $1}'
```

Nginx 설정 파일 생성:

```bash
sudo vi /etc/nginx/sites-available/hwiyeong.shop
```

**주의: 아래 설정에서 `<BACK_PRIVATE_IP>`를 위에서 확인한 실제 프라이빗 IP로 변경**

```nginx
server {
    listen 80;
    server_name hwiyeong.shop;
    root /var/www/hotel;
    index index.html;

    client_max_body_size 10m;

    # SPA 라우팅
    location / {
        try_files $uri /index.html;
    }

    # 백엔드 주소 (백엔드 EC2의 사설 IP와 포트로 변경 필요)
    set $backend http://<BACK_PRIVATE_IP>:8888;

    # API 프록시
    location ^~ /api/ {
        proxy_pass $backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Authorization $http_authorization;
        proxy_connect_timeout 10s;
        proxy_read_timeout 60s;
    }

    # 업로드 리소스
    location ^~ /uploads/ {
        proxy_pass $backend/uploads/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 10s;
        proxy_read_timeout 60s;
    }

    # 소셜 로그인 프록시
    location ^~ /api/oauth2/authorization/ {
        proxy_pass $backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location ^~ /login/oauth2/ {
        proxy_pass $backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

적용:

```bash
sudo ln -sf /etc/nginx/sites-available/hwiyeong.shop /etc/nginx/sites-enabled/hwiyeong.shop

# 백엔드 IP가 올바르게 설정되었는지 확인
grep "set \$backend" /etc/nginx/sites-available/hwiyeong.shop

sudo nginx -t && sudo systemctl reload nginx
```

### 5-3. Let's Encrypt(Certbot) - HTTPS 적용

```bash
sudo snap install core; sudo snap refresh core
sudo snap install --classic certbot
sudo ln -sf /snap/bin/certbot /usr/bin/certbot

# 실제 이메일 주소로 교체 필요 (인증서 만료 알림 수신용)
sudo certbot --nginx -d hwiyeong.shop \
  --agree-tos -m admin@hwiyeong.shop \
  --redirect --non-interactive

# 자동 갱신 타이머 확인
sudo systemctl list-timers | grep certbot
```

### 5-4. 프론트 정적 파일 배포

로컬 빌드 → 서버 복사:

```bash
# 로컬 (프로젝트 루트)
npm ci
npm run build  # dist/ 생성

# Windows에서 업로드
echo 업로드 예: scp -i "pem키_경로\\my-key.pem" -r dist/* ubuntu@<FRONT_PUBLIC_IP>:/tmp/hotel-dist/

# 서버에서
sudo mkdir -p /var/www/hotel
sudo rsync -av --delete /tmp/hotel-dist/ /var/www/hotel/
sudo chown -R www-data:www-data /var/www/hotel
sudo systemctl reload nginx
```

체크:

```bash
curl -I https://hwiyeong.shop
```

---

## 6) Back EC2 설정 (Java, 서비스, .env, JAR)

SSH:

```bash
ssh -i ~/Downloads/my-key.pem ubuntu@<BACK_PUBLIC_IP>
```

### 6-1. Java & 디렉터리

```bash
sudo apt-get update -y
sudo apt-get install -y openjdk-17-jre-headless unzip
java -version  # 17.x 확인

# 디렉터리 구조 생성 및 권한 설정
sudo mkdir -p /opt/my-backend/releases /opt/my-backend/shared/logs /opt/my-backend/uploads
sudo chown -R ubuntu:ubuntu /opt/my-backend
sudo chmod 755 /opt/my-backend/uploads

# 디렉터리 구조 확인
ls -la /opt/my-backend/
```

### 6-2. **.env 생성 (요청한 예시로 교체 적용)**

```bash
sudo vi /opt/my-backend/.env
```

아래 **그대로** 붙여넣기:

```env
# ────────────────────────────────
# 🗄️  데이터베이스 설정
# ────────────────────────────────
DB_HOST=your-rds-host
DB_PORT=3306
DB_USERNAME=your-db-username
DB_PASSWORD=your-db-password

# ────────────────────────────────
# 🔑 OAuth2 설정 (Naver)
# ────────────────────────────────
NAVER_CLIENT_ID=your-naver-client-id
NAVER_CLIENT_SECRET=your-naver-client-secret

# ────────────────────────────────
# 🔑 OAuth2 설정 (Google)
# ────────────────────────────────
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# ────────────────────────────────
# 🔑 OAuth2 설정 (Kakao)
# ────────────────────────────────
KAKAO_CLIENT_ID=your-kakao-client-id
KAKAO_CLIENT_SECRET=your-kakao-client-secret

# ────────────────────────────────
# 🔐 JWT 설정
# ────────────────────────────────
JWT_SECRET=your-jwt-secret-key-min-64-characters

# ────────────────────────────────
# 📧 메일 설정 (SendGrid SMTP)
# ────────────────────────────────
MAIL_HOST=smtp.sendgrid.net
MAIL_PORT=587
MAIL_USERNAME=apikey
MAIL_PASSWORD=your-sendgrid-api-key
MAIL_FROM=support@your-domain.com
MAIL_FROM_NAME=Your App Name
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true

# ────────────────────────────────
# 💳 토스페이먼츠 API 설정
# ────────────────────────────────
TOSS_PAYMENTS_SECRET_KEY=your-toss-secret-key
TOSS_PAYMENTS_CLIENT_KEY=your-toss-client-key

# ────────────────────────────────
# 🤖 reCAPTCHA 설정
# ────────────────────────────────
VITE_RECAPTCHA_SITE_KEY=your-recaptcha-site-key
RECAPTCHA_SECRET=your-recaptcha-secret-key
RECAPTCHA_VERIFY_URL=https://www.google.com/recaptcha/api/siteverify

# ────────────────────────────────
# 🌐 CORS & API 설정
# ────────────────────────────────
CORS_ALLOWED_ORIGINS=https://your-domain.com
VITE_API_BASE=https://your-domain.com/api
```

권한 설정 및 환경변수 값 교체:

```bash
sudo chown ubuntu:ubuntu /opt/my-backend/.env
chmod 600 /opt/my-backend/.env

# ⚠️ 중요: 플레이스홀더 값을 실제 키로 교체 필요
grep "your-" /opt/my-backend/.env

# 각 서비스별 키 발급 방법:
# ────────────────────────────────────────────────
# DB 설정: RDS 엔드포인트 및 생성 시 설정한 계정 정보
#   DB_HOST=hotel.xxxx.ap-northeast-2.rds.amazonaws.com
#   DB_USERNAME=admin
#   DB_PASSWORD=<RDS 생성 시 설정한 비밀번호>
#
# OAuth2 키 발급:
#   Google: https://console.cloud.google.com/apis/credentials
#   Naver: https://developers.naver.com/apps
#   Kakao: https://developers.kakao.com/console/app
#
# JWT Secret 생성:
#   openssl rand -base64 64
#
# Toss Payments: https://developers.tosspayments.com/
#   (테스트 키/라이브 키 구분 필요)
#
# SendGrid API Key: https://app.sendgrid.com/settings/api_keys
#
# reCAPTCHA: https://www.google.com/recaptcha/admin
#   (v2 또는 v3 선택)
#
# CORS & API:
#   CORS_ALLOWED_ORIGINS=https://hwiyeong.shop
#   VITE_API_BASE=https://hwiyeong.shop/api
# ────────────────────────────────────────────────

# 예시: sed를 사용한 일괄 교체
# sed -i 's/your-rds-host/hotel.xxxx.ap-northeast-2.rds.amazonaws.com/g' /opt/my-backend/.env
# sed -i 's/your-db-username/admin/g' /opt/my-backend/.env
# sed -i 's/your-jwt-secret-key-min-64-characters/실제_생성한_64자_이상_시크릿/g' /opt/my-backend/.env
```

### 6-3. JAR 업로드 (파일명: **app.jar**)

```bash
# 로컬에서 빌드 후 업로드 (예시)
# Windows PowerShell
scp -i "pem키_경로\\my-key.pem" "C:\\path\\to\\app.jar" ubuntu@<BACK_PUBLIC_IP>:/opt/my-backend/

# 서버에서 심볼릭 링크
ln -sf /opt/my-backend/app.jar /opt/my-backend/current.jar
```

### 6-4. systemd 서비스 등록 (app.jar 기준)

```bash
sudo vi /etc/systemd/system/my-backend.service
```

```ini
[Unit]
Description=My Backend (Spring Boot)
After=network.target

[Service]
User=ubuntu
WorkingDirectory=/opt/my-backend
EnvironmentFile=/opt/my-backend/.env
ExecStart=/usr/bin/java -jar /opt/my-backend/current.jar
Environment=JAVA_TOOL_OPTIONS=-XX:+UseZGC
Restart=always
RestartSec=5
StartLimitInterval=300
StartLimitBurst=5
SuccessExitStatus=143
StandardOutput=append:/opt/my-backend/shared/logs/app.log
StandardError=append:/opt/my-backend/shared/logs/app.err

[Install]
WantedBy=multi-user.target
```

적용/시작:

```bash
sudo systemctl daemon-reload
sudo systemctl enable my-backend
sudo systemctl start my-backend
sudo journalctl -u my-backend -f
```

포트 확인:

```bash
ss -lntp | grep 8888
```

---

## 7) Spring 설정 체크포인트

* `server.port: 8888`
* `server.forward-headers-strategy: framework` (HTTPS 인식, yml에서 설정했다면 OK)
* DB/JWT/CORS/파일 URL은 `.env` 또는 yml에서 일관되게 반영
* Actuator Health 노출: `/actuator/health` (Nginx 경유: `/api/actuator/health`)

---

## 8) OAuth 리다이렉트/콜백 등록

각 제공자 콘솔에 아래 **정확히 등록**:

* Google : `https://hwiyeong.shop/login/oauth2/code/google`
* Naver  : `https://hwiyeong.shop/login/oauth2/code/naver`
* Kakao  : `https://hwiyeong.shop/login/oauth2/code/kakao`

시작 URL(리다이렉트 트리거):

* `https://hwiyeong.shop/api/oauth2/authorization/google` (naver/kakao 동일 패턴)

> 백엔드 `application.yml`의 `redirect-uri`와 **완전 일치**해야 함.

---

## 9) 최종 점검

```bash
# 1. 환경변수 로드 확인 (백엔드 EC2에서)
sudo journalctl -u my-backend -n 100 | grep -i "env\|property\|datasource"

# 2. 업로드 디렉터리 확인
ls -la /opt/my-backend/uploads
curl -I https://hwiyeong.shop/uploads/

# 3. 프론트 정적 페이지
curl -I https://hwiyeong.shop

# 4. 백엔드 헬스체크 (프론트 프록시 경유)
curl -sS https://hwiyeong.shop/api/actuator/health | jq

# 5. OAuth 시작 (302 리다이렉트 나오면 정상)
curl -i https://hwiyeong.shop/api/oauth2/authorization/google

# 6. 인증 필요한 API (토큰 없으면 401/403 정상)
curl -i https://hwiyeong.shop/api/owner/reservations

# 7. 백엔드 로그에서 에러 확인
sudo journalctl -u my-backend --since "10 minutes ago" | grep -i error

# 8. Nginx 에러 로그 확인
sudo tail -f /var/log/nginx/error.log
```

브라우저 DevTools → **Network** 에서 `/api/**` 요청에 `Authorization: Bearer <JWT>`가 붙는지 확인.

---

## 10) 트러블슈팅 메모

* **404 Not found: login/oauth2/code/...**

  * Nginx에 `/login/oauth2/` 프록시 포함 여부 확인
  * OAuth 콘솔 Redirect URI 오탈자(https/도메인/경로) 확인
* **500 MissingRequestHeader 'Authorization'**

  * 프론트에서 토큰 미주입 or Nginx 헤더 드랍
  * Nginx에 `proxy_set_header Authorization $http_authorization;` 설정 확인
  * 컨트롤러에서 `@RequestHeader("Authorization")`를 모든 경로에 강제하지 않았는지 확인
* **DB 연결 실패**

  * RDS SG Inbound = **SG-backend**인지, 백엔드에서 `mariadb -h` 접속 테스트
* **CORS 에러**

  * 프론트 도메인만 허용하도록 설정. OPTIONS 프리플라이트 허용.
  * `.env`의 `CORS_ALLOWED_ORIGINS` 확인
* **TLS 갱신**

  * certbot timer 기본 활성화. 확인: `systemctl list-timers | grep certbot`
* **파일 업로드 실패**

  * `/opt/my-backend/uploads` 권한 확인: `sudo chown -R ubuntu:ubuntu /opt/my-backend/uploads`
  * Nginx `client_max_body_size` 확인
* **환경변수 로드 실패**

  * `/opt/my-backend/.env` 존재/권한 확인
  * Spring Boot에서 `import: optional:file:.env` 사용 시 로그로 로드 확인

---

## 11) 운영 팁

### 11-1. 기본 설정

* 시간대: `sudo timedatectl set-timezone Asia/Seoul`
* 서비스 자동재시작: systemd `Restart=always` (5분 내 5회 재시작 제한)
* 로그
  * Nginx: `/var/log/nginx/{access,error}.log`
  * 백엔드: `journalctl -u my-backend -f`, `/opt/my-backend/shared/logs/app.log`

### 11-2. 헬스체크 스크립트

백엔드 EC2에 헬스체크 스크립트 생성:

```bash
sudo vi /opt/my-backend/healthcheck.sh
```

```bash
#!/bin/bash
set -euo pipefail

HEALTH_URL="http://localhost:8888/actuator/health"
RESPONSE=$(curl -sf "$HEALTH_URL" 2>&1 || echo "FAILED")

if echo "$RESPONSE" | grep -q '"status":"UP"'; then
  echo "[$(date +'%Y-%m-%d %H:%M:%S')] Backend is healthy"
  exit 0
else
  echo "[$(date +'%Y-%m-%d %H:%M:%S')] Backend is down! Response: $RESPONSE"
  exit 1
fi
```

```bash
sudo chmod +x /opt/my-backend/healthcheck.sh
sudo chown ubuntu:ubuntu /opt/my-backend/healthcheck.sh

# 테스트 실행
/opt/my-backend/healthcheck.sh

# (선택) cron으로 주기적 체크 및 알림
# crontab -e
# */5 * * * * /opt/my-backend/healthcheck.sh >> /opt/my-backend/shared/logs/health.log 2>&1
```

### 11-3. 로그 로테이션

```bash
sudo vi /etc/logrotate.d/my-backend
```

```
/opt/my-backend/shared/logs/*.log {
    daily
    rotate 14
    compress
    delaycompress
    missingok
    notifempty
    create 0640 ubuntu ubuntu
    sharedscripts
    postrotate
        systemctl reload my-backend > /dev/null 2>&1 || true
    endscript
}
```

---

### JAR 업데이트 절차 (app.jar)

```bash
# 1. 새 JAR 업로드
scp -i my-key.pem app.jar ubuntu@<BACK_IP>:/opt/my-backend/

# 2. 심볼릭 링크 갱신
ssh -i my-key.pem ubuntu@<BACK_IP>
ln -sf /opt/my-backend/app.jar /opt/my-backend/current.jar

# 3. 서비스 재시작
sudo systemctl restart my-backend
sudo journalctl -u my-backend -f  # 로그 확인
```
