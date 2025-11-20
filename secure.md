# Hotel 프로젝트 보안 강화 보고서

**작성일**: 2025-10-21  
**브랜치**: feature/xss, feature/RECAPTCHA  
**작성자**: GitHub Copilot

---

## 📋 개요

본 보고서는 Hotel 프로젝트에 적용된 보안 강화 조치를 정리한 문서입니다. 초기 reCAPTCHA 기능만 존재하던 상태에서, XSS(Cross-Site Scripting) 방어 메커니즘과 추가 보안 설정이 구현되었습니다.

---

## 🔐 1. reCAPTCHA 봇 방어 시스템

### 1.1 프론트엔드 구현

**파일**: `hotel-web/src/components/user/login_page/Login.vue`

**적용 내용**:
- Google reCAPTCHA v2 위젯 통합
- 동적 사이트 키 로딩 (`/api/auth/recaptcha/site-key` 엔드포인트)
- 로그인 시 reCAPTCHA 토큰 검증 필수화
- 사용자 친화적 에러 메시지 제공
- 토큰 만료 시 자동 재인증 유도

**주요 기능**:
```javascript
// reCAPTCHA 렌더링
renderRecaptcha() {
  this.recaptchaWidgetId = window.grecaptcha.render(this.$refs.recaptcha, {
    sitekey: this.recaptchaSiteKey,
    callback: this.onRecaptchaSuccess,
    'expired-callback': this.onRecaptchaExpired,
    'error-callback': this.onRecaptchaError
  });
}

// 로그인 요청 시 토큰 포함
const response = await http.post('/auth/login', {
  username: this.username,
  password: this.password,
  recaptchaToken: this.recaptchaToken
});
```

### 1.2 백엔드 구현

**파일**: `my-backend/src/main/java/com/example/backend/authlogin/controller/LoginController.java`

**적용 내용**:
- reCAPTCHA 사이트 키 제공 엔드포인트 (`/api/auth/recaptcha/site-key`)
- Google reCAPTCHA API를 통한 서버 측 토큰 검증
- 환경변수 기반 설정 관리 (`RECAPTCHA_SITE_KEY`, `RECAPTCHA_SECRET`)
- 검증 실패 시 로그인 차단

**검증 로직**:
```java
@Value("${RECAPTCHA_SITE_KEY:${VITE_RECAPTCHA_SITE_KEY:}}")
private String recaptchaSiteKey;

private boolean verifyRecaptcha(String token) {
    // Google reCAPTCHA API 호출
    // score 기반 검증 (0.5 이상 통과)
}
```

### 1.3 환경 설정

**파일**: `my-backend/.env`

```properties
RECAPTCHA_SITE_KEY=6LcBuugrAAAAACQCkfNiJBiaGyl9RY40e8hQylUR
VITE_RECAPTCHA_SITE_KEY=6LcBuugrAAAAACQCkfNiJBiaGyl9RY40e8hQylUR
RECAPTCHA_SECRET=6LcBuugrAAAAAHW_MLSYS8yB9ieUyoaV4pc4odId
RECAPTCHA_VERIFY_URL=https://www.google.com/recaptcha/api/siteverify
```

---

## 🛡️ 2. XSS(Cross-Site Scripting) 방어 시스템

### 2.1 프론트엔드 XSS 방어

#### 2.1.1 DOMPurify 기반 HTML 정화

**파일**: `hotel-web/src/composables/useSafeHtml.js`

**적용 내용**:
- DOMPurify 라이브러리를 활용한 HTML 정화 유틸리티
- 기본 프로필: HTML 태그만 허용 (`USE_PROFILES: { html: true }`)
- 스크립트, 이벤트 핸들러 자동 제거

```javascript
import DOMPurify from 'dompurify';

const DEFAULT_PROFILE = Object.freeze({ USE_PROFILES: { html: true } });

export function sanitizeHtml(input, options = DEFAULT_PROFILE) {
  const value = typeof input === 'function' ? input() : input;
  if (!value) return '';
  return DOMPurify.sanitize(value, options);
}
```

#### 2.1.2 SafeHtml 컴포넌트

**파일**: `hotel-web/src/components/common/SafeHtml.vue`

**적용 내용**:
- 재사용 가능한 안전한 HTML 렌더링 컴포넌트
- 모든 `v-html` 사용을 이 컴포넌트로 대체
- computed를 통한 자동 정화

```vue
<script setup>
import { computed, toRefs } from 'vue';
import { sanitizeHtml } from '@/composables/useSafeHtml';

const props = defineProps({
  content: { type: String, default: '' },
  tag: { type: String, default: 'div' },
  sanitizeOptions: { type: Object, default: undefined }
});

const sanitizedHtml = computed(() => 
  sanitizeHtml(() => props.content, sanitizeOptions.value)
);
</script>

<template>
  <component :is="tag" v-html="sanitizedHtml"></component>
</template>
```

#### 2.1.3 적용 파일 목록

**수정된 파일**:
1. **`hotel-web/src/components/owner/OwnerHotelEdit.vue`**
   - 편의시설 아이콘 렌더링에 SafeHtml 적용
   - 기존: `<span class="ic" v-html="a.ic"></span>`
   - 변경: `<SafeHtml class="ic" tag="span" :content="a.ic" />`

**검증 결과**:
- 전체 Vue 파일 스캔 결과, 직접 `v-html` 사용은 `SafeHtml.vue` 내부에만 존재
- `innerHTML` 직접 조작 코드 없음

### 2.2 백엔드 XSS 방어

#### 2.2.1 이메일 템플릿 HTML 이스케이프

**파일**: `my-backend/src/main/java/com/example/backend/admin/service/AdminPaymentNotificationService.java`

**적용 내용**:
- HTML 이메일 생성 시 사용자 입력 데이터 이스케이프
- 수동 구현된 `escapeHtml` 메서드로 위험 문자 치환

```java
private String escapeHtml(String input) {
    if (input == null) return "";
    StringBuilder builder = new StringBuilder(input.length());
    for (char c : input.toCharArray()) {
        switch (c) {
            case '&' -> builder.append("&amp;");
            case '<' -> builder.append("&lt;");
            case '>' -> builder.append("&gt;");
            case '"' -> builder.append("&quot;");
            case '\'' -> builder.append("&#39;");
            default -> builder.append(c);
        }
    }
    return builder.toString();
}
```

**적용 위치**:
- 결제 알림 메일 제목, 본문
- 고객명, 호텔명 등 사용자 입력 데이터
- 영수증 URL, 추가 메시지 등

```java
// 사용 예시
+ "<p>안녕하세요, " + escapeHtml(resolveCustomerName(payment)) + " 고객님.</p>"
+ "<a href=\"" + escapeHtml(payment.getReceiptUrl()) + "\">영수증 보기</a>"
```

---

## 🌐 3. 인프라 및 모니터링

### 3.1 Sentry 에러 트래킹

**파일**: `hotel-web/src/main.js`

**적용 내용**:
- Sentry SDK 통합으로 실시간 에러 모니터링
- PII(개인 식별 정보) 수집 활성화
- Vue 앱 전체 에러 추적

```javascript
import * as Sentry from "@sentry/vue";

Sentry.init({
  app,
  dsn: "https://acacef884a7f193be8308f7eb4cbdb98@o4510221734772736.ingest.us.sentry.io/4510221763543041",
  sendDefaultPii: true
});
```

### 3.2 Prometheus + Grafana 모니터링

**아키텍처**:
- **Monitoring EC2**: Prometheus + Grafana 전용 서버
- **Back EC2**: Spring Boot 애플리케이션 (포트 8888)
- **보안그룹 설정**:
  - `SG-monitor`: 22(SSH), 9090(Prometheus), 3000(Grafana) - 내 IP만 허용
  - `SG-backend`: 8888 포트 - `SG-frontend`, `SG-monitor`에서만 접근 가능

**Prometheus 설치 (Monitoring EC2)**:
```bash
# Prometheus 바이너리 다운로드 및 설치
cd /tmp
curl -LO https://github.com/prometheus/prometheus/releases/download/v2.55.1/prometheus-2.55.1.linux-amd64.tar.gz
tar xvf prometheus-2.55.1.linux-amd64.tar.gz
sudo mv prometheus-2.55.1.linux-amd64 /opt/prometheus

# 사용자 및 권한 설정
sudo useradd --no-create-home --shell /usr/sbin/nologin prometheus
sudo mkdir -p /opt/prometheus/data
sudo chown -R prometheus:prometheus /opt/prometheus
```

**Prometheus 설정 (`/opt/prometheus/prometheus.yml`)**:
```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'spring-boot-backend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['<BACK_PRIVATE_IP>:8888']
```

**Prometheus systemd 서비스 (`/etc/systemd/system/prometheus.service`)**:
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

**Grafana 설치 (Monitoring EC2)**:
```bash
# Grafana 설치
sudo apt-get update -y
sudo apt-get install -y adduser libfontconfig1
wget https://dl.grafana.com/oss/release/grafana_11.1.4_amd64.deb
sudo dpkg -i grafana_11.1.4_amd64.deb
sudo apt --fix-broken install -y  # 의존성 자동 해결
sudo systemctl enable grafana-server
sudo systemctl start grafana-server
```

**Grafana 설정**:
- 접속: `http://<MONITOR_PUBLIC_IP>:3000`
- 기본 계정: `admin` / `admin` (초기 로그인 후 변경 필수)
- Prometheus 데이터소스 추가: `http://localhost:9090`
- 대시보드 Import: Spring Boot(12900), Micrometer JVM(4701)

**백엔드 Actuator 설정 (`application.yml`)**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: never
  metrics:
    export:
      prometheus:
        enabled: true
```

**보안 포인트**:
- `/actuator/prometheus` 엔드포인트는 **내부 네트워크에서만 접근**
- 8888 포트는 외부(0.0.0.0/0)에 절대 노출하지 않음
- Prometheus(9090), Grafana(3000)는 내 IP만 허용
- SSH 터널을 통한 접근 권장

---

## 🔧 4. 개발 환경 개선

### 4.1 환경 변수 로딩 개선

**파일**: `my-backend/src/main/resources/application.yml`

**문제점**: 
- VS Code 디버그 시 작업 디렉터리가 프로젝트 루트(`c:\new\hotel`)로 설정되어 `.env` 파일을 찾지 못함

**해결책**:
```yaml
spring:
  config:
    import: optional:file:./my-backend/.env[.properties],optional:file:.env[.properties]
```

**효과**:
- 루트에서 실행 시 `./my-backend/.env` 탐색
- my-backend 폴더에서 실행 시 `./.env` 탐색
- 두 경로 모두 지원으로 디버그/프로덕션 환경 동시 지원

### 4.2 데이터베이스 연결 설정

**수정 내용**:
```yaml
# 로컬 개발 환경
datasource:
  url: jdbc:mariadb://localhost:3307/hotel

# EC2 배포 환경
datasource:
  url: jdbc:mariadb://<RDS_ENDPOINT>:3306/hotel
```

---

## 📊 5. 보안 강화 전후 비교

### 5.1 XSS 방어

| 항목 | 이전 | 이후 |
|------|------|------|
| 프론트엔드 HTML 렌더링 | 직접 `v-html` 사용 | DOMPurify + SafeHtml 컴포넌트 |
| 백엔드 이메일 템플릿 | 원시 문자열 삽입 | HTML 이스케이프 적용 |
| 보안 테스트 | 미실시 | XSS 페이로드 차단 확인 |

### 5.2 봇 방어

| 항목 | 이전 | 이후 |
|------|------|------|
| 로그인 보호 | 없음 | reCAPTCHA v2 필수 |
| 봇 탐지 | 없음 | Google reCAPTCHA 서버 검증 |
| 무차별 대입 공격 방어 | 취약 | 봇 차단으로 완화 |

### 5.3 모니터링

| 항목 | 이전 | 이후 |
|------|------|------|
| 에러 추적 | 콘솔 로그만 | Sentry 실시간 알림 |
| 성능 모니터링 | 없음 | Prometheus + Grafana |
| 메트릭 수집 | 없음 | Actuator 엔드포인트 |

---

## ✅ 6. 검증 및 테스트

### 6.1 XSS 방어 검증

**테스트 케이스**:
```html
<!-- 주입 시도 -->
<img src=x onerror=alert('XSS')>
<script>alert('XSS')</script>
<div onclick="alert('XSS')">Click</div>

<!-- DOMPurify 처리 후 -->
<img src="x">
<!-- script 태그 완전 제거 -->
<div>Click</div>  <!-- onclick 제거 -->
```