# Phase 12 - Render + Supabase Free Web Deployment Plan

## Status

In progress. Phase 12 has been redirected to Render + Supabase. Step 1 prepares and verifies the
repository deployment unit; Step 2 requires a Render account, a Supabase project, and provider
secrets. The earlier Oracle VM design remains documented as an alternative in ADR 0019.

## Objective

Publish the existing full-stack service to the web for a portfolio/demo workload with the smallest
operational surface, using two Render Docker Web Services and one Supabase PostgreSQL project.

## Design reasons and important decisions

- Render builds the existing backend and frontend Dockerfiles, preserving the reproducible local
  container boundary.
- The frontend Nginx proxy receives the backend public HTTPS URL through an environment variable. This
  keeps browser requests same-origin while remaining compatible with Render Free Web Service network
  restrictions.
- Supabase provides standard PostgreSQL. Flyway remains the only schema migration owner, and the
  existing pgvector migration remains responsible for enabling the `vector` extension.
- The deployment accepts free-tier sleep, low quotas, and limited recovery guarantees in exchange for
  simpler operations. It is not a production availability commitment.
- Gemini is the default server-side text/OCR provider. Local Ollama fallbacks are disabled because the
  Render services do not include an Ollama host; current embedding/RAG routes require a separately
  reachable embedding endpoint.

## Planned steps

### Step 1 - Managed deployment packaging and runbook

- Add the `prod` Spring profile with environment-backed datasource configuration and safe health output.
- Read Render's dynamic `PORT` while preserving the local `SERVER_PORT` override.
- Make the Nginx frontend proxy runtime-configurable for a Render backend HTTPS URL.
- Add `render.yaml`, a Supabase connection template, ADR, and Render/Supabase deployment runbook.
- Validate the merged Compose configuration and run the existing application checks.

### Step 2 - Render/Supabase provisioning and first deployment

- Create a Supabase project and copy its Session Pooler values into Render secrets.
- Create the Render services, configure the backend public HTTPS URL in the frontend proxy, and verify
  public frontend-to-backend smoke tests.
- Apply Flyway migrations and complete public HTTPS smoke tests.
- Export a disposable database backup and verify restoration before treating the deployment as durable.

## Explicitly excluded

- OCI VM provisioning, Kubernetes, image registry publication, and distributed infrastructure.
- Render Postgres, because the Free database expires after 30 days.
- Supabase Auth replacing the existing Spring Security/JWT boundary.
- Public exposure of PostgreSQL or Ollama.
- JobKorea credential collection or upload automation.
- Treating the free services as highly available or production-grade without measured reliability and
  recovery evidence.

## Completion criteria

- [ ] Supabase PostgreSQL is created and the Session Pooler connection is verified.
- [ ] The Render Blueprint creates the backend and frontend services.
- [ ] The public hostname serves the SPA over HTTPS.
- [ ] Backend readiness passes through the frontend proxy.
- [ ] Signup, login, owned CRUD, and resume version creation smoke tests pass.
- [ ] An AI smoke test passes when the Gemini key is configured; embedding/RAG remains explicitly
  unverified without an external embedding endpoint.
- [ ] A PostgreSQL export and restore are verified outside the live project.
- [ ] Free-tier limits and an incident recovery procedure are documented.
