# AI RAG Platform

실무형 PR 단위로 RAG 서비스를 단계적으로 구현하는 프로젝트입니다.

## 현재 진행 상태

- **완료**: `PR-00` ~ `PR-10` (로컬 기능 + Dockerfile/배포 포장 + AWS 1차 운영 반영까지)
- **다음**: `PR-11` (비동기 인제스트: SQS + 워커) — 상세는 `진행순서.md`

## AI 모델/프로바이더 결정

- 현재 프로젝트는 `OpenAI API` 기준으로 진행합니다.
- MVP 기본 모델
  - 임베딩: `text-embedding-3-small`
  - 답변 생성: `gpt-4o-mini`
- 추후 필요 시 Provider 추상화로 `Azure OpenAI` 전환 가능하게 유지합니다.

## OpenAI 키 연결

- 키가 없으면 fallback 답변으로 동작하며, 실제 LLM 품질 답변이 나오지 않습니다.
- 백엔드 실행 전에 환경변수를 설정하세요.

```powershell
$env:APP_OPENAI_API_KEY="sk-..."
$env:APP_OPENAI_CHAT_MODEL="gpt-4o-mini"
$env:APP_OPENAI_EMBEDDING_MODEL="text-embedding-3-small"
```

- 설정 후 `backend`를 재실행하면 `POST /api/rag/ask`가 OpenAI 호출로 동작합니다.

## 현재 구현된 기능

### 프론트엔드

- 로그인/로그아웃 UI
- JWT 저장/유지(`Pinia` + `localStorage`)
- 인증 라우팅 가드(미로그인 시 `/login` 리다이렉트)
- 헬스체크 화면(`/health`)
- 문서 화면(`/documents`)
  - 문서 생성(제목 + 텍스트)
  - 내 문서 목록 조회
  - 문서 상세 조회
  - 문서 삭제
  - 문서 멀티 선택 + 질의 입력
  - 답변 + sources(근거) 표시
- Naive UI 기반 기본 레이아웃(`AppShell`)

### 백엔드

- Spring Boot + PostgreSQL(pgvector) + Flyway 기반
- 공통 에러 포맷(`VALIDATION_ERROR`, `MALFORMED_JSON` 등)
- CORS(dev에서 `http://localhost:5173` 허용)
- 인증/인가 1차
  - 회원가입 API: `POST /api/auth/register`
  - 로그인 API: `POST /api/auth/login`
  - JWT 발급 및 필터 검증
  - 보호 API 접근 제어(`/api/auth/**`, `/api/health`, `/actuator/health` 제외 인증 필요)
- 문서 API
  - `POST /api/documents` (생성)
  - `GET /api/documents` (내 문서 목록)
  - `GET /api/documents/{id}` (내 문서 상세)
  - `DELETE /api/documents/{id}` (내 문서 삭제)
  - 사용자 소유 문서 격리(본인 문서만 조회 가능)
- RAG API
  - `POST /api/rag/ask` (`question`, `documentIds[]`)
  - 선택 문서 소유권 검증
  - 선택 문서 범위 벡터 검색 Top-K + 답변 + sources 반환

### DB/Flyway

- `V1__create_users.sql`
- `V2__create_documents.sql`
- dev seed 관리자 계정 생성
  - email: `admin@local.dev`
  - password: `admin1234`

## 로컬 실행 주소

- 프론트: `http://localhost:5173`
- 로그인: `http://localhost:5173/login`
- 문서 화면: `http://localhost:5173/documents`
- 백엔드: `http://localhost:8080`
- 헬스체크: `http://localhost:8080/actuator/health`

## 빠른 실행

### 1) DB 실행

```powershell
cd C:\dev\ai-rag-platform\infra
docker compose up -d
```

### 2) 백엔드 실행

```powershell
cd C:\dev\ai-rag-platform\backend
.\gradlew bootRun
```

### 3) 프론트 실행

```powershell
cd C:\dev\ai-rag-platform\frontend
npm install
npm run dev -- --host
```

## 앞으로 구현할 항목 (TODO)

`진행순서.md` 기준 **남은 PR**은 아래입니다.

- `PR-11`: 비동기 인제스트(SQS + 워커)

선택/문서화(운영 마무리):

- 운영 체크리스트(장애 대응·롤백) 정리
- (선택) S3 버킷·CloudWatch 알람 등 PR-10 확장 항목

## 참고

- 상세 PR 체크리스트: `진행순서.md`

## GitHub Pages 배포

- 워크플로 파일: `.github/workflows/deploy-pages.yml`
- 트리거: `main` 브랜치 push 시 자동 배포
- 예상 URL: `https://tissue45.github.io/ai-rag-platform/`
- 빌드 시 API 베이스 URL 주입: `VITE_API_BASE_URL` (GitHub Actions Secret)
- 시크릿 누락 방지를 위해 빌드 단계에서 `VITE_API_BASE_URL` 비어 있으면 실패하도록 검증 추가

초기 1회는 GitHub 저장소 설정에서 `Settings > Pages > Source`를 `GitHub Actions`로 설정해야 합니다.

## Docker / AWS 진행 상황

### Docker ✅

- 백엔드 Dockerfile 작성 및 이미지 빌드/배포 흐름 구성
- ECR 저장소(`ai-rag-backend`)로 이미지 push
- 컨테이너 실행 시 JVM 옵션 추가:
  - `-Djava.net.preferIPv4Stack=true`
  - 목적: 배포 환경에서 DB 연결 타임아웃 이슈 완화

### AWS 인프라 ✅

- RDS(PostgreSQL) 생성 및 백엔드 연결 구성
- ECS Fargate 클러스터/서비스로 백엔드 배포
- ALB + Target Group 연동(백엔드 8080, HTTPS 리스너)
- Route 53으로 API 도메인(`api.airag.site` 등) 연결
- Secrets Manager: JWT·DB 비밀번호를 ECS 태스크 `secrets`로 주입(태스크 정의 예: `ai-rag-backend:3`)
- CloudWatch 로그(`/ecs/ai-rag-backend`)

### 운영 반영된 수정 ✅

- 백엔드 CORS/보안 설정 보완
  - `OPTIONS` 요청 허용(프리플라이트 대응)
  - GitHub Pages 출처 허용
- 프론트 라우터 배포 경로 보정
  - `createWebHistory(import.meta.env.BASE_URL)` 적용
- API 베이스 URL 정규화
  - 환경변수에 `/api`가 들어와도 중복 경로(`/api/api/...`)가 되지 않도록 처리
- 로그인 폼 기본 계정값 제거(의도치 않은 admin 로그인 방지)

### 남은 선택 작업

- 운영 체크리스트(장애 대응·롤백) 문서화
- (선택) S3, CloudWatch 알람, LLM 키도 Secrets Manager로 이관 등

