# AI RAG Platform

실무형 PR 단위로 RAG 서비스를 단계적으로 구현하는 프로젝트입니다.

## 현재 진행 상태

- 완료: `PR-00`, `PR-01`, `PR-01.1`, `PR-02`, `PR-03`, `PR-04`, `PR-05`, `PR-06`, `PR-07`, `PR-08`
- 배포 관련: `PR-09`, `PR-10` 일부 선행 진행(백엔드 Docker/ECR/ECS/RDS/ALB, 프론트 GitHub Pages)
- 다음 핵심 개발: `PR-09` (배포 포장)

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

아래는 `진행순서.md` 기준의 남은 PR입니다.

- `PR-09`: 배포 포장(Dockerfile)
- `PR-10`: AWS 배포(ECS Fargate + RDS + S3)
- `PR-11`: 비동기 인제스트(SQS + 워커)

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

### Docker

- 백엔드 Dockerfile 작성 및 이미지 빌드/배포 흐름 구성
- ECR 저장소(`ai-rag-backend`)로 이미지 push 완료
- 컨테이너 실행 시 JVM 옵션 추가:
  - `-Djava.net.preferIPv4Stack=true`
  - 목적: 배포 환경에서 DB 연결 타임아웃 이슈 완화

### AWS 인프라

- RDS(PostgreSQL) 생성 및 백엔드 연결 구성 완료
- ECS Fargate 클러스터/서비스로 백엔드 배포 완료
- ALB + Target Group 연동 완료(백엔드 8080)
- 헬스체크 경로/매처 조정으로 서비스 상태 안정화 진행
- 보안 그룹 연동(ALB -> ECS 8080) 반영

### 운영 반영된 수정

- 백엔드 CORS/보안 설정 보완
  - `OPTIONS` 요청 허용(프리플라이트 대응)
- 프론트 라우터 배포 경로 보정
  - `createWebHistory(import.meta.env.BASE_URL)` 적용
- API 베이스 URL 정규화
  - 환경변수에 `/api`가 들어와도 중복 경로(`/api/api/...`)가 되지 않도록 처리

### 현재 남은 배포 작업

- API HTTPS 적용(도메인 + ACM 퍼블릭 인증서 + ALB 443 리스너)
- `VITE_API_BASE_URL`을 `https://api.<도메인>`으로 최종 전환
- CORS 허용 출처에 GitHub Pages 도메인 최종 반영
- 백엔드 이미지 재배포 후 최종 통합 테스트

