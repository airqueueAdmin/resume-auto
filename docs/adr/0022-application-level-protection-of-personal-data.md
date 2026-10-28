# ADR 0022: Application-level protection of personal data

## Status

Accepted

## Context

Career Note stores user-owned identity data and career documents that can contain personal information. Database access alone must not expose readable emails, names, career history, resume content, application answers, or AI-derived copies of that content.

Authentication secrets have different requirements: passwords, refresh tokens, email verification codes, extension authorization codes, and tool approval tokens are only verified and therefore remain one-way hashed.

## Decision

- Encrypt user-entered text, career periods, and employment status at rest with AES-256-GCM through JPA attribute converters.
- Encrypt identity, career, resume, job-posting, application-question, AI-result, and embedding source-text columns.
- Store the normalized email as an encrypted value and use an HMAC-SHA-256 blind index for login, duplicate detection, and email verification lookups.
- Keep plaintext available only inside the authorized application flow so the owner can review and use their documents.
- Convert JSONB AI artifact columns to encrypted text; JSON is parsed after decryption in the application.
- Hash client IP addresses before using them as in-memory rate-limit keys.
- Require `DATA_ENCRYPTION_KEY_BASE64` to be supplied in deployed environments. The checked-in key is only a local development fallback and must be replaced before deployment.
- Run a Flyway migration that changes protected columns to text, encrypts existing rows, creates the email blind index, and removes the plaintext email uniqueness constraint.

## Consequences

Positive:

- A database dump no longer contains directly readable user-entered personal data.
- Email lookup remains functional without storing a searchable plaintext email.
- Existing AI, retrieval, resume, and export flows continue to operate after application-level decryption.

Trade-offs:

- Protected text cannot be searched or indexed directly in SQL; user scoping and application retrieval remain mandatory.
- Date-range checks that compared plaintext SQL dates are moved to the domain validation layer because encrypted dates cannot be meaningfully compared in PostgreSQL.
- Experience career-period ordering is performed after decrypting only the authenticated user's rows in the application layer, then paginated in memory. This preserves the existing newest-start-date API contract without adding a plaintext date sort column.
- The encryption key is critical operational data. Losing it makes protected data unrecoverable; rotating it requires a planned re-encryption migration.
- Vector similarity search still uses the stored vector representation. The source text and ownership links are protected, but vector-at-rest encryption would require a different retrieval architecture.
- The owner-facing API returns plaintext only after authentication because the user must be able to review and edit their own documents.
