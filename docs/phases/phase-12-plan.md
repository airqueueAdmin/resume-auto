# Phase 12 - Oracle VM Free Web Deployment Plan

## Status

In progress. Phase 12 has been authorized as the next phase. Step 1 prepares and verifies the
repository deployment unit; Step 2 requires an OCI account, an available Always Free VM, and a user
controlled DNS/TLS target.

## Objective

Publish the existing full-stack service to the web for no monthly infrastructure charge within the
intended three-user scope, using one Oracle Cloud Always Free VM and the existing Docker Compose
topology.

## Design reasons and important decisions

- The current Compose topology already keeps the frontend, backend, PostgreSQL + pgvector, and Ollama
  integration on one host, which avoids adding a cross-origin API contract for the first deployment.
- Oracle's Always Free VM is selected because it can run persistent Docker Compose services and a small
  local Ollama model. The deployment accepts single-host availability and manual operations as the
  trade-off for a zero monthly infrastructure target.
- The PostgreSQL volume remains persistent on the VM. Database backups are created with `pg_dump` and
  copied outside the live container volume.
- Only the reverse proxy is public. Database, backend, frontend, and Ollama ports bind to loopback or
  the internal Compose network.
- Gemini remains an optional server-side provider. Its quota and data-handling policy are external
  dependencies; Ollama remains required for the current embedding index path.

## Planned steps

### Step 1 - Production Compose packaging and deployment runbook

- Add the `prod` Spring profile with environment-backed datasource configuration and safe health output.
- Add Compose bind-address variables and an Oracle override that selects the prod Spring profile; the
  Oracle environment binds application ports to loopback.
- Add an Oracle environment template, ADR, and VM deployment runbook.
- Validate the merged Compose configuration and run the existing application checks.

### Step 2 - OCI VM provisioning and first deployment

- Create an Always Free eligible Linux VM in the OCI Home Region.
- Install Docker, Compose, Ollama, and the host reverse proxy.
- Pull `llama3.1:8b` and `nomic-embed-text` and verify container-to-host Ollama access.
- Start the stack from a reviewed commit, apply Flyway migrations, and complete public HTTPS smoke tests.
- Create and verify the first external PostgreSQL backup.

## Explicitly excluded

- Kubernetes, managed database migration, image registry publication, and automatic deployment.
- Public exposure of PostgreSQL, the Spring Boot port, Nginx port, or Ollama.
- JobKorea credential collection or upload automation.
- Treating the free VM as highly available or production-grade without measured reliability evidence.

## Completion criteria

- [ ] The VM is created with only Always Free eligible resources.
- [ ] The public hostname serves the SPA over HTTPS.
- [ ] Backend readiness passes through the frontend proxy.
- [ ] Signup, login, owned CRUD, resume version creation, and one AI/indexing smoke flow pass.
- [ ] PostgreSQL backup and restore are verified outside the live volume.
- [ ] Monthly cost controls and an incident recovery procedure are documented.
