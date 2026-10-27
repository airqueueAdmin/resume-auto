# Render + Supabase 배포 운영 가이드

## 범위

이 가이드는 현재 저장소를 Render와 Supabase Free 플랜에 배포하기 위한 첫 번째 관리형 웹 배포
절차입니다.

- `career-note-api`: Render Docker Web Service, Spring Boot
- `career-note-web`: Render Docker Web Service, React 정적 파일과 Nginx 프록시
- PostgreSQL: Supabase Free Project
- 스키마: Spring Boot 기동 시 Flyway가 적용

Render의 무료 PostgreSQL은 사용하지 않습니다. 무료 Render PostgreSQL은 생성 후 30일이 지나면
만료되므로 개인 데이터의 장기 저장소로 적합하지 않습니다.

## 1. Supabase 프로젝트 생성

1. Supabase에서 새 프로젝트를 생성합니다.
2. 한국 사용자와 가까운 리전을 선택합니다.
3. 프로젝트가 준비되면 `Connect` 메뉴를 엽니다.
4. `Session pooler` 연결 정보를 복사합니다.

Spring Boot와 Flyway에는 다음 형태의 값을 사용합니다.

```text
DB_URL=jdbc:postgresql://<session-pooler-host>:5432/postgres?sslmode=require
DB_USERNAME=postgres.<project-ref>
DB_PASSWORD=<database-password>
```

시간 컬럼은 PostgreSQL `TIMESTAMPTZ`로 저장하며, 애플리케이션 DB 세션의 표시 기준은
`Asia/Seoul`로 설정됩니다. 실제 시점은 보존하면서 `created_at`과 `updated_at`을 KST로
확인하는 방식입니다. Supabase SQL Editor에서 직접 조회할 때 UTC로 표시되면 다음처럼
해당 조회 세션의 기준을 바꿉니다.

```sql
SET TIME ZONE 'Asia/Seoul';
SELECT created_at, updated_at FROM experiences ORDER BY created_at DESC LIMIT 10;
```

`TIMESTAMPTZ`는 내부적으로 절대 시점을 저장하므로 기존 데이터에 9시간을 더하는
마이그레이션은 실행하지 않습니다. 그렇게 하면 실제 시점이 중복으로 이동합니다.

웹 화면과 Chrome 확장 프로그램도 API의 UTC 시각을 `Asia/Seoul` 기준으로 변환해 생성·수정
시각을 표시합니다.

Transaction pooler의 포트 `6543`은 사용하지 않습니다. Spring JDBC와 Flyway가 세션 상태와
prepared statement를 사용할 수 있으므로 Session pooler 포트 `5432`를 사용합니다.

마이그레이션 `V6__create_embedding_chunks.sql`이 PostgreSQL `vector` 확장을 활성화합니다.
마이그레이션이 실패하면 테이블을 수동으로 생성하거나 Hibernate `ddl-auto`를 변경하지 말고,
Supabase PostgreSQL의 확장 권한과 Flyway 오류를 먼저 확인합니다.

## 2. Render Blueprint 생성

1. 저장소를 GitHub에 push합니다.
2. Render에서 `New > Blueprint`를 선택합니다.
3. 저장소를 연결하고 루트의 `render.yaml`을 사용합니다.
4. Blueprint가 요구하는 비밀값을 Render Dashboard에서 입력합니다.

현재 `render.yaml`은 `onCommit`으로 설정되어 있습니다. Render의 `After CI Checks Pass`는 새
커밋에서 CI 체크가 하나라도 감지되고 모두 성공해야 배포하므로, 원격 CI가 아직 연결되지 않은
초기 배포에서는 사용하지 않습니다. GitHub Actions 등 CI 체크가 실제로 실행되는 것을 확인한 뒤
두 서비스의 `autoDeployTrigger`를 `checksPass`로 바꾸면 됩니다. CI 체크가 0개이면 Render는
배포를 시작하지 않습니다.

필수 비밀값:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `GEMINI_API_KEY`

`JWT_SECRET_BASE64`와 `EMAIL_VERIFICATION_HMAC_SECRET_BASE64`는 Blueprint의
`generateValue`로 생성됩니다. 생성 후 값을 코드나 문서에 복사하지 않습니다.

Render Free Web Service는 private network 요청을 보낼 수는 있지만 다른 Free Web Service가
private network로 받는 구성에는 제한이 있으므로, 현재 프론트엔드는 백엔드의 공개 HTTPS URL을
Nginx 프록시 대상으로 사용합니다. `career-note-web`의 환경변수는 다음처럼 설정합니다.

```text
BACKEND_BASE_URL=https://<backend-service>.onrender.com
```

`http://`, private hostname, 포트만 있는 값은 입력하지 않습니다. 브라우저에는 프론트엔드
origin만 노출되고 Nginx가 `/api`와 `/actuator` 요청을 백엔드 공개 URL로 전달하므로 일반 API
CORS 설정은 추가하지 않아도 됩니다.

## 3. 첫 배포 확인

Render 배포 로그에서 다음을 확인합니다.

1. 백엔드 이미지가 Java 프로세스를 시작합니다.
2. Supabase 연결이 성공합니다.
3. Flyway가 모든 migration을 적용합니다.
4. `/actuator/health/readiness`가 HTTP 200을 반환합니다.
5. 프론트엔드 `/`가 HTTP 200을 반환합니다.
6. 프론트엔드에서 `/api/v1/auth/signup` 요청이 백엔드로 전달됩니다.

Render 서비스 URL에서 다음을 확인합니다.

```text
https://<web-service>.onrender.com/
https://<web-service>.onrender.com/actuator/health/readiness
```

백엔드 URL을 직접 공개하는 경우에도 `career-note-api`의 `/actuator` 응답에는 상세 내부 정보가
노출되지 않아야 합니다. 기본 공개 진입점은 프론트엔드 서비스로 유지합니다.

## 4. 기능 스모크 테스트

최소한 다음 순서로 확인합니다.

1. 회원가입
2. 로그인
3. `GET /api/v1/users/me`
4. Experience 생성·조회·수정
5. Project 생성·조회·수정
6. JobPosting 생성·조회
7. Resume과 immutable ResumeVersion 생성
8. 로그아웃 후 Refresh Session 재사용 거부
9. 회원탈퇴 후 재로그인·Refresh Session 재사용 거부
10. 다른 사용자의 리소스 ID에 대한 `404` 확인

Gemini 키를 설정한 경우 Job Description 분석을 추가로 확인합니다. 현재 배포 Blueprint는
Ollama fallback을 비활성화합니다. 따라서 Ollama가 별도로 연결되지 않은 상태에서 embedding
index, RAG matching, grounded resume generation을 성공했다고 간주하지 않습니다.

## 5. 데이터 보호와 무료 플랜 제한

- Supabase Free 프로젝트가 저활동으로 일시 중지될 수 있음을 전제로 합니다.
- Free 플랜의 데이터베이스 용량과 egress 한도를 Dashboard에서 주기적으로 확인합니다.
- Supabase Free 플랜에 의존하기 전에 `pg_dump` 또는 별도 export를 수행하고, 복구 가능한지
  disposable PostgreSQL에 검증합니다.
- Render 무료 Web Service는 유휴 후 sleep하고 다음 요청에서 cold start가 발생할 수 있습니다.
- 사용자에게 제공할 공개 서비스나 지속적인 가용성이 필요해지면 Render와 Supabase의 유료 플랜,
  백업, 모니터링, 비용 상한을 별도로 검토합니다.

## 6. 실패 시 확인 순서

- Flyway 실패: Supabase 연결 문자열, SSL, 사용자명, 비밀번호, `vector` 확장 오류를 확인합니다.
- Render 포트 오류: Spring Boot가 Render가 주입한 `PORT`를 사용하고, 프론트엔드 Nginx가
  `PORT`와 `BACKEND_BASE_URL`을 사용하도록 확인합니다.
- 프론트엔드 502: 백엔드 공개 HTTPS URL, Render 백엔드의 health check, Nginx의 TLS proxy 설정을
  확인합니다.
- 로그인 실패: JWT secret이 배포 사이에 변경되지 않았는지와 브라우저 저장소의 이전 토큰을
  확인합니다.
- AI 실패: Gemini API quota·키·timeout과 Ollama fallback 비활성화 상태를 분리해서 확인합니다.

비밀값을 로그에 출력하거나, 사용자 Access JWT와 Refresh Token을 문제 재현 자료에 포함하지
않습니다.
