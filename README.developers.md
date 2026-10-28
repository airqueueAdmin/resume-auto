# Career Note

## 한 문장으로 이해하기

Career Note는 사용자의 경력·프로젝트를 근거 데이터로 보관하고, 여러 채용공고에 맞는 지원 문서를 준비하게 하는 서비스입니다. AI가 만든 결과는 원본 경력 데이터를 덮어쓰지 않으며, 사용자가 검토하고 승인한 경우에만 새로운 이력서 버전이 됩니다.

## 기술 스택과 선택 이유

| 영역 | 기술 | 선택 이유 |
| --- | --- | --- |
| Backend | Java 25, Spring Boot 4.1.1 | 장기 유지보수 가능한 모듈형 모놀리스와 명시적인 경계 |
| Web/API | Spring MVC, Bean Validation, Spring Security | HTTP 계약, 입력 검증, JWT 기반 인증을 표준 방식으로 구성 |
| Persistence | PostgreSQL 17, Spring Data JPA, HikariCP | 관계형 소유권·불변성·트랜잭션을 DB에서 보장 |
| Migration | Flyway | 스키마 변경을 버전 관리하고 배포 순서를 재현 |
| Retrieval | pgvector, Ollama embedding | 사용자 데이터와 벡터를 같은 PostgreSQL 안에서 소유자 기준으로 필터링 |
| AI | Gemini/OpenAI/Ollama adapter | provider를 교체해도 Application 계층의 생성 계약은 유지 |
| Frontend | React 19, TypeScript, Vite | 서비스 작업공간과 검토 흐름을 빠르게 구성 |
| Extension | Chrome Extension, thin client | JobKorea 지원 페이지의 읽기·검토·명시적 입력만 보조 |
| Delivery | Docker Compose, Nginx | 로컬과 배포 환경의 구성 차이를 줄이고 frontend/backend를 재현 가능하게 패키징 |

## 아키텍처를 읽는 방법

```text
API Controller
    ↓ 인증·입력 검증·응답 변환
Application Service
    ↓ 유스케이스·소유권·트랜잭션·AI orchestration
Domain
    ↓ 상태 규칙·불변성·검증
Infrastructure
    ↓ JPA/JDBC·Flyway·외부 LLM/Embedding·메일
PostgreSQL / External Provider
```

기능별 패키지를 우선합니다. `controller`, `service`, `repository` 같은 전역 패키지에 서로 다른 도메인을 섞지 않습니다. 단순한 CRUD라도 Application Service가 현재 사용자 확인과 관계 소유권 검사를 담당해야 합니다.

## 핵심 비즈니스 모델

### 1. Source of truth와 derived artifact

| 구분 | 데이터 | 규칙 |
| --- | --- | --- |
| Source | Experience, Project, JobPosting, ResumeVersion, 지원 질문/답변 | 사용자가 작성·검토하는 사실 데이터 |
| Derived | EmbeddingChunk, JobAnalysis, JobMatch, ResumeDraft | source에서 계산된 결과. 원본을 덮어쓰지 않고 새로 저장 |
| Approval result | 승인된 ResumeVersion | 사용자의 명시적 승인 후에만 생성되는 문서 버전 |

Embedding과 AI 결과는 편의를 위한 캐시가 아니라 provenance를 가진 파생 결과입니다. source가 바뀌면 임베딩을 다시 만들고, 기존 AI 결과는 과거 실행 결과로 보존합니다.

### 2. 사용자 소유권

- 모든 사용자 리소스는 인증된 JWT subject의 `user_id`로 조회합니다.
- 요청 body의 `user_id`나 owner ID는 권위 있는 값으로 사용하지 않습니다.
- 단일 리소스와 nested relationship 모두 `resource_id + authenticated_user_id`로 확인합니다.
- 존재하지 않거나 다른 사용자의 리소스는 정보 노출을 막기 위해 `404`를 반환합니다.
- 사용자 삭제는 현재 soft-delete 정책과 Refresh Session 폐기를 따릅니다.

이 규칙은 Controller가 아니라 Application Service와 Repository query 양쪽에서 확인되어야 합니다. 새 endpoint를 추가할 때 cross-user read/update/delete와 relationship 생성 테스트를 같이 추가합니다.

### 3. ResumeVersion 불변성

Resume은 이름과 최신 버전 번호를 관리하는 container입니다. 실제 이력서 내용은 `ResumeVersion` snapshot으로 저장하고 기존 버전을 수정하지 않습니다.

AI draft 승인 흐름은 다음과 같습니다.

```text
JobPosting + owned career evidence
        ↓ retrieval
grounded ResumeDraft + source references
        ↓ user review / explicit approval
new immutable ResumeVersion
```

따라서 “이력서 수정”은 기존 row update가 아니라 새 version 생성입니다. 버전 번호는 row lock과 DB unique constraint로 순차성을 지킵니다.

### 4. RAG와 AI 경계

1. 사용자의 Experience/Project를 source-linked chunk로 나눕니다.
2. embedding을 만들고 사용자별 `embedding_chunks`로 교체합니다.
3. 요청 사용자와 source type으로 Top-K를 필터링합니다.
4. 제한된 evidence만 LLM에 전달합니다.
5. 모델의 citation ID를 서버가 검증합니다.
6. 검증된 결과와 source snapshot을 immutable AI artifact로 저장합니다.

LLM은 citation이 있더라도 사실성 검증이 끝난 것은 아니므로 사용자 검토가 필요합니다. 새 AI 기능은 prompt version, output schema, timeout/retry, token usage, grounding 실패를 함께 설계합니다.

### 5. JobKorea와 Extension의 책임 경계

- JobKorea는 개인 이력서 write API를 사용하지 않습니다.
- 서비스는 검토된 ResumeVersion을 JobKorea 호환 파일로 생성하고 사용자가 다운로드합니다.
- 로그인, 비밀번호, cookie, refresh token, CAPTCHA, 자동 제출, 자동 upload는 다루지 않습니다.
- 최종 파일 선택과 제출은 사용자가 JobKorea에서 직접 수행합니다.
- Extension은 서비스의 source of truth가 아니며, 보이는 공고·질문·필드를 읽고 검토된 텍스트를 명시적으로 입력하는 얇은 보조 클라이언트입니다.

Extension에 새로운 자동화 동작을 추가할 때도 “읽기 → 사용자 검토 → 명시적 입력” 순서를 깨지 않습니다. 페이지에 보이는 공고를 자동으로 JobPosting으로 저장하거나, 사용자의 승인 없이 draft/version을 만들지 않습니다.

## 개인정보와 보안 경계

- 비밀번호, Refresh Token, 이메일 인증코드, Extension 연결코드, Tool approval token은 복호화가 필요 없으므로 해시만 저장합니다.
- 이메일·이름·경력·프로젝트·경력 기간·이력서·지원 답변·공고·AI 결과·검색 원문은 AES-256-GCM으로 DB 저장 시 암호화합니다.
- 경력의 재직 여부도 암호화하며, 암호문을 해석한 뒤 API에서 boolean으로 복원합니다.
- 이메일 로그인/중복 검사는 암호화된 이메일을 복호화해 검색하지 않고 HMAC blind index를 사용합니다.
- 암호화 키는 `DATA_ENCRYPTION_KEY_BASE64`로 주입합니다. 키 분실은 데이터 복구 불가이며, 키 변경은 별도 re-encryption migration이 필요한 운영 작업입니다.
- 로그와 예외에는 이메일, IP 원문, 토큰, 요청 body, resume/job 원문, LLM payload를 남기지 않습니다.
- owner에게 반환하는 API 데이터는 작업을 위해 복호화된 값일 수 있지만, 인증·소유권 검사를 통과한 요청에만 반환합니다.
- 암호화된 날짜는 SQL에서 정렬할 수 없으므로 경력 최신순 정렬은 사용자 소유 행을 애플리케이션에서 복호화한 뒤 적용합니다. 개인정보 보호와 기존 API 정렬 계약을 함께 지키기 위한 의도적인 비용입니다.

상세 결정은 [ADR 0022](docs/adr/0022-application-level-protection-of-personal-data.md), 운영 주의사항은 [개인정보 보호 runbook](docs/runbooks/personal-data-protection.md)을 기준으로 합니다.

## 코드 탐색 순서

1. `api/`: 요청·응답 계약과 인증 진입점
2. `application/`: 유스케이스, transaction, owner scope, provider orchestration
3. `domain/`: 상태 변경 규칙과 immutable/soft-delete 정책
4. `infrastructure/persistence/`: JPA/JDBC query, migration과의 타입 계약
5. `src/test/`: 정상 흐름보다 권한·실패·마이그레이션·provider 경계 확인
6. `frontend/src/App.tsx`, `frontend/src/style.css`: 현재 UI 작업 흐름과 API 소비 방식

외부 provider를 수정할 때는 provider adapter와 application port를 분리합니다. provider 응답의 원문이나 사용자 콘텐츠를 로그에 찍지 않고, 성공/실패 종류·request ID·길이·token metadata처럼 비식별 메타데이터만 기록합니다.

## 협업 시 지켜야 할 변경 규칙

- DB 변경은 기존 migration을 수정하지 않고 새 Flyway migration으로 추가합니다.
- source data와 AI·embedding 같은 derived data를 구분하고, immutable artifact를 덮어쓰지 않습니다.
- 새 사용자 리소스에는 cross-user read/update/delete와 soft-delete/404 테스트를 추가합니다.
- 암호화 대상 컬럼은 JPA converter, 마지막 암호화 migration 이후의 새 migration, schema validation, raw JDBC 경계를 함께 수정합니다.
- 개인정보·외부 연동 경계가 바뀌면 ADR을 갱신하고, 검증 방법과 알려진 제한을 PR 설명에 남깁니다.
