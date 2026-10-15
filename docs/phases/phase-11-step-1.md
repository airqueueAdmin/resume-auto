# Phase 11 Step 1 - Reproducible Full-Stack Containers

## Status

Complete for this step. Phase 11 remains in progress.

## Objective and scope

Package the existing frontend, backend, and PostgreSQL stack so a reviewer can build and run the same
Java 25/Spring Boot, React, Nginx, and pgvector boundaries with one Docker Compose project.

## Design reasons and important decisions

- Backend and frontend remain separate runtime images so static delivery and API lifecycle can evolve
  independently.
- Multi-stage builds keep JDK, Maven, Node, and TypeScript toolchains out of runtime images.
- The backend runs as a non-root `app` user.
- Nginx serves immutable hashed assets, proxies same-origin API/health requests, and falls back to
  `index.html` for React BrowserRouter deep links.
- Compose waits for PostgreSQL health before backend startup and backend readiness before frontend
  startup.
- Host Ollama is reached through `host.docker.internal`; no model is bundled into an application image.

## Implemented

- Root backend multi-stage `Dockerfile` using Java 25 JDK/JRE Alpine images
- Frontend multi-stage `Dockerfile` using Node 24 and Nginx 1.29 Alpine images
- Non-root backend runtime
- Nginx `/api` and `/actuator` reverse proxy, asset caching, and SPA fallback
- Full-stack Compose services and health-ordered dependencies
- Separate backend/frontend Docker build contexts with secret and build-output exclusions
- Configurable host ports and Compose-specific Ollama endpoints

## Explicitly excluded

- Cloud deployment, HTTPS termination, DNS, managed secrets, or external image registry
- Horizontal scaling or distributed rate limiting
- Automatic migrations outside normal Spring Boot/Flyway startup
- CI image publication and production rollout

## Verification

- `docker compose config` passed.
- Backend and frontend images built successfully from clean multi-stage build contexts.
- PostgreSQL, backend, and frontend containers started with health-ordered dependencies.
- Direct backend readiness returned HTTP 200.
- Nginx-proxied readiness returned HTTP 200.
- `/login` returned the SPA document with the application root.
- Backend runtime identity is non-root (`uid=100(app)`).
- Existing backend suite remains green at 98 tests, and the frontend production build passes.

## Failures encountered and root causes

- Initial image downloads and Maven offline dependency resolution took longer than normal because the
  local Docker cache was empty. Both builds completed without code or configuration changes.
- The first frontend healthcheck used `localhost`, which resolved to IPv6 inside the Nginx image while
  Nginx listened on IPv4. Pinning the internal probe to `127.0.0.1` corrected the health status.
- Docker reported that the local client credential config was unreadable while resolving public image
  manifests. Public image inspection and builds still succeeded; no registry credentials were required.

## Concepts to understand

- Reproducible packaging and actual production deployment are different milestones.
- Health-ordered startup reduces race conditions but does not replace retries or orchestration policy.
- Same-origin proxying avoids a separate browser CORS contract for the deployed frontend.
- Non-root execution limits impact but does not remove the need for image and dependency scanning.

## Completion criteria

- [x] Frontend and backend runtime images build successfully.
- [x] Runtime images exclude source build toolchains and local secret files.
- [x] Backend runs without root privileges.
- [x] Compose starts PostgreSQL, backend, and frontend in health order.
- [x] API health proxy and SPA deep-link fallback work.
- [x] Existing automated tests and frontend build remain green.

## Next step

Phase 11 Step 2 adds a non-deploying CI quality gate for tests, frontend build, Compose validation,
container builds, and vulnerability reporting.
