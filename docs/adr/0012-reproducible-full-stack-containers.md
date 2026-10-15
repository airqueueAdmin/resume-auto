# ADR 0012: Reproducible Full-Stack Containers

## Status

Accepted for Phase 11 Step 1.

## Context

The application had repeatable PostgreSQL startup but required separate host Java and Vite processes.
That is convenient for development but does not prove that a reviewer can build and run the complete
system as a consistent delivery unit. Phase 9 deliberately deferred cloud and distributed
infrastructure because no measured scaling need exists.

## Decision

- Package the Spring Boot backend and React frontend as separate runtime images.
- Use multi-stage builds so Maven/JDK and npm/Node build toolchains do not remain in runtime images.
- Run the backend as a non-root user.
- Serve frontend assets through Nginx with same-origin `/api` and `/actuator` reverse proxying and SPA
  fallback.
- Extend the existing Compose project with health-ordered backend and frontend services while retaining
  the ability to start PostgreSQL alone.
- Reach a host Ollama instance through configurable `host.docker.internal` URLs; do not bundle model
  weights into application images.
- Keep cloud deployment, image publication, Redis, queues, and orchestration platforms deferred.

## Consequences

Positive:

- Reviewers can reproduce the full stack without installing Java, Maven, Node, or Nginx on the host.
- Runtime images have smaller attack and maintenance surfaces than build images.
- Same-origin delivery avoids a second browser CORS configuration.
- Existing local Java/Vite workflows remain available.

Trade-offs:

- Docker builds download toolchain and dependency layers on a cold cache.
- `host.docker.internal` is a local Compose bridge, not a production model-service topology.
- Compose health and single-instance operation do not prove production scalability or availability.
