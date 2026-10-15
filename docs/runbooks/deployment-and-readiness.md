# Deployment and Readiness Runbook

## Scope

This runbook operates the current single-host Docker Compose delivery unit. It is intentionally
provider-neutral and does not create cloud resources, publish images, or introduce Kubernetes, Redis,
queues, or a distributed tracing backend.

The current release model is:

1. CI validates a commit.
2. The operator checks out that exact commit on the application host.
3. The host injects configuration and secrets outside Git.
4. PostgreSQL is backed up before startup that may apply migrations.
5. Compose starts PostgreSQL, backend, and frontend in health order.
6. Readiness and smoke checks pass before traffic is accepted.

## Configuration and secrets

Copy `.env.example` only as a local configuration reference. Never commit `.env`, provider keys,
database passwords, or a production JWT secret. Production values must be injected by the host's
secret manager, protected environment configuration, or an equivalent deployment mechanism.

At minimum, replace the local defaults for:

- `POSTGRES_PASSWORD`
- `JWT_SECRET_BASE64`
- `JWT_ISSUER`
- `GEMINI_API_KEY` when Gemini OCR or resume drafting is enabled
- `OPENAI_API_KEY` when the OpenAI provider is selected
- `COMPOSE_OLLAMA_CHAT_URL` and `COMPOSE_OLLAMA_EMBEDDING_URL` when Ollama is not on the host

Use a unique, sufficiently long Base64 JWT secret. Rotating it invalidates access tokens signed by
the previous secret; users must authenticate again. Refresh-session rotation and revocation remain
database-backed, so the database backup and secret rotation should be coordinated.

Before startup, validate the rendered configuration without exposing values in logs:

```powershell
docker compose config --quiet
docker compose config --environment
```

Review the rendered structure and verify that secrets are supplied by the deployment environment,
not by a committed file or image layer.

## Release preflight

- Confirm the intended Git commit and that its remote CI run is successful.
- Confirm Docker Engine and Compose versions are the versions used by the release evidence.
- Confirm disk space for the new images, PostgreSQL data, and one backup copy.
- Confirm the PostgreSQL backup destination is outside the container writable layer.
- Confirm provider dependencies are available if the release enables AI or embedding routes.
- Confirm the release does not rely on a local Ollama process unless the host bridge is configured.

The current Compose file builds from the checked-out source. Record the source commit and image
metadata before rollout:

```powershell
git rev-parse HEAD
docker compose build backend frontend
docker image inspect ai-career-platform-backend ai-career-platform-frontend `
  --format '{{.RepoTags}} {{.Id}} {{.Size}}'
```

The backend Dockerfile retries Maven dependency resolution and packaging up to five times,
with a 5-to-25 second backoff, because a transient Maven Central `502 Bad Gateway` must not
invalidate an otherwise reproducible image build.

## Backup and restore

Create a plain SQL backup before a release that can apply Flyway migrations. The command uses the
PostgreSQL service environment and does not place a database password in the command line:

```powershell
$backupDir = "backups"
New-Item -ItemType Directory -Path $backupDir -Force | Out-Null
$backupFile = Join-Path $backupDir ("ai-career-" + (Get-Date -Format "yyyyMMdd-HHmmss") + ".sql")
docker compose exec -T postgres pg_dump `
  -U $env:POSTGRES_USER `
  -d $env:POSTGRES_DB `
  --no-owner --no-privileges > $backupFile
Get-Item $backupFile
```

Keep the backup outside the Compose volume and verify that it is non-empty. Test restoration into a
disposable PostgreSQL database before treating the backup as a recovery point. Do not restore over
the live database without a maintenance window and an explicit data-loss decision:

```powershell
Get-Content -Raw $backupFile | docker compose exec -T postgres `
  psql -U $env:POSTGRES_USER -d $env:POSTGRES_DB
```

The restore command is an example for a controlled disposable restore. A production restore must
first stop application traffic, preserve the failed database for investigation, and verify row counts
and Flyway history after restoration.

## Migration rollout

Flyway is the only schema migration owner. Do not use Hibernate schema generation or `flyway clean`
as a deployment shortcut.

1. Back up PostgreSQL.
2. Start or verify PostgreSQL health:

   ```powershell
   docker compose up -d postgres
   docker compose ps postgres
   ```

3. Start the backend from the checked-out release:

   ```powershell
   docker compose up -d backend
   docker compose logs --no-log-prefix backend
   ```

4. Confirm Flyway validation and migration output, then verify readiness.
5. Start or update the frontend only after backend readiness succeeds:

   ```powershell
   docker compose up -d frontend
   ```

Migrations must be additive or backward-compatible with the application version that may still be
serving traffic. Destructive changes require a separately reviewed migration plan, backup, and a
forward-fix strategy; do not assume that an older image can run against a newer schema.

## Rollback

Rollback means restoring the application image/source version and configuration, not blindly reversing
database migrations. The safe sequence is:

1. Stop promotion and preserve logs, the current commit, and the database backup.
2. Revert backend/frontend to the last known-good release artifact or checked-out commit.
3. Restart the application services and verify readiness.
4. If the schema is backward-compatible, continue serving with the previous application version.
5. If the schema is not backward-compatible, use the reviewed forward fix. Restore the database only
   with an approved outage and verified backup, because restoration loses writes after the backup.

There is no automatic rollback or traffic router in the current single-host setup.

## Readiness and smoke checks

Run all checks after startup or rollback:

```powershell
docker compose ps
Invoke-RestMethod http://localhost:8080/actuator/health/liveness
Invoke-RestMethod http://localhost:8080/actuator/health/readiness
Invoke-RestMethod http://localhost:5173/actuator/health/readiness
Invoke-WebRequest http://localhost:5173/ -UseBasicParsing
```

Acceptance criteria:

- PostgreSQL is `healthy` and accepts `pg_isready` checks.
- Backend readiness returns HTTP 200.
- Frontend health is `healthy` and the frontend-proxied readiness returns HTTP 200.
- SPA deep links return the frontend shell rather than an Nginx 404.
- Logs show no migration failure, datasource failure, or repeated restart loop.
- A representative authenticated API smoke test succeeds without logging an access token.

Readiness is a dependency and startup signal. It is not a throughput, availability, or business
correctness SLO.

## Incident checks

Start with the narrowest relevant evidence:

```powershell
docker compose ps --all
docker compose logs --tail=200 postgres
docker compose logs --tail=200 backend
docker compose logs --tail=200 frontend
docker stats --no-stream
```

- If PostgreSQL is unhealthy, check disk space, credentials, port conflicts, and the PostgreSQL log.
- If Flyway fails, stop promotion, preserve the backup, and inspect the migration error before any
  retry. Never delete the volume as a first response.
- If backend readiness fails, check datasource connectivity, migration status, and restart loops.
- If frontend health passes but proxied readiness fails, check backend health and Nginx upstream
  connectivity.
- If an AI route fails, distinguish provider unavailability from database or application failure;
  request IDs may be used to correlate sanitized logs without recording prompts or secrets.

## Measured local baseline

Measured on 2026-10-15 on the local Windows/Docker Desktop environment using a separate
`phase11-step3` Compose project, a new PostgreSQL volume, already-built images, and temporary host
ports. The measurement is a cold container/database start, not a host boot or image pull benchmark.

| Measurement | Result |
| --- | ---: |
| Compose start to backend readiness HTTP 200 | 22.4 s |
| Compose start to frontend HTTP 200 | 22.4 s |
| Backend image size | 135,757,142 bytes (about 135.8 MB) |
| Frontend image size | 26,353,719 bytes (about 26.4 MB) |
| PostgreSQL/pgvector image size | 160,561,945 bytes (about 160.6 MB) |
| Backend readiness, 20 samples | p50 19.98 ms; p95 25.27 ms; max 29.16 ms |
| Frontend-proxied readiness, 20 samples | p50 19.20 ms; p95 24.94 ms; max 24.97 ms |

All 40 readiness requests returned HTTP 200. These are local loopback observations with warm images
and no concurrent load. They are a baseline for later comparison, not production capacity evidence.
No cloud platform, replica count, queue, Redis deployment, or SLO is selected from this sample alone.

## Review triggers

Revisit the architecture only when measurements show a need: sustained request latency, concurrent
load, restart recovery, database connection pressure, provider timeout cost, backup duration, or
operational toil. Until then, keep the single-instance boundary explicit and improve the runbook and
tests before adding distributed infrastructure.
