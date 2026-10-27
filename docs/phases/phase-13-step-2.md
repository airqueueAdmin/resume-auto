# Phase 13 Step 2 - Service Authentication and Answer Context

## Status

Complete for the authentication and read-only context contract. Phase 13 remains in progress.

## Objective and scope

Connect the JobKorea-first extension to the existing service without copying refresh tokens or creating a
second resume-management system inside the browser. The extension receives a short-lived access token,
selects an existing immutable ResumeVersion, and requests grounded career evidence with provenance.

Answer generation, automatic field insertion, site-specific selectors, login automation, upload, and final
submission remain out of scope for this step.

## Implemented

- Added a one-time authorization code table and Flyway migration.
- Added the authenticated web-service endpoint `/extension/connect` for issuing a two-minute code.
- Added a public code-exchange endpoint that returns only the standard 15-minute access token; refresh
  tokens are never returned to the extension.
- Added pessimistic locking and consumed-at tracking so an authorization code cannot be replayed.
- Added extension endpoints for paginated ResumeVersion choices, selected immutable version context, and
  question-based vector evidence with source metadata.
- Added CORS only for the extension API path and `chrome-extension://*` origins; application endpoints
  remain authenticated and user-scoped.
- Updated the popup to connect, select a ResumeVersion, request evidence, and inspect visible JobKorea
  fields. It keeps the access token in `chrome.storage.session` only.

## Verification

- `AiCareerPlatformApplicationTests#extensionAuthorizationIsOneTimeAndResumeVersionAccessIsScoped`
  passed with PostgreSQL Testcontainers.
- Flyway validated and applied 20 migrations in the integration test.
- Frontend and extension syntax/build checks remain required before container rebuild.

## Security and ownership checks

- Code issuance requires the authenticated web-service session.
- Code exchange is public only because possession of the short-lived one-time code is required.
- ResumeVersion list, selected context, and evidence search use the authenticated user ID and return
  `404` for another user's version.
- The extension does not receive passwords, cookies, refresh tokens, CAPTCHA data, or backend secrets.

## Known limitations

- The extension defaults to the deployed Render frontend URL; local Docker users can replace it with
  `http://localhost:5173` in the service address field.
- Evidence retrieval requires the user's career embeddings to be indexed and does not yet generate an
  answer for each detected field.
- Field detection remains generic; JobKorea semantic adapters and rich-text handling are later work.

## Next step

Implement a review-only answer proposal surface for detected fields. Require explicit per-field approval,
respect required/max-length constraints, and keep final submission manual on JobKorea.
