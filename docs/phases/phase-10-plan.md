# Phase 10 - JobKorea Resume Integration Plan

## Status

Implemented. See the immutable [Phase 10 completion record](phase-10.md). This file remains the original
implementation plan and boundary description.

## Objective

Use an approved, immutable `ResumeVersion` from this platform to generate a JobKorea-compatible resume
file that the user can upload through the JobKorea resume workflow at
`https://resume.jobkorea.co.kr/resume/upload`.

The Phase 10 outcome is file generation and user-controlled handoff: generate one verified file, let the
user download and review it, and optionally open the official upload page. The platform does not upload,
publish, or create a JobKorea resume on the user's behalf.

The internal generation target is the versioned `JOBKOREA_STANDARD_V1` template defined by
[ADR 0008](../adr/0008-jobkorea-compatible-auto-resume.md). This is a canonical, structured mapping
target, not a copy of JobKorea's private UI. The first approved draft may create the Resume container
automatically; the user should not need to create an empty Resume first.

## Evidence and current boundary

- The referenced upload URL redirects unauthenticated visitors to a login-required page, so the upload
  contract cannot be verified from a public session.
- The user-provided upload screen confirms that JobKorea accepts exactly one file, up to 5 MB, with
  `PDF`, `PNG`, `JPG`, or `JPEG` formats.
- The upload screen warns users to remove unnecessary personal information and states that generative AI
  creates the resume from the uploaded file and may produce inaccurate content that must be reviewed.
- JobKorea's published terms describe resume registration and user-controlled resume disclosure, but do
  not establish a public personal-resume creation API.
- The publicly documented JobKorea API page describes a `채용정보 API` for approved media partners to
  display recruiting information; its application asks for an institutional operator, business details,
  site information, and API-use approval. It is not evidence of an API for creating a personal resume
  available to an individual member.
- Therefore, as of 2026-10-13, the platform treats direct personal-resume API access as unavailable to
  an individual developer unless JobKorea provides a separate written personal-resume/partner contract.

Sources checked during planning:

- [JobKorea resume upload page](https://resume.jobkorea.co.kr/resume/upload)
- [JobKorea individual-member terms](https://m.jobkorea.co.kr/Service_JK/Provision_LOC.asp)
- [JobKorea recruiting-information API terms](https://www.jobkorea.co.kr/network/Popup_Xml.asp)
- User-provided authenticated upload-screen text confirming the one-file/5 MB/format constraints and
  generative-AI review warning.

## Planned steps

### Step 1 - Upload contract and authenticated-flow discovery

- The currently observed file contract is one `PDF`/`PNG`/`JPG`/`JPEG` file with a maximum size of 5 MB;
  keep this as a versioned, changeable UI contract rather than an API promise.
- Use official public API terms and individual-member documentation to determine the supported boundary.
  A test account may inspect UX behavior but cannot establish a supported write-API contract.
- Do not infer private endpoints from browser traffic as a production integration contract.

### Step 2 - Resume data mapping

- Define the canonical `JOBKOREA_STANDARD_V1` sections and map approved ResumeVersion data to the verified
  JobKorea schema only after authenticated-flow discovery.
- Mark fields that require user input or cannot be inferred from career evidence.
- Preserve source citations and approval metadata internally; do not upload hidden provenance markers
  into user-visible JobKorea fields unless the user explicitly chooses to.
- Keep the internal canonical schema separate from the provider-specific export adapter so a JobKorea UI
  change does not rewrite the career domain.

### Step 3 - User-controlled file export and handoff MVP

- Start from a saved JobPosting action: generate a grounded draft using that posting and the user's
  retrieved Experience/Project evidence.
- Require the user to review the draft and explicitly approve it before creating a Resume. When no
  existing Resume is selected, create the Resume container and immutable Version 1 automatically using a
  job-targeted name.
- Generate one verified PDF upload file from an approved ResumeVersion. The browser renderer rasterizes
  the reviewed Korean layout into A4 PDF pages so Korean fonts do not need to be installed on the backend.
- Validate the one-file limit, supported extension/MIME type, 5 MB maximum, readability, and required
  content before making the file available for download.
- Provide a user-visible preview and an explicit “open JobKorea upload page” action.
- Never upload to JobKorea, proxy JobKorea credentials, or store login cookies in this platform.

### Step 4 - Privacy, audit, and operational controls

- Minimize the personal data included in exported files.
- Record the selected ResumeVersion, template version, export timestamp, and download result without
  storing JobKorea credentials or session artifacts.
- Add deletion/revocation handling, redacted logs, rate limits, and temporary-file cleanup.
- Add contract tests using fixtures and a local renderer; do not use a production personal account in
  automated tests.

Implemented baseline:

- The browser reports only the owned `ResumeVersion` ID, `JOBKOREA_STANDARD_V2` template version,
  server timestamp, generated file size, and `DOWNLOAD_TRIGGERED`/`GENERATION_FAILED` result.
- PDF bytes, rendered content, personal fields, photos, filenames, JobKorea credentials, cookies, and
  sessions are not sent to or stored by the backend.
- Export-audit writes are authenticated, ownership-scoped, validated against the 5 MB contract, and
  covered by the existing local rate limiter.
- Export-audit metadata is automatically deleted after a configurable 30-day default retention period;
  the cleanup schedule and enablement are environment-configurable, and the test profile disables it.
- Temporary browser object URLs are revoked after rendering/download. No server temporary file exists.
- Authenticated JobKorea end-to-end verification and downstream field mapping remain open.

## Explicit non-goals

- Collecting or replaying JobKorea passwords, refresh tokens, cookies, or CAPTCHA answers
- Headless login automation, private API reverse engineering, or anti-bot bypass
- Uploading or publishing without an explicit user action and confirmation
- Assuming the recruiting-information API permits personal-resume creation
- Implementing a JobKorea API adapter or server-side upload in Phase 10
- Treating a test account or observed private browser endpoint as an official write-API contract

## Completion criteria

- [x] The user-controlled file export/handoff MVP is verified through local rendering, validation,
      download initiation, and explicit official-page handoff.
- [x] The observed one-file, 5 MB, PDF/PNG/JPG/JPEG contract and field mappings are captured in a versioned
      export contract.
- [x] Approved ResumeVersion data is mapped without inventing unsupported user facts.
- [x] User preview, explicit confirmation, and failure recovery are implemented.
- [x] The UI warns about unnecessary personal information and requires review of AI-generated content
      before the user completes the JobKorea upload.
- [x] Credentials, upload sessions, and JobKorea remote-operation artifacts never enter application
      storage or logs.
- [x] Privacy, ownership, audit, and integration tests pass.
- [x] Official public evidence confirms that the available API is for recruitment information and does
      not provide an individual personal-resume write contract.

## Dependencies and risks

- The upload UI and observed file contract may change without notice.
- The current individual-access boundary means file export/handoff is the supported product boundary.
- A future direct API integration, if ever authorized, requires a separate phase, contract, ADR, and
  explicit user-consent model; it is not an extension of this file-generation MVP.
- Resume data contains sensitive personal information and must be treated as user-owned data during
  export, transport, temporary storage, and deletion.
