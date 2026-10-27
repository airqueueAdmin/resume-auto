# Oracle Cloud Always Free VM 배포 운영 가이드

## 범위

이 가이드는 현재 React/Nginx, Spring Boot, PostgreSQL + pgvector, 임베딩 전용 Ollama 스택을 Oracle Cloud
Infrastructure Linux VM 한 대에 배포하는 절차를 설명한다. 텍스트 생성·OCR은 외부 Gemini/OpenAI
provider를 사용하며, 최대 3명의 사용자가 적은 빈도로
사용하는 환경을 대상으로 하며, 기존 Compose 동일 출처 프록시 구조를 유지하기 위해 모든
애플리케이션을 한 호스트에서 실행한다.

Oracle Always Free 리소스의 사용 가능 여부는 테넌시의 Home Region과 현재 리소스 수용량에
따라 달라진다. OCI 콘솔에서 Always Free eligible로 표시되는 shape과 스토리지만 사용한다.
수용량 부족을 해결하기 위해 유료 shape으로 변경할 때는 예상 비용을 먼저 확인한다.

현재 기준 문서: [Oracle Always Free 리소스](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm)

## 1. VM 생성

OCI에서 테넌시의 Home Region에 Always Free eligible Ampere A1 Flex shape을 사용하는 Linux VM을
생성한다. 콘솔에 표시되는 무료 할당량을 기준으로 처음에는 2 OCPU와 12GB 메모리를 사용한다.
초기 boot volume은 50GB로 설정하고, boot volume과 block volume의 전체 사용량이 Always Free
한도를 넘지 않도록 한다.

공인 IPv4 주소를 할당한다. 다음 인바운드 규칙을 추가한다.

- TCP 22: 가능하면 본인의 고정된 관리용 IP에서만 허용
- TCP 80: 인증서 발급과 HTTP → HTTPS 리디렉션을 위해 인터넷에서 허용
- TCP 443: 인터넷에서 허용

5432, 8080, 5173, 11434 포트에는 공용 인바운드 규칙을 추가하지 않는다. 애플리케이션과
Ollama 포트는 loopback 또는 Docker 내부 네트워크에 바인딩한다.

## 2. 호스트 의존성 설치

SSH로 접속한 뒤 선택한 Linux 배포판에 맞는 공식 설치 방법으로 Docker Engine, Compose plugin,
Git, Ollama를 설치한다. Ollama는 Docker가 접근할 수 있는 호스트 인터페이스에서 수신하도록
설정하되, OCI 보안 목록이나 호스트 방화벽에서 11434 포트를 외부에 공개하지 않는다.

Linux 호스트 브리지를 사용하는 경우 Ollama가 `0.0.0.0:11434`에서 수신하도록 설정하고 다음
명령으로 로컬 응답을 확인한다.

```bash
curl http://127.0.0.1:11434/api/tags
```

현재 임베딩에 사용하는 모델만 내려받는다.

```bash
ollama pull nomic-embed-text
ollama list
```

Always Free A1 VM의 메모리는 애플리케이션 스택과 공유되므로 텍스트 생성용 Ollama fallback은
비활성화한다. 생성·자기소개 답변·OCR은 외부 Gemini/OpenAI provider를 사용하고, Ollama는
`nomic-embed-text` 임베딩 요청만 처리한다.

## 3. 릴리스 체크아웃과 보안값 생성

CI 검사를 통과한 검토 완료 commit을 사용한다.

```bash
git clone <repository-url> ai-career-platform
cd ai-career-platform
git checkout <reviewed-commit>
cp .env.oracle.example .env.oracle
chmod 600 .env.oracle
```

`.env.oracle`에서 PostgreSQL 비밀번호, JWT secret, 이메일 인증 secret, Gmail SMTP 인증 정보,
발신자 주소, Gemini key를 교체한다. JWT secret과 이메일 인증 secret은 서로 독립적으로 생성하며,
각각 디코딩했을 때 최소 32바이트의 무작위 값이 되도록 한다. 이 파일은 VM에만 보관하고 Git에
추가하지 않는다.

```bash
openssl rand -base64 32
openssl rand -base64 32
```

Gemini를 사용하는 경우 `GEMINI_API_KEY`는 backend 환경에만 둔다. React 빌드 환경이나 브라우저
JavaScript에 넣지 않는다.

이메일 인증을 시작하기 전에 전용 Gmail 계정을 준비한다.

1. 서비스 이메일 인증 발송만 사용할 Gmail 계정을 만든다.
2. 해당 Google 계정에서 2단계 인증을 활성화한다.
3. backend용 Google 앱 비밀번호를 만들고, 16자리 값을 공백 없이 복사한다.
4. `SMTP_USERNAME`과 `EMAIL_FROM_ADDRESS`를 해당 Gmail 주소로 설정한다.
5. 앱 비밀번호를 `SMTP_PASSWORD`에 넣는다. 일반 Google 계정 비밀번호는 사용하거나 저장하지 않는다.
6. `SMTP_HOST=smtp.gmail.com`, 포트 `587`, STARTTLS 활성화, implicit SSL 비활성화 설정을 유지한다.

이메일 인증이 활성화되어 있는데 필수 SMTP 설정이 비어 있으면 backend가 의도적으로 시작에
실패한다. SMTP 인증 정보를 frontend 이미지나 소스 저장소에 넣지 않는다. 기본 보호 설정은
클라이언트별 가입/재발송 요청을 1시간에 20회, 서비스 전체의 실제 인증 메일 발송을 하루 최대
400회로 제한한다. 이 카운터는 메모리에만 저장되어 backend 재시작 시 초기화된다. backend를
여러 개 실행하기 전에는 공유되고 영속적인 rate limiter로 교체해야 한다.

## 4. Compose 검증과 시작

시작하기 전에 병합된 Compose 설정을 렌더링한다.

```bash
docker compose --env-file .env.oracle \
  -f docker-compose.yml \
  -f docker-compose.oracle.yml \
  config --quiet
```

스택을 시작한다.

```bash
docker compose --env-file .env.oracle \
  -f docker-compose.yml \
  -f docker-compose.oracle.yml \
  up -d --build
```

서비스 상태와 readiness를 확인한다.

```bash
docker compose --env-file .env.oracle -f docker-compose.yml -f docker-compose.oracle.yml ps
curl http://127.0.0.1:8080/actuator/health/readiness
curl http://127.0.0.1:5173/
```

Flyway는 backend 시작 과정에서 실행된다. Migration 오류를 해결하기 위해 `postgres_data`
volume을 삭제하지 않는다. 먼저 로그와 데이터베이스를 보존한다.

## 5. HTTPS 추가

`career.example.com`과 같은 DNS `A` 레코드를 VM 공인 IP로 연결한다. 호스트에 Caddy를 설치하고
다음과 같은 설정을 사용한다.

```text
career.example.com {
    reverse_proxy 127.0.0.1:5173
}
```

Caddy를 reload하고 브라우저에서 SPA가 HTTPS로 열리는지 확인한다. 이후 frontend Nginx 컨테이너가
Compose 네트워크를 통해 `/api`와 `/actuator` 요청을 backend로 프록시하여 동일 출처 계약을
유지한다. frontend 컨테이너의 포트를 직접 공용 포트로 노출하지 않는다.

## 6. 최초 스모크 테스트

공개 HTTPS 주소에서 다음 항목을 확인한다.

1. SPA 루트와 브라우저 deep link가 정상적으로 열린다.
2. 가입 시 이메일 인증 메일이 도착하고, 잘못된 코드는 거부되며, 올바른 코드는 계정을 활성화하고,
   활성화 전에는 로그인이 허용되지 않는다.
3. 인증된 사용자로 Experience 또는 Project 하나를 생성하고 다시 조회할 수 있다.
4. JobPosting을 저장할 수 있다.
5. 인증된 사용자의 Ollama embedding indexing이 성공한다.
6. 설정된 provider를 사용하는 grounding 기반 AI 흐름 하나가 성공한다.
7. 이후 새 ResumeVersion을 생성해도 이전 ResumeVersion이 변경되지 않는다.

문제를 진단할 때 access token, 비밀번호, 이력서 내용, provider key를 로그에 남기거나 붙여 넣지
않는다.

## 7. 릴리스 전 백업

Migration이나 애플리케이션 변경 전에 실행 중인 Compose volume과 별도의 위치에 PostgreSQL dump를
생성한다.

```bash
mkdir -p backups
backup="backups/ai-career-$(date -u +%Y%m%d-%H%M%S).sql"
docker compose --env-file .env.oracle -f docker-compose.yml -f docker-compose.oracle.yml \
  exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --no-owner --no-privileges' \
  > "$backup"
test -s "$backup"
```

백업을 별도의 저장 위치에도 복사하고, 임시 PostgreSQL 인스턴스에 복원하는 테스트를 수행한다.
VM이나 volume 장애로 유일한 백업까지 잃지 않도록 최소 하나의 백업은 VM 외부에 보관한다.

## 8. 업데이트와 롤백

업데이트 전에 현재 commit을 기록하고 PostgreSQL을 백업한 뒤 병합된 Compose 설정을 렌더링한다.
그 다음 검토된 commit을 가져오고 다시 빌드한다.

```bash
git
docker compose --env-file .env.oracle -f docker-compose.yml -f docker-compose.oracle.yml \
  up -d --buildrev-parse HEAD
```

스키마가 이전 버전과 호환되는 경우에만 애플리케이션을 이전 검토 commit으로 되돌린다. Flyway
Migration을 자동으로 역방향 실행하지 않는다. 데이터베이스 복원은 백업 이후에 기록된 변경사항을
잃을 수 있는 명시적인 데이터 손실 작업이다.

## 9. 비용과 보안 점검

- OCI compute, volume, network 리소스가 모두 Always Free eligible로 표시되는지 확인한다.
- OCI가 테넌시에 결제 계정을 요구하는 경우 billing budget 또는 사용량 알림을 설정한다.
- 5432, 8080, 5173, 11434 포트를 외부에 공개하지 않는다.
- VM, shell history, 환경 파일이 노출되면 JWT와 provider secret을 교체한다.
- 호스트 OS, Docker 이미지, 애플리케이션 의존성을 정기적으로 업데이트한다.
- PostgreSQL 데이터, Docker layer, Ollama 모델, 백업이 VM 저장 공간을 함께 사용하므로 디스크
  사용량을 재확인한다.
- 이력서나 채용공고 데이터가 Gemini로 전송될 때는 Gemini를 외부 데이터 처리자로 취급한다.
  데이터 정책상 로컬 추론이 필요하고 관련 OCR 기능이 필요하지 않은 경우 Ollama 전용 경로를
  사용한다.
