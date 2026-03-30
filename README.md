# AI RAG Platform

실무형 PR 단위로 RAG 서비스를 단계적으로 구현하는 프로젝트입니다.

## 현재 진행 상태

- 완료: `PR-00`, `PR-01`, `PR-01.1`, `PR-02`, `PR-03`, `PR-04`, `PR-05`
- 다음: `PR-06` (청킹 + 임베딩 저장)

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
  - 사용자 소유 문서 격리(본인 문서만 조회 가능)

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

- `PR-06`: 청킹 + 임베딩 저장(인제스트 1차)
  - `document_chunks`, `chunk_embeddings` 테이블
  - 청킹 로직 + 임베딩 생성
  - 인제스트 상태 관리
- `PR-07`: 단발 질의(RAG) + 선택 문서 멀티
  - `question + documentIds[]`
  - 선택 문서 범위 벡터 검색 + 답변 + sources
- `PR-08`: 프론트 문서 멀티 선택 + 질의 화면
  - 문서 선택/질문/답변/근거 표시
- `PR-09`: 배포 포장(Dockerfile)
- `PR-10`: AWS 배포(ECS Fargate + RDS + S3)
- `PR-11`: 비동기 인제스트(SQS + 워커)

## 참고

- 상세 PR 체크리스트: `진행순서.md`

