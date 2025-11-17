## Prometheus + Grafana 모니터링 가이드

이미 **EC2 Front/Back + RDS** 배포가 끝났다는 가정으로,
여기서는 **모니터링 EC2 + Prometheus + Grafana** 부분만 정리합니다.

---

### 1. 전체 구조

- Back EC2 (이미 존재)
  - Spring Boot 애플리케이션이 `8888` 포트에서 실행 중
  - `/actuator/prometheus` 엔드포인트에서 메트릭 제공 (Micrometer + Prometheus 설정 필요)
- Monitoring EC2 (새로 생성)
  - Prometheus 서버: `9090` 포트
  - Grafana 서버: `3000` 포트
- 보안그룹
  - `SG-backend`: 8888 포트를 `SG-frontend`와 `SG-monitor`에서만 허용
  - `SG-monitor`: 22, 9090, 3000 포트를 **내 IP만** 허용

핵심 아이디어:

- **Prometheus → Back EC2 `/actuator/prometheus`** 로 메트릭을 긁어옴
- **Grafana → Prometheus** 를 데이터소스(Source)로 사용해서 대시보드 그림

---

### 2. 보안그룹 설정

#### 2-1. `SG-monitor` (Monitoring EC2용)

- Inbound
  - 22/TCP: `내 IP` (SSH 접속용)
  - 9090/TCP: `내 IP` (Prometheus 웹 콘솔; 필요 없으면 안 열어도 됨)
  - 3000/TCP: `내 IP` (Grafana 웹 화면)
- Outbound
  - All (기본값)

#### 2-2. `SG-backend` 수정

이미 `SG-frontend`에서 8888 포트를 허용해 둔 상태에서, 아래 규칙 추가:

- Inbound 추가
  - 8888/TCP: **소스 = SG-monitor**

이렇게 하면 **Monitoring EC2 → Back EC2:8888** 로만 메트릭을 가져올 수 있고,
8888 포트는 여전히 외부 전체(0.0.0.0/0)에는 닫혀 있습니다.

---

### 3. 백엔드 `/actuator/prometheus` 확인

먼저 Back EC2에서 메트릭 엔드포인트가 잘 나오는지 확인합니다.

```bash
ssh -i ~/Downloads/my-key.pem ubuntu@<BACK_PUBLIC_IP>

curl http://localhost:8888/actuator/health
curl http://localhost:8888/actuator/prometheus
```

- 첫 번째: 헬스 상태 JSON
- 두 번째: Prometheus 포맷의 텍스트(많은 줄) → 나오면 OK

만약 `/actuator/prometheus` 가 404 또는 접근 불가라면, `application.yml` 에서 Actuator 노출 설정을 확인합니다.

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: never
```

수정 후에는 백엔드 서비스를 재시작해야 적용됩니다.

```bash
sudo systemctl restart my-backend
sudo journalctl -u my-backend -f
```

> 보안 포인트
>
> - `/actuator/prometheus`는 **Back EC2 내부에서만 접근** 가능하게 두고,
>   인터넷에서 바로 열지 않습니다.

---

### 4. Monitoring EC2 생성

AWS 콘솔에서 새 EC2 인스턴스를 만듭니다.

- OS: Ubuntu 22.04 LTS
- 타입: t2.micro (프리티어)
- VPC: 기존 EC2, RDS와 동일 VPC
- 보안그룹: `SG-monitor`
- 퍼블릭 IP: 할당 (혹은 EIP 바인딩)

생성 후 SSH 접속:

```bash
ssh -i ~/Downloads/my-key.pem ubuntu@<MONITOR_PUBLIC_IP>
```

이제 이 서버 위에 Prometheus와 Grafana를 설치합니다.

---

### 5. Prometheus 설치 및 설정

#### 5-1. Prometheus 바이너리 설치

```bash
ssh -i ~/Downloads/my-key.pem ubuntu@<MONITOR_PUBLIC_IP>

cd /tmp
curl -LO https://github.com/prometheus/prometheus/releases/download/v2.55.1/prometheus-2.55.1.linux-amd64.tar.gz
tar xvf prometheus-2.55.1.linux-amd64.tar.gz
sudo mv prometheus-2.55.1.linux-amd64 /opt/prometheus

sudo useradd --no-create-home --shell /usr/sbin/nologin prometheus
sudo chown -R prometheus:prometheus /opt/prometheus
```

#### 5-2. `prometheus.yml` 작성

`<BACK_PRIVATE_IP>` 는 **Back EC2의 VPC 내부 IP** 입니다.

```bash
sudo vi /opt/prometheus/prometheus.yml
```

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'spring-boot-backend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['<BACK_PRIVATE_IP>:8888']
```

설명:

- `scrape_interval`: 몇 초마다 메트릭을 가져올지 (15초마다)
- `job_name`: Grafana에서 구분할 이름
- `targets`: Prometheus가 메트릭을 가져올 대상 (Back EC2)

#### 5-3. 데이터 디렉터리 생성

Prometheus가 메트릭과 쿼리 로그를 저장할 디렉터리를 만들어 줍니다.

```bash
sudo mkdir -p /opt/prometheus/data
sudo chown -R prometheus:prometheus /opt/prometheus
```

#### 5-4. systemd 서비스 등록

```bash
sudo vi /etc/systemd/system/prometheus.service
```

```ini
[Unit]
Description=Prometheus
After=network.target

[Service]
User=prometheus
Group=prometheus
Type=simple
ExecStart=/opt/prometheus/prometheus \
  --config.file=/opt/prometheus/prometheus.yml \
  --storage.tsdb.path=/opt/prometheus/data

Restart=always

[Install]
WantedBy=multi-user.target
```

서비스 시작:

```bash
sudo systemctl daemon-reload
sudo systemctl enable prometheus
sudo systemctl start prometheus
sudo systemctl status prometheus
```

테스트:

```bash
curl http://localhost:9090/metrics | head
```

웹 브라우저에서 (SG에 9090 포트를 열어둔 경우):

- `http://<MONITOR_PUBLIC_IP>:9090`
- 상단 메뉴 → `Status` → `Targets` → `spring-boot-backend`가 `UP` 상태인지 확인

---

### 6. Grafana 설치 및 설정

#### 6-1. Grafana 설치

Monitoring EC2에서:

```bash
sudo apt-get update -y
sudo apt-get install -y adduser libfontconfig1
wget https://dl.grafana.com/oss/release/grafana_11.1.4_amd64.deb
sudo dpkg -i grafana_11.1.4_amd64.deb  # 여기서 의존성 에러가 나면 아래 명령 실행
sudo apt --fix-broken install -y       # 누락된 패키지 자동 설치 후 다시 dpkg 필요 X
sudo systemctl enable grafana-server
sudo systemctl start grafana-server
```

`SG-monitor` 보안그룹에서 3000 포트가 **내 IP만 허용**되어 있는지 다시 확인합니다.

#### 6-2. Grafana 접속

브라우저에서 접속:

- 주소: `http://<MONITOR_PUBLIC_IP>:3000`
- 기본 계정: `admin` / `admin`
- 첫 로그인 시 비밀번호를 강력한 값으로 변경

#### 6-3. Prometheus 데이터소스 연결

1. Grafana 왼쪽 메뉴에서 `Configuration` (톱니바퀴 아이콘) → `Data sources`
2. `Add data source` 버튼 클릭
3. 타입 선택: `Prometheus`
4. URL 입력: `http://localhost:9090`
5. 맨 아래 `Save & Test` 클릭 → `Data source is working` 메시지 확인

#### 6-4. 대시보드 가져오기

1. 왼쪽 메뉴 → `Dashboards` → `Import`
2. Grafana.com 대시보드 ID 입력 (Spring Boot(12900)/Micrometer JVM(4701) 하나 선택)
3. `Prometheus` 데이터소스 선택 후 `Import`

이제 HTTP 요청 수, 에러 비율, JVM 메모리/쓰레드, DB 커넥션 등 다양한 그래프를 볼 수 있습니다.

---

### 7. 모니터링 + 보안 요약

- **백엔드 포트(8888)**
  - 외부 전체(0.0.0.0/0)에는 절대 열지 말 것
  - `SG-frontend`, `SG-monitor` 에서만 허용
- **Prometheus 웹(9090)**
  - 가능하면 내 IP만 허용 (혹은 아예 외부에서 안 열고, SSH 터널로만 접속)
- **Grafana 웹(3000)**
  - 내 IP만 허용 + 기본 계정 비번 강하게 변경
- `/actuator/prometheus` 엔드포인트
  - Back EC2 내부에서만 직접 호출 가능
  - Prometheus가 백엔드에 접근하는 통로일 뿐, 인터넷에 공개되면 안 됨

---

### 8. 확인 체크리스트

1. Back EC2
   - `curl http://localhost:8888/actuator/prometheus` 결과가 잘 나오는지
2. Monitoring EC2
   - `curl http://localhost:9090/metrics` 정상 응답
   - Prometheus UI `Status → Targets` 에서 `spring-boot-backend` 상태가 `UP`
3. Grafana
   - `http://<MONITOR_PUBLIC_IP>:3000` 접속 가능 (내 IP에서만)
   - Prometheus 데이터소스 `Save & Test` 성공
   - 대시보드에서 HTTP, JVM 등의 그래프가 움직이는지 확인
