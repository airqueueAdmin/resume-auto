# ADR 0019: Use an Oracle Cloud Always Free VM for the First Web Deployment

## Status

Accepted for Phase 12.

## Context

The project has a reproducible Docker Compose delivery unit containing the React/Nginx frontend,
Spring Boot backend, and PostgreSQL + pgvector. The backend calls external Gemini/OpenAI providers
for text generation and OCR, and uses host Ollama only for embeddings. A deployment target must
therefore support a persistent single host, Docker Compose, PostgreSQL storage, and a small embedding
service while keeping the monthly cost at zero for the intended three-user scope.

Managed free hosting would split the application across services. That would require a CORS contract,
separate frontend API configuration, a managed PostgreSQL migration, and a replacement for the current
Ollama embedding boundary. Render's free PostgreSQL also expires after 30 days, which is unsuitable for
the persistent personal data held by this service.

## Decision

- Use one Oracle Cloud Infrastructure Always Free eligible Linux VM as the first web deployment target.
- Run the existing Docker Compose stack on the VM, with the database volume kept on the VM's persistent
  block storage.
- Run Ollama on the VM host only for embedding requests and keep the existing
  `host.docker.internal` bridge for that embedding boundary. Text-generation Ollama fallback remains
  opt-in and disabled in the Oracle environment.
- Bind PostgreSQL, backend, and frontend host ports to loopback. Expose only the HTTPS reverse proxy.
- Use a host-level Caddy or equivalent reverse proxy for TLS and the public hostname.
- Keep Gemini API access server-side through environment-injected secrets. Do not put provider keys in the
  frontend bundle or Git.
- Treat this as a single-host hobby deployment. Backups, restore testing, restart recovery, and manual
  release promotion remain operator responsibilities.

## Consequences

- The current same-origin frontend proxy can remain in place, so the first deployment does not need a
  cross-origin browser contract.
- PostgreSQL + pgvector and the existing Ollama embedding model remain available without a new AI
  provider integration.
- The VM is a single failure domain and has no managed database backup retention or automatic failover.
- Oracle account signup, region capacity, public IP, DNS, TLS, OS patching, and firewall configuration are
  deployment prerequisites outside the repository.
- A `prod` Spring profile is required so local health details are not exposed by the public deployment.
