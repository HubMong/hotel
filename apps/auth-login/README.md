# 🔐 Spring Boot 소셜 로그인 시스템

Spring Boot 기반 소셜 로그인 시스템으로, **네이버, 구글, 카카오** 3개 플랫폼의 OAuth2 인증을 지원하며 JWT 토큰 기반 인증을 구현한 백엔드 애플리케이션입니다.

## ✨ 주요 기능

### 🔑 소셜 로그인 시스템
- **지원 플랫폼**: 네이버, 구글, 카카오
- **OAuth2 인증**: Spring Security OAuth2 Client 사용
- **계정 통합**: 동일 이메일로 여러 플랫폼 연동 시 자동 계정 병합
- **사용자 정보**: 각 플랫폼별 사용자 정보 추출 및 저장

### 🛡️ JWT 인증 시스템
- **토큰 기반 인증**: 로그인 성공 시 JWT 토큰 발급
- **프론트엔드 연동**: 토큰을 포함한 리다이렉트 URL 제공
- **보안**: 256비트 이상의 안전한 비밀키 사용
- **만료 시간**: 24시간 (86400000ms) 설정

### 👥 사용자 관리
- **User 엔티티**: JPA 기반 사용자 정보 관리
- **Role 시스템**: USER, ADMIN 권한 구분
- **계정 병합**: 이메일 기반 중복 계정 자동 통합
- **프로필 정보**: 이름, 이메일, 프로필 이미지 저장

## 🔧 기술 스택

### 백엔드
- **Spring Boot**: 3.5.5
- **Spring Security**: OAuth2 Client + JWT
- **Spring Data JPA**: 데이터베이스 ORM
- **MariaDB**: 데이터베이스
- **Maven**: 빌드 도구

### 라이브러리
- **JJWT**: JWT 토큰 처리
- **spring-dotenv**: 환경변수 관리
- **Lombok**: 코드 간소화

## 🚀 빠른 시작

### 1. 프로젝트 클론
```bash
git clone <repository-url>
cd my-backend
```

### 2. 데이터베이스 설정
MariaDB를 설치하고 데이터베이스를 생성하세요:
```sql
CREATE DATABASE hotel_db;
```

### 3. 환경변수 설정
```bash
# 환경변수 파일 생성
copy .env.example .env
```

`.env` 파일을 열어서 실제 값들을 입력하세요:
```env
# 데이터베이스 설정
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password

# 네이버 OAuth2
NAVER_CLIENT_ID=your_naver_client_id
NAVER_CLIENT_SECRET=your_naver_client_secret

# 구글 OAuth2
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret

# 카카오 OAuth2
KAKAO_CLIENT_ID=your_kakao_client_id
KAKAO_CLIENT_SECRET=your_kakao_client_secret

# JWT 설정
JWT_SECRET=your_jwt_secret_key_minimum_256_bits
```

### 4. 애플리케이션 실행
# my-backend에서 폴더 이동 후 npm run start 하시면 스프링 실행되도록 json 파일에 설정해놨습니다

# hotel-web에서는 npm run dev 하시면 프론트 실행됩니다.
```

서버가 `http://localhost:8888`에서 실행됩니다.

## 🔑 OAuth2 설정 가이드

각 소셜 플랫폼에서 애플리케이션을 등록하고 클라이언트 ID/Secret을 발급받으세요:

### 네이버
1. [네이버 개발자 센터](https://developers.naver.com/) 접속
2. 애플리케이션 등록
3. 서비스 URL: `http://localhost:8888`
4. Callback URL: `http://localhost:8888/login/oauth2/code/naver`

### 구글
1. [Google Cloud Console](https://console.cloud.google.com/) 접속
2. OAuth 2.0 클라이언트 ID 생성
3. 승인된 리디렉션 URI: `http://localhost:8888/login/oauth2/code/google`

### 카카오
1. [카카오 개발자 센터](https://developers.kakao.com/) 접속
2. 애플리케이션 등록
3. Redirect URI: `http://localhost:8888/login/oauth2/code/kakao`

## 📡 API 엔드포인트

```
GET  /oauth2/authorization/{provider}  - 소셜 로그인 시작 (naver, google, kakao)
GET  /login/oauth2/code/{provider}     - OAuth2 콜백 처리  
POST /auth/login                       - 일반 로그인 (향후 확장)
GET  /user/me                          - 현재 사용자 정보
```

## 🔐 보안 기능

### OAuth2 보안
- 각 소셜 플랫폼별 클라이언트 ID/Secret 관리
- 안전한 리다이렉트 URI 설정
- CSRF 보호 설정

### JWT 보안
- HS256 알고리즘 사용
- 24시간 토큰 만료 시간
- 안전한 시크릿 키 관리

### 환경변수 보안
- **`.env` 파일**: Git에 포함되지 않아 개인 정보 안전 보호
- **환경변수 우선순위**: 시스템 환경변수 > .env 파일 > 기본값
- **팀 협업**: `.env.example` 템플릿으로 필요한 설정 공유

## 🌐 프론트엔드 연동

### OAuth2 로그인 플로우
1. 프론트엔드에서 소셜 로그인 버튼 클릭
2. 백엔드 OAuth2 엔드포인트로 리다이렉트
3. 소셜 플랫폼 인증 완료 후 콜백
4. JWT 토큰과 함께 프론트엔드로 리다이렉트 (`http://localhost:5173/oauth2/redirect`)
5. 프론트엔드에서 토큰 저장 및 사용자 정보 표시

### CORS 설정
- `http://localhost:5173` 허용 (Vue.js 개발 서버)
- JWT 토큰 기반 인증
- RESTful API 설계

## 📈 개발 히스토리

### Phase 1: 기본 OAuth2 구현
- Spring Security OAuth2 Client 설정
- 네이버, 구글, 카카오 OAuth2 연동

### Phase 2: JWT 인증 추가
- JWT 토큰 발급 및 검증 시스템
- 프론트엔드 리다이렉트 처리

### Phase 3: 사용자 관리 고도화
- 계정 통합 로직 구현
- 사용자 정보 관리 최적화

### Phase 4: 환경 관리 개선 ⭐
- `.env` 파일 기반 환경변수 관리
- spring-dotenv 라이브러리 도입
- 보안 강화 및 팀 협업 편의성 증대
- 불필요한 파일 제거
- 자동 환경변수 로드 시스템 구축

## 🎯 주요 성과

- ✅ **완전한 3-플랫폼 소셜 로그인** - 네이버, 구글, 카카오
- ✅ **JWT 기반 안전한 인증 시스템**
- ✅ **계정 통합 및 중복 처리**
- ✅ **환경별 설정 분리 및 보안 강화**
- ✅ **모든 애플리케이션 오류 해결**
- ✅ **성능 최적화** (4.963초 빠른 시작)
- ✅ **현대적 환경변수 관리 시스템**

## 📚 주요 의존성

```xml
<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- OAuth2 Client -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>

<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>

<!-- 환경변수 관리 -->
<dependency>
    <groupId>me.paulschwarz</groupId>
    <artifactId>spring-dotenv</artifactId>
    <version>2.5.4</version>
</dependency>

<!-- MariaDB -->
<dependency>
    <groupId>org.mariadb.jdbc</groupId>
    <artifactId>mariadb-java-client</artifactId>
</dependency>
```

## 🔧 트러블슈팅

### 일반적인 문제들

**Q: 카카오 로그인 시 "Invalid credentials" 오류**  
A: `application.properties`에 `client-authentication-method=client_secret_post` 추가

**Q: 환경변수가 로드되지 않음**  
A: `.env` 파일이 프로젝트 루트에 있는지, spring-dotenv dependency가 추가되었는지 확인

**Q: 데이터베이스 연결 오류**  
A: MariaDB 서버가 실행 중인지, `hotel_db` 데이터베이스가 생성되었는지 확인

---

**개발 기간**: 2025년 9월 14일  
**주요 기술**: Spring Boot 3.5.5, Spring Security, OAuth2, JWT, MariaDB, Vue.js 3