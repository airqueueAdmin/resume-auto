# Oracle Cloud Always Free VM Deployment Runbook

## Scope

This runbook deploys the current React/Nginx, Spring Boot, PostgreSQL + pgvector, and Ollama stack to
one Oracle Cloud Infrastructure Linux VM. It is intended for at most three low-volume users and keeps
the application on one host to preserve the existing same-origin Compose proxy.

Oracle Always Free resource availability depends on the tenancy Home Region and current capacity. Use
only shapes and storage that the OCI console labels Always Free eligible. Do not upgrade a resource to a
paid shape just to work around capacity without reviewing the cost impact first.

Current reference: [Oracle Always Free resources](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm).

## 1. Create the VM

In OCI, create a Linux VM in the tenancy Home Region with an Always Free eligible Ampere A1 Flex shape.
Start with the free allocation shown in the console, such as 2 OCPUs and 12 GB memory. Use a 50 GB
boot volume initially and keep the total boot plus block volume allocation within the Always Free limit.

Assign a public IPv4 address. Add ingress rules for:

- TCP 22 from your own fixed administration IP, if possible
- TCP 80 from the public internet for certificate issuance and HTTP to HTTPS redirect
- TCP 443 from the public internet

Do not add public ingress rules for 5432, 8080, 5173, or 11434. The application and Ollama ports are
bound to loopback or the internal Docker network.

## 2. Install host dependencies

Connect over SSH and install Docker Engine, the Compose plugin, Git, and Ollama using the vendors'
current installation instructions for the selected Linux distribution. Keep Ollama listening on the
host interface used by Docker, but do not open port 11434 in the OCI security list or host firewall.

For a Linux host bridge, configure Ollama to listen on `0.0.0.0:11434` and verify locally:

```bash
curl http://127.0.0.1:11434/api/tags
```

Pull the models used by the current application:

```bash
ollama pull llama3.1:8b
ollama pull nomic-embed-text
ollama list
```

The 8B model may respond slowly on a small CPU VM. The application already has bounded timeouts; treat
an Ollama timeout as a provider availability issue rather than increasing every timeout without a
measurement.

## 3. Check out the release and create secrets

Use a reviewed commit whose CI checks have passed:

```bash
git clone <repository-url> ai-career-platform
cd ai-career-platform
git checkout <reviewed-commit>
cp .env.oracle.example .env.oracle
chmod 600 .env.oracle
```

Edit `.env.oracle` and replace the PostgreSQL password, JWT secret, and Gemini key. Generate a JWT
secret with at least 32 decoded random bytes. Keep the file on the VM and outside Git.

If Gemini is enabled, keep `GEMINI_API_KEY` in the backend environment only. Never put it in the React
build environment or browser JavaScript.

## 4. Validate and start Compose

Render the merged files before starting anything:

```bash
docker compose --env-file .env.oracle \
  -f docker-compose.yml \
  -f docker-compose.oracle.yml \
  config --quiet
```

Start the stack:

```bash
docker compose --env-file .env.oracle \
  -f docker-compose.yml \
  -f docker-compose.oracle.yml \
  up -d --build
```

Check service state and readiness:

```bash
docker compose --env-file .env.oracle -f docker-compose.yml -f docker-compose.oracle.yml ps
curl http://127.0.0.1:8080/actuator/health/readiness
curl http://127.0.0.1:5173/
```

Flyway runs during backend startup. Do not delete the `postgres_data` volume to fix a migration error;
preserve the logs and database first.

## 5. Add HTTPS

Point a DNS `A` record such as `career.example.com` at the VM public IP. Install Caddy on the host and
use a configuration equivalent to:

```text
career.example.com {
    reverse_proxy 127.0.0.1:5173
}
```

Reload Caddy and verify that the browser loads the SPA over HTTPS. The frontend Nginx container then
proxies `/api` and `/actuator` to the backend on the Compose network, preserving the same-origin
contract. Do not expose the frontend container directly as a public port.

## 6. First smoke test

From the public HTTPS hostname, verify:

1. SPA root and a browser deep link load successfully.
2. Signup and login work.
3. One authenticated Experience or Project can be created and read back.
4. A JobPosting can be saved.
5. Ollama embedding indexing succeeds for the authenticated user.
6. One grounded AI flow succeeds with the configured provider.
7. A generated ResumeVersion remains immutable after a later version is created.

Do not log or paste access tokens, passwords, resume contents, or provider keys while diagnosing a
failed smoke test.

## 7. Backup before releases

Create a PostgreSQL dump outside the live Compose volume before migrations or application changes:

```bash
mkdir -p backups
backup="backups/ai-career-$(date -u +%Y%m%d-%H%M%S).sql"
docker compose --env-file .env.oracle -f docker-compose.yml -f docker-compose.oracle.yml \
  exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --no-owner --no-privileges' \
  > "$backup"
test -s "$backup"
```

Copy the backup to a separate storage location and test restoration into a disposable PostgreSQL
instance. Keep at least one backup outside the VM so a VM or volume failure does not destroy the only
copy.

## 8. Update and rollback

Before an update, record the current commit, back up PostgreSQL, and render the merged Compose config.
Then pull the reviewed commit and rebuild:

```bash
git rev-parse HEAD
docker compose --env-file .env.oracle -f docker-compose.yml -f docker-compose.oracle.yml \
  up -d --build
```

Rollback the application to the previous reviewed commit if the schema remains backward-compatible.
Do not automatically reverse Flyway migrations. A database restore is an explicit data-loss operation
for writes made after the backup.

## 9. Cost and security checks

- Confirm every OCI compute, volume, and network resource is labeled Always Free eligible.
- Set a billing budget or usage alert if OCI requires a billing account for the tenancy.
- Never expose 5432, 8080, 5173, or 11434 publicly.
- Rotate the JWT and provider secrets if the VM, shell history, or environment file is exposed.
- Update the host OS, Docker images, and application dependencies regularly.
- Recheck disk usage because PostgreSQL data, Docker layers, Ollama models, and backups share the VM
  storage budget.
- Treat Gemini as an external data processor when resume or job data is sent to it; use Ollama-only
  paths when the data policy requires local inference and the relevant OCR functionality is not needed.
