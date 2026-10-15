# Phase 10 Step 4 - Privacy-Safe Export Audit Baseline

## Status

Complete for the export-audit baseline. Phase 10 remains in progress because authenticated JobKorea
upload verification and downstream field mapping are still open.

## Objective

Record enough metadata to diagnose the browser-side export flow without moving resume content,
personal profile data, files, or JobKorea session artifacts into backend storage.

## Design reasons and important decisions

- The browser remains the renderer and file owner. The backend never receives PDF bytes or temporary
  image data.
- An audit row is linked to both the authenticated user and an owned immutable `ResumeVersion`.
  Cross-user and soft-deleted Resume access returns the existing not-found boundary.
- The result names what the application can prove: `DOWNLOAD_TRIGGERED` means the browser download was
  initiated, not that JobKorea accepted or published the file.
- `JOBKOREA_STANDARD_V2`, result, file size, and server timestamp are allowlisted and constrained in
  both request validation and PostgreSQL.
- Failure events contain no raw browser exception, rendered content, filename, profile fields, or other
  personal data.
- Export-audit calls use the existing single-instance rate limiter. No Redis or queue dependency was
  added without measured need.
- Export-audit metadata is retained for 30 days by default and deleted by a UTC scheduled cleanup;
  the retention duration, schedule, and cleanup enablement are configurable.

## Implemented

- PostgreSQL V11 `jobkorea_export_audits` migration with user and ResumeVersion foreign keys
- Append-only audit entity and repository
- Authenticated `POST /api/v1/jobkorea/export-audits`
- Server-side ResumeVersion ownership validation
- Success file-size validation against the observed 5 MB maximum
- Browser reporting for download-triggered and generation-failed outcomes
- Direct DOM capture through `html2canvas` with `allowTaint=false` instead of SVG `foreignObject`
  rasterization
- Profile-photo input restricted to local JPG, PNG, or WEBP data URLs
- Immediate revocation of browser object URLs; no server temporary files
- `필수 데모 데이터 채우기` action for local flow testing
- Demo values fill only currently empty birth date, gender, mobile, desired location, and first education
  fields, plus missing start/end months for currently loaded Career entries; they remain browser-local and
  reset review confirmation
- A separate `경력 데모 기간 채우기` action supports Career entries loaded after the general demo-data
  action and never overwrites an existing period
- Configurable 30-day export-audit retention cleanup with scheduling disabled in the test profile
- Visible warning that demo values must be replaced before a real upload

## Explicitly excluded

- PDF/profile/photo/filename storage
- JobKorea credentials, cookies, sessions, CAPTCHA data, or remote upload results
- Claiming that `DOWNLOAD_TRIGGERED` proves a completed local save or JobKorea upload
- Automatic Gemini API-key creation or rotation
- Phase 10 completion before authenticated test-account verification

## Verification

- Backend compile/package passed.
- Frontend TypeScript and Vite production build passed.
- Full backend test suite passed: 98 tests, 0 failures, 0 errors, and 0 skipped, using PostgreSQL
  17/Testcontainers and all 11 Flyway migrations.
- Integration coverage verifies unauthenticated rejection, cross-user isolation, request validation,
  successful download/failure audit events, OpenAPI exposure, and migration creation.
- Retention unit coverage verifies UTC-clock cutoff calculation and repository deletion delegation.
- The frontend build verifies the explicit demo-data helper and audit request types.

## Failures encountered and root causes

- The initial renderer serialized the preview into an SVG `foreignObject`, drew that SVG onto a Canvas,
  and then called `toDataURL()`. Browsers can mark that Canvas as tainted even when the SVG uses a local
  object URL, so PDF generation failed with a `SecurityError`. The renderer now captures the DOM directly
  with `html2canvas`, disables tainted-image drawing, enables CORS-safe image loading, and restricts photo
  uploads to local raster formats.
- Existing Vite warnings concern bundle size and third-party `use client` directives; they do not fail
  the production build.

## Concepts to understand

- Audit metadata should state only what the system can observe; a browser download trigger is not an
  external upload receipt.
- Data minimization is stronger when sensitive bytes never cross the backend boundary.
- Ownership validation is required even for metadata-only records.
- Test-only dummy values must require an explicit user action and must not silently become user facts.

## Completion criteria for this step

- [x] Export results are linked to an authenticated user's owned immutable ResumeVersion.
- [x] Stored data excludes PDF content and personal/export profile fields.
- [x] File-size and enum constraints are enforced in the API and database.
- [x] Temporary files are not created server-side and browser object URLs are revoked.
- [x] Required-field demo data is explicit, browser-local, and visibly marked for replacement.
- [x] Export-audit retention is bounded by a configurable 30-day default and scheduled deletion.
- [x] Backend and frontend executable checks pass.

## Next step and known limitations

Phase 10 closes at the user-controlled file handoff boundary because official public evidence does not
provide an individual personal-resume write API. Any future server-side adapter requires a written
partner contract, a new ADR, and separate authorization and consent design.
