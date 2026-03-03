<div align="center">

# OMTeam Server

**CMC 18기 헬스케어 모바일 앱 백엔드**

사용자 맞춤 데일리 운동 미션 추천 및 AI 기반 피드백을 제공하는 Spring Boot 백엔드 서버

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.9-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=flat-square&logo=redis&logoColor=white)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white)](https://www.docker.com/)
[![AWS](https://img.shields.io/badge/AWS-EC2_·_ECR-FF9900?style=flat-square&logo=amazonaws&logoColor=white)](https://aws.amazon.com/)
[![CI](https://img.shields.io/github/actions/workflow/status/Central-MakeUs/OMTeam_Server/ci.yml?branch=develop&label=CI&style=flat-square&logo=githubactions&logoColor=white)](https://github.com/Central-MakeUs/OMTeam_Server/actions)

</div>

---

## 목차

- [개요](#개요)
- [주요 기능](#주요-기능)
- [기술 스택](#기술-스택)
- [아키텍처](#아키텍처)
- [시작하기](#시작하기)
- [API 명세](#api-명세)
- [환경 변수](#환경-변수)
- [배포](#배포)

---

## 개요

OMTeam Server는 사용자의 생활 패턴과 체력 수준에 맞는 데일리 운동 미션을 AI가 추천하고, 수행 결과에 대한 피드백을 제공하는 헬스케어 앱의 백엔드입니다.

**핵심 흐름은 단순합니다.** 사용자가 앱을 열면 AI가 오늘의 운동 미션 3개를 추천합니다. 사용자는 1개를 선택해 수행하고 결과를 기록합니다. 매주 AI가 패턴을 분석해 피드백을 제공합니다.

Google, Kakao, Apple 소셜 로그인을 지원하며, LangGraph 기반 AI 서버와 WebClient로 비동기 연동합니다. Resilience4j Circuit Breaker로 AI 서버 장애 시에도 서비스가 중단되지 않습니다.

---

## 주요 기능

| 기능 | 설명 |
|------|------|
| **소셜 로그인** | Google, Kakao, Apple idToken 서명 검증 후 자체 JWT 발급 |
| **온보딩** | 앱 사용 목적, 근무 시간대, 운동 가능 시간, 선호 운동, 생활 패턴 수집 |
| **데일리 미션** | AI가 사용자 맞춤 운동 미션 3개 추천 → 1개 선택 → 결과 기록 |
| **캐릭터 성장** | 미션 시작 30회 초과 시 캐릭터 레벨업 |
| **주간 리포트** | AI 기반 주간 수행 분석 및 피드백 |
| **챗봇** | LangGraph API 연동 질문-선택지 기반 대화 |
| **푸시 알림** | Firebase FCM으로 리마인드/체크인/회고 알림 스케줄링 |

---

## 기술 스택

### 애플리케이션

| 분류 | 기술 | 버전 |
|------|------|------|
| Language | Java | 21 |
| Framework | Spring Boot | 3.5.9 |
| ORM | Spring Data JPA | - |
| Security | Spring Security + jjwt | 0.13.0 |
| Async Client | Spring WebFlux WebClient | - |
| Resilience | Resilience4j Circuit Breaker | 2.2.0 |
| Push | Firebase Admin SDK | 9.4.3 |
| Docs | springdoc OpenAPI | 2.8.15 |
| Util | Lombok | - |

### 인프라

| 분류 | 기술 |
|------|------|
| DB | MySQL 8.0 |
| Cache / 토큰 저장 | Redis 7 |
| Container | Docker, Docker Compose |
| Registry | AWS ECR |
| Server | AWS EC2 |
| Proxy | Nginx + Let's Encrypt |
| CI/CD | GitHub Actions |

---

## 아키텍처

### 패키지 구조

```
com.omteam.omt/
├── common/
│   ├── response/        # ApiResponse<T> 공통 응답 래퍼
│   └── exception/       # BusinessException + GlobalExceptionHandler
├── config/              # Security, CORS, Swagger, WebClient 설정
├── security/
│   ├── auth/
│   │   ├── controller/  # OAuth 엔드포인트
│   │   ├── service/     # 로그인 흐름 처리
│   │   ├── jwt/         # JwtTokenProvider
│   │   └── oauth/       # OAuthClient 인터페이스 + 프로바이더별 구현체
│   └── principal/       # UserPrincipal
├── user/                # 사용자 관리
├── onboarding/          # 온보딩
├── mission/             # 미션 추천/결과
├── character/           # 캐릭터 성장
├── report/              # 일일/주간 리포트
├── chat/                # AI 챗봇
└── notification/        # FCM 푸시 알림
```

### 공통 API 응답 형식

모든 엔드포인트는 `ApiResponse<T>`를 반환합니다.

```json
// 성공
{ "success": true, "data": { ... }, "error": null }

// 실패
{ "success": false, "data": null, "error": { "code": "...", "message": "..." } }
```

### 소셜 로그인 인증 흐름

```
클라이언트                    OMTeam Server                  Provider (Google/Kakao/Apple)
    │                              │                                    │
    │  POST /auth/oauth/{provider} │                                    │
    │  { "idToken": "..." }        │                                    │
    │─────────────────────────────>│                                    │
    │                              │  JWKS public key 요청              │
    │                              │───────────────────────────────────>│
    │                              │  public key 응답                   │
    │                              │<───────────────────────────────────│
    │                              │                                    │
    │                              │  idToken 서명 검증                  │
    │                              │  User 조회 or 생성                  │
    │                              │  JWT(access + refresh) 발급        │
    │                              │                                    │
    │  { accessToken, refreshToken }                                    │
    │<─────────────────────────────│                                    │
```

### 데일리 미션 추천 흐름

```
클라이언트                OMTeam Server              AI Server (LangGraph)
    │                         │                              │
    │  POST /api/missions/    │                              │
    │  daily/recommend        │                              │
    │────────────────────────>│                              │
    │                         │  사용자 온보딩 데이터 조회     │
    │                         │  WebClient 비동기 요청        │
    │                         │─────────────────────────────>│
    │                         │                              │  미션 3개 생성
    │                         │  미션 3개 응답               │
    │                         │<─────────────────────────────│
    │                         │  DB 저장 (daily_recommended_mission)
    │  미션 3개 반환           │
    │<────────────────────────│
    │                         │
    │  POST /api/missions/    │   ※ Circuit Breaker (Resilience4j)
    │  daily/start            │     AI 서버 장애 시 Fallback 동작
    │────────────────────────>│
    │  POST /api/missions/    │
    │  daily/complete         │
    │────────────────────────>│
```

---

## 시작하기

### 사전 요구사항

- [Docker](https://www.docker.com/) 및 Docker Compose
- [Java 21](https://openjdk.org/projects/jdk/21/) (로컬 빌드 시)
- MySQL 8.0 (로컬 DB 사용 시)

### 로컬 실행 (Docker Compose)

**1. 저장소 클론**

```bash
git clone https://github.com/Central-MakeUs/OMTeam_Server.git
cd OMTeam_Server
```

**2. 환경 변수 파일 생성**

```bash
cp .env.example .env
```

`.env` 파일을 열어 필수 값을 채워넣습니다. 아래 [환경 변수](#환경-변수) 섹션을 참고하세요.

**3. 서비스 실행**

```bash
# Spring Boot + Redis + AI Server 전체 실행
docker compose up -d

# 로그 확인
docker compose logs -f omt-app
```

**4. API 문서 확인**

서버가 기동되면 아래 URL에서 Swagger UI를 열 수 있습니다.

```
http://localhost:8080/swagger-ui/index.html
```

**5. 헬스체크**

```bash
curl http://localhost:8080/actuator/health
```

### 로컬 빌드 및 테스트

```bash
# 빌드
./gradlew build

# 단위 테스트
./gradlew test

# 통합 테스트
./gradlew integrationTest

# 전체 테스트
./gradlew allTests

# 단일 테스트 클래스 실행
./gradlew test --tests "com.omteam.omt.security.auth.service.AuthServiceTest"

# 클린 빌드
./gradlew clean build
```

---

## API 명세

전체 API 명세는 Swagger UI(`/swagger-ui/index.html`)에서 확인할 수 있습니다.

<details>
<summary>인증 (Auth)</summary>

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/auth/oauth/{provider}` | 소셜 로그인 (`provider`: google, kakao, apple) |
| `POST` | `/auth/refresh` | Access Token 재발급 |
| `POST` | `/auth/logout` | 로그아웃 (Refresh Token 무효화) |
| `POST` | `/auth/withdraw` | 회원 탈퇴 |

**소셜 로그인 요청 예시**

```bash
curl -X POST http://localhost:8080/auth/oauth/kakao \
  -H "Content-Type: application/json" \
  -d '{ "idToken": "<kakao-id-token>" }'
```

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGci...",
    "refreshToken": "eyJhbGci...",
    "isNewUser": true
  },
  "error": null
}
```

</details>

<details>
<summary>온보딩 (Onboarding)</summary>

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/api/onboarding` | 온보딩 정보 등록 |
| `GET` | `/api/onboarding` | 온보딩 정보 조회 |
| `PUT` | `/api/onboarding` | 온보딩 정보 수정 |

</details>

<details>
<summary>데일리 미션 (Mission)</summary>

| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/api/missions/daily/recommend` | AI 기반 오늘의 미션 3개 추천 |
| `POST` | `/api/missions/daily/start` | 미션 시작 (1개 선택) |
| `POST` | `/api/missions/daily/complete` | 미션 완료/실패 결과 기록 |
| `GET` | `/api/missions/daily/status` | 오늘의 미션 상태 조회 |

</details>

---

## 환경 변수

`.env.example`을 복사해 `.env`를 생성하고 아래 값을 채워넣습니다.

<details>
<summary>전체 환경 변수 목록</summary>

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `SPRING_PROFILES_ACTIVE` | 활성 프로파일 | `local` |
| `SPRING_DATASOURCE_URL` | MySQL JDBC URL | - |
| `SPRING_DATASOURCE_USERNAME` | DB 사용자명 | - |
| `SPRING_DATASOURCE_PASSWORD` | DB 비밀번호 | - |
| `SPRING_DATA_REDIS_HOST` | Redis 호스트 | `omt-redis` |
| `SPRING_DATA_REDIS_PORT` | Redis 포트 | `6379` |
| `JWT_SECRET` | JWT 서명 키 (256bit 이상) | - |
| `JWT_ACCESS_EXPIRE` | Access Token 만료 (초) | `300` |
| `JWT_REFRESH_EXPIRE` | Refresh Token 만료 (초) | `1209600` |
| `OAUTH_GOOGLE_IOS_CLIENT_ID` | Google OAuth iOS Client ID | - |
| `OAUTH_GOOGLE_ANDROID_CLIENT_ID` | Google OAuth Android Client ID | - |
| `OAUTH_KAKAO_CLIENT_ID` | Kakao OAuth Client ID | - |
| `OAUTH_APPLE_CLIENT_ID` | Apple OAuth Client ID | - |
| `AI_SERVER_BASE_URL` | AI 서버 base URL | `http://omt-ai:8000` |
| `AI_SERVER_TIMEOUT` | AI 서버 요청 타임아웃 (ms) | - |
| `FCM_SERVICE_ACCOUNT_PATH` | Firebase 서비스 계정 JSON 경로 | - |
| `APP_PORT` | 앱 노출 포트 | `8080` |

**AI 서버 관련 (LangGraph)**

| 변수명 | 설명 |
|--------|------|
| `ANTHROPIC_API_KEY` | Anthropic API 키 |
| `UPSTAGE_API_KEY` | Upstage API 키 |
| `LANGFUSE_SECRET_KEY` | Langfuse 시크릿 키 |
| `LANGFUSE_PUBLIC_KEY` | Langfuse 퍼블릭 키 |
| `LANGFUSE_HOST` | Langfuse 호스트 |
| `LANGSMITH_API_KEY` | LangSmith API 키 |
| `LANGSMITH_PROJECT` | LangSmith 프로젝트명 |

</details>

---

## 배포

### CI/CD 파이프라인

```
push to develop / PR → CI (빌드 · 단위 테스트 · Docker 이미지 빌드 검증)

push to main         → CD (ECR 이미지 푸시 → EC2 배포 → 헬스체크)
```

**GitHub Actions Secrets 필수 설정값**

| Secret | 설명 |
|--------|------|
| `AWS_ACCESS_KEY_ID` | AWS 액세스 키 |
| `AWS_SECRET_ACCESS_KEY` | AWS 시크릿 키 |
| `EC2_HOST` | EC2 퍼블릭 IP 또는 도메인 |
| `EC2_USERNAME` | EC2 접속 사용자명 |
| `EC2_SSH_KEY` | EC2 SSH 프라이빗 키 |
| `JWT_SECRET` | JWT 서명 키 |
| `RDS_URL` | RDS JDBC URL |
| `RDS_USERNAME` | RDS 사용자명 |
| `RDS_PASSWORD` | RDS 비밀번호 |
| `FCM_SERVICE_ACCOUNT_JSON` | Firebase 서비스 계정 JSON (전체 내용) |
| `GH_PAT` | AI 서버 레포 접근용 GitHub PAT |

### 프로덕션 배포 (수동)

EC2에서 직접 실행할 경우:

```bash
# ECR 로그인
aws ecr get-login-password --region ap-northeast-2 | \
  docker login --username AWS --password-stdin <ECR_REGISTRY>

# 프로덕션 Compose 실행 (Spring Boot + AI Server + Nginx + Redis)
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# 로그 확인
docker compose logs -f omt-app
```

### SSL 인증서 발급 (초기 설정)

```bash
# Let's Encrypt 인증서 발급 (최초 1회)
docker compose run --rm certbot certonly \
  --webroot --webroot-path=/var/www/certbot \
  -d yourdomain.com

# Nginx 재시작
docker compose restart nginx
```


**[버그 리포트](https://github.com/Central-MakeUs/OMTeam_Server/issues) · [기능 제안](https://github.com/Central-MakeUs/OMTeam_Server/issues)**

</div>
