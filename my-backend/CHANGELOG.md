# 프로젝트 구현 내역 📋

## 개요
Spring Boot 기반 소셜 로그인 시스템으로, 네이버, 구글, 카카오 3개 플랫폼의 OAuth2 인증을 지원하며 JWT 토큰 기반 인증을 구현한 백엔드 애플리케이션입니다.

## � 주요 기능

### 1. 소셜 로그인 시스템
- **지원 플랫폼**: 네이버, 구글, 카카오
- **OAuth2 인증**: Spring Security OAuth2 Client 사용
- **계정 통합**: 동일 이메일로 여러 플랫폼 연동 시 자동 계정 병합
- **사용자 정보**: 각 플랫폼별 사용자 정보 추출 및 저장

### 2. JWT 인증 시스템
- **토큰 기반 인증**: 로그인 성공 시 JWT 토큰 발급
- **프론트엔드 연동**: 토큰을 포함한 리다이렉트 URL 제공
- **보안**: 256비트 이상의 안전한 비밀키 사용
- **만료 시간**: 24시간 (86400000ms) 설정

### 3. 사용자 관리
- **User 엔티티**: JPA 기반 사용자 정보 관리
- **Role 시스템**: USER, ADMIN 권한 구분
- **계정 병합**: 이메일 기반 중복 계정 자동 통합
- **프로필 정보**: 이름, 이메일, 프로필 이미지 저장

### 4. OAuth2 커스텀 서비스
- **CustomOAuth2UserService**: 각 플랫폼별 사용자 정보 처리
- **OAuth2AuthenticationSuccessHandler**: 로그인 성공 후 JWT 발급 및 리다이렉트
- **OAuth2Attributes**: 플랫폼별 속성 매핑 유틸리티

### 5. 보안 설정
- **Spring Security**: OAuth2 및 JWT 인증 설정
- **CORS 지원**: 프론트엔드 연동을 위한 CORS 활성화
- **JwtAuthenticationFilter**: JWT 토큰 검증 필터
- **SecurityConfig**: 보안 규칙 및 OAuth2 설정

### 6. 데이터베이스 연동
- **MariaDB**: 운영 데이터베이스
- **JPA/Hibernate**: ORM 매핑
- **Repository 패턴**: LoginRepository를 통한 데이터 접근
- **자동 DDL**: 테이블 자동 생성 및 업데이트

### 7. API 엔드포인트
```
GET  /oauth2/authorization/{provider}  - 소셜 로그인 시작
GET  /login/oauth2/code/{provider}     - OAuth2 콜백 처리  
POST /auth/login                       - 일반 로그인 (향후 확장)
GET  /user/me                          - 현재 사용자 정보
```

### 8. 환경변수 관리 ⭐ **최신 업데이트**
- **`.env` 파일 기반**: 개발 환경변수 관리
- **spring-dotenv**: 자동 환경변수 로드 라이브러리  
- **Git 보안**: `.env` 파일만 Git 제외로 민감정보 보호
- **기본값 제공**: 환경변수 없어도 기본값으로 실행 가능
- **팀 협업**: `.env.example` 템플릿으로 필요 변수 공유
- **자동 로드**: Spring Boot 시작 시 `.env` 파일 자동 읽기
- **단순화**: 하나의 환경변수 파일로 통합 관리

## 🛠️ 기술 스택 추가

### 백엔드 의존성
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

<!-- MariaDB -->
<dependency>
    <groupId>org.mariadb.jdbc</groupId>
    <artifactId>mariadb-java-client</artifactId>
</dependency>

<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
</dependency>
```

### 프론트엔드 구성
- Vue.js 3 + Vite 프로젝트 구조
- 소셜 로그인 버튼 컴포넌트
- OAuth2 리다이렉트 처리
- JWT 토큰 관리

## � 기술 스택

### 백엔드
- **Spring Boot**: 3.5.5
- **Spring Security**: OAuth2 Client + JWT
- **Spring Data JPA**: 데이터베이스 ORM
- **MariaDB**: 데이터베이스
- **Maven**: 빌드 도구

### 라이브러리
- **JJWT**: JWT 토큰 처리
- **spring-dotenv**: 환경변수 관리 ⭐
- **Lombok**: 코드 간소화

### 환경 관리
- **환경변수**: `.env` 파일 기반 설정 ⭐
- **프로필**: local, production 환경 분리
- **보안**: Git에서 민감정보 제외

## 🚀 실행 방법

### 1. 환경 설정
```bash
# 환경변수 파일 생성
copy .env.example .env

# .env 파일에 실제 OAuth2 클라이언트 정보 입력
```

### 2. 데이터베이스 설정
```sql
-- MariaDB에서 데이터베이스 생성
CREATE DATABASE hotel_db;
```

### 3. 애플리케이션 실행
```bash
# 의존성이 자동으로 다운로드되고 .env 파일이 로드됩니다
./mvnw spring-boot:run
```

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

### Phase 4: 환경 관리 개선 ⭐ **최신**
- `.env` 파일 기반 환경변수 관리
- spring-dotenv 라이브러리 도입
- 보안 강화 및 팀 협업 편의성 증대
- 불필요한 파일 제거 (배치 파일, application-local.properties)
- 자동 환경변수 로드 시스템 구축
- 단일 환경변수 파일로 관리 단순화

## 🎯 성과
- ✅ 완전한 3-플랫폼 소셜 로그인 구현
- ✅ JWT 기반 안전한 인증 시스템
- ✅ 계정 통합 및 중복 처리
- ✅ 환경별 설정 분리 및 보안 강화
- ✅ 모든 애플리케이션 오류 해결
- ✅ 성능 최적화 (4.963초 빠른 시작)
- ✅ 현대적 환경변수 관리 시스템 도입 ⭐

## 🔐 보안 기능

### 1. OAuth2 설정
- 각 소셜 플랫폼별 클라이언트 ID/Secret 관리
- 안전한 리다이렉트 URI 설정
- CSRF 보호 설정

### 2. JWT 보안
- HS256 알고리즘 사용
- 24시간 토큰 만료 시간
- 안전한 시크릿 키 관리

### 3. 계정 통합
- 동일 이메일 사용 시 계정 자동 통합
- 소셜 프로바이더 정보 중복 방지
- 메인 계정 기준 데이터 통합

## 🌐 프론트엔드 연동

### OAuth2 로그인 플로우
1. 프론트엔드에서 소셜 로그인 버튼 클릭
2. 백엔드 OAuth2 엔드포인트로 리다이렉트
3. 소셜 플랫폼 인증 완료 후 콜백
4. JWT 토큰과 함께 프론트엔드로 리다이렉트
5. 프론트엔드에서 토큰 저장 및 사용자 정보 표시

### API 통신
- CORS 설정으로 `http://localhost:5173` 허용
- JWT 토큰 기반 인증
- RESTful API 설계

## 📊 데이터베이스 설계

### Users 테이블
```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    password VARCHAR(255),
    phone VARCHAR(255),
    address TEXT,
    provider VARCHAR(50),
    provider_id VARCHAR(255),
    social_providers TEXT,
    role VARCHAR(20) DEFAULT 'USER',
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 🚀 배포 준비

### Git 설정
- `.gitignore` 파일 구성
- 민감 정보 분리 (application-local.properties)
- README.md 작성

### 환경 변수 관리
- 로컬 개발: `application-local.properties`
- 프로덕션: 서버 환경변수 또는 외부 설정 파일

## 🎯 주요 달성 사항

1. ✅ **완전한 소셜 로그인 시스템** - 네이버, 구글, 카카오 3개 플랫폼
2. ✅ **JWT 기반 인증 시스템** - 토큰 생성, 검증, 필터링
3. ✅ **계정 통합 기능** - 동일 이메일 계정 자동 통합
4. ✅ **보안 설정 완료** - CORS, CSRF, 암호화
5. ✅ **프론트엔드 연동** - Vue.js와 완전 연동
6. ✅ **데이터베이스 연동** - MariaDB + JPA
7. ✅ **환경 설정 분리** - 개발/프로덕션 환경 분리
8. ✅ **Git 보안 설정** - 민감 정보 보호

## 📈 성능 최적화

- Spring Data JDBC 제거로 모듈 충돌 해결
- MariaDB Dialect 자동 감지로 경고 제거
- JPA Open-in-View 최적화
- HikariCP 커넥션 풀 사용
- Hibernate 쿼리 최적화

---

**개발 기간**: 2025년 9월 14일  
**주요 기술**: Spring Boot 3.5.5, Spring Security, OAuth2, JWT, MariaDB, Vue.js 3
