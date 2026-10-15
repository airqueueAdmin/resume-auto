# ADR 0006: JobKorea Integration Boundary

## Status

Accepted and implemented in Phase 10.

## Context

The requested JobKorea workflow starts at an authenticated upload page. The public page does not expose
the upload contract without a login. JobKorea's published recruiting-information API terms describe
approved media-partner access to recruiting information, not personal-resume creation.

The observed upload screen accepts one PDF, PNG, JPG, or JPEG file up to 5 MB and warns that generative
AI may produce inaccurate content from the uploaded file. It also asks users to avoid unnecessary
personal information.

Automating a private web form by collecting a user's password, replaying cookies, reverse-engineering
private endpoints, or bypassing anti-automation controls would create security, privacy, and service-term
risk. It would also be brittle: a UI change could silently publish the wrong resume.

## Decision

- Treat the platform's approved `ResumeVersion` as the source of truth.
- Build the first JobKorea integration as a user-controlled export and handoff flow.
- Enforce the observed upload constraints locally: exactly one file, at most 5 MB, and PDF/PNG/JPG/JPEG
  only; treat the limit as a versioned external contract subject to re-verification.
- Open the official upload page for the user to authenticate and upload; do not proxy credentials or
  session cookies through this application.
- Do not implement direct API creation or server-side upload in Phase 10. Reconsider it only in a
  separate phase after a documented JobKorea personal-resume API or partnership contract is obtained
  and its authentication, payload, rate, and deletion rules are verified.
- Require explicit user confirmation before file generation/download and maintain a redacted audit record
  of the selected version and export result.

## Consequences

Positive:

- User credentials and JobKorea sessions stay within JobKorea's login boundary.
- The integration remains compliant with the verified external contract instead of depending on private
  endpoints.
- Export remains useful even if JobKorea never offers a third-party personal-resume API.

Trade-offs:

- The initial flow requires a user to complete the final upload step.
- The observed file format/size contract and field mappings may drift and must remain versioned; an
  authenticated test account cannot establish an API contract.
- Users must review AI-generated content and remove unnecessary personal information before upload.
- A future official API adapter, if authorized, will require provider-specific consent, idempotency, and
  outage handling; it is explicitly outside Phase 10.

## Rejected alternatives

- Storing JobKorea credentials was rejected because it increases account-takeover and personal-data risk.
- Browser automation against private endpoints was rejected because it is brittle and may violate service
  terms or anti-automation controls.
- Treating the recruiting-information API as a resume-creation API was rejected because the published
  contract does not support that inference.
