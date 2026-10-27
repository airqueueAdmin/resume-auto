# ADR 0021: Use Render and Supabase for the First Managed Web Deployment

## Status

Accepted for the current Phase 12 deployment target. This supersedes the Oracle VM target for the
current deployment attempt; the Oracle design remains in ADR 0019 as an alternative for a later
single-host deployment.

## Context

The repository already contains a reproducible Spring Boot container, a React/Nginx container, Flyway
migrations, PostgreSQL integration tests, and a PostgreSQL + pgvector schema. The first public deployment
should be easy to operate for a portfolio/demo workload without introducing a VM, reverse-proxy host
administration, or a new authentication system.

The application owns authentication and authorization through Spring Security, short-lived access JWTs,
and persisted refresh sessions. Supabase should therefore provide PostgreSQL only; Supabase Auth is not
introduced as a second identity boundary.

## Decision

- Run the Spring Boot backend as a Render Docker web service.
- Run the React/Nginx frontend as a second Render Docker web service.
- Keep the frontend and backend in the same Render region and use Render private networking for the
  Nginx `/api` proxy.
- Use a Supabase Free project as the external PostgreSQL database. The application continues to own
  schema changes through Flyway, including the existing `vector` extension migration.
- Use the Supabase Session Pooler connection for the persistent Spring Boot backend on Render's IPv4
  network. Use SSL and do not use the transaction pooler for the application or Flyway migrations.
- Keep Gemini/OpenAI provider keys server-side in Render environment variables. Do not put secrets in
  the frontend bundle or repository.
- Disable local Ollama fallbacks in the Render environment. Retrieval indexing and RAG flows require a
  separately reachable embedding endpoint until a hosted embedding boundary is deliberately added.
- Use Render Blueprint configuration in `render.yaml`; do not create a Render Postgres resource because
  the free Render Postgres database expires after 30 days.

## Consequences

### Benefits

- No VM patching, Docker host administration, or public database port is required.
- Existing Dockerfiles and same-origin frontend proxy remain the deployment contract.
- Supabase remains standard PostgreSQL, so existing JDBC, Flyway, ownership constraints, and pgvector
  migrations remain applicable.
- Render health checks and Blueprint configuration provide a reproducible service definition.

### Trade-offs and limitations

- Render Free web services sleep after inactivity and have a cold-start delay.
- Supabase Free projects can be paused after low activity, have a limited database quota, and do not
  provide downloadable managed backups. The service is therefore a portfolio/demo deployment, not a
  production availability or recovery promise.
- The external database introduces network latency and a provider dependency.
- The current embedding adapter targets Ollama. AI flows that require embeddings remain unavailable
  unless an external Ollama embedding endpoint is configured.
- A future migration away from Supabase must preserve PostgreSQL compatibility and be performed through
  an explicit backup/export and Flyway verification process.

## Operational boundaries

- Never put `DB_PASSWORD`, JWT secrets, or LLM keys in `render.yaml`, the frontend, or committed `.env`
  files.
- Keep Supabase's database password separate from application-user passwords.
- Verify database export/restore before treating the deployment as durable.
- Do not replace the existing JWT identity boundary with Supabase Auth without a separate ADR and data
  migration plan.
