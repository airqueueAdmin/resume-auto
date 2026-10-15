# Phase 10 - JobKorea User-Controlled Resume Handoff

## Status

Complete for the supported public integration boundary. Direct JobKorea resume creation remains
unavailable without a separate written partner contract.

## Objective and scope

Convert an approved immutable `ResumeVersion` into a reviewed JobKorea-compatible PDF and hand control
back to the user for upload through JobKorea. The platform must not collect JobKorea credentials,
replay sessions, bypass anti-automation controls, or claim that a browser download proves a successful
JobKorea upload.

## Design reasons and important decisions

- JobKorea's public API documentation describes recruitment-information retrieval, not personal-resume
  creation. Its published terms exclude project/personal use and require users to visit JobKorea for
  applications and resume writing.
- JobKorea's individual-member FAQ documents user-driven resume registration and file attachment. It
  does not document a personal-resume write API.
- A test account cannot establish a supported API contract. Browser traffic, private endpoints, browser
  extensions, cookie replay, or headless login would be fragile and outside the approved security and
  terms boundary.
- The stable integration is therefore file generation plus explicit user-controlled upload. Provider UI
  changes remain external and are not treated as a server API contract.

Official evidence reviewed on 2026-10-14:

- https://www.jobkorea.co.kr/service/api
- https://www.jobkorea.co.kr/network/Popup_Xml.asp
- https://www.jobkorea.co.kr/help/faq/user?Page=1&searchWord=%EC%9D%B4%EB%A0%A5%EC%84%9C

## Implemented features

- Grounded JobPosting-targeted resume generation and explicit approval into immutable ResumeVersion
- JobKorea field-oriented browser editor and A4 PDF preview
- Repeatable 자기소개서 title/content sections with three generated defaults and user-controlled N items
- Required-field validation, explicit demo-data helpers, and AI/personal-data review confirmation
- Browser-side PDF generation with one-file and 5 MB validation
- Explicit link to the official JobKorea upload page; no remote upload or publication
- Privacy-safe export audit metadata with ownership validation and no resume/file/profile bytes
- Configurable 30-day export-audit retention cleanup
- CORS-safe DOM capture and raster-only local profile-photo handling

## Explicitly excluded features

- Direct personal-resume API adapter without a written JobKorea partner contract
- JobKorea passwords, cookies, refresh tokens, CAPTCHA answers, or session storage
- Private endpoint discovery, browser automation, or anti-bot bypass
- Claiming that `DOWNLOAD_TRIGGERED` proves local save, upload, parsing, or publication
- Hidden transmission of source citations or unsupported candidate facts

## Tests and executable verification

- Full backend suite passes: 98 tests, 0 failures, 0 errors, and 0 skipped.
- PostgreSQL/Testcontainers verifies ownership, validation, Flyway migrations, export-audit writes, and
  the existing AI/tool/workflow boundaries.
- Frontend TypeScript and Vite production build pass.
- Browser export failures caused by tainted Canvas rendering were corrected with direct DOM capture.
- Export-audit retention cutoff calculation has focused unit coverage.

## Failures encountered and root causes

- Gemini resume generation initially failed because a free API quota returned 503 and because reasoning
  plus structured output exceeded the previous token budget. The provider key was replaced manually;
  bounded fallback remains, while automatic key rotation is intentionally deferred.
- SVG `foreignObject` rendering tainted the Canvas before `toDataURL()`. Direct `html2canvas` capture and
  safe raster inputs removed that path.
- Generated self-introduction sections were collapsed into one summary during export parsing. Numbered
  title/content markers now preserve every repeatable section.

## Concepts the learner must understand

- A public read API does not imply a write API or permission to automate an authenticated website.
- A test account can validate UX behavior but cannot create an integration contract.
- User-controlled file handoff is a valid integration architecture when the provider has no supported
  write API.
- Audit events must describe only observable facts and should be retained for a bounded period.

## Completion criteria

- [x] Approved immutable resume data maps to a reviewable JobKorea-compatible file.
- [x] Required fields, file size, preview, confirmation, and failure recovery are implemented.
- [x] Credentials and JobKorea session artifacts never enter application storage or logs.
- [x] Privacy-safe ownership-scoped audit records and bounded retention are implemented.
- [x] The absence of a supported personal-resume API is documented from official public evidence.
- [x] Direct upload and browser automation remain outside the product boundary.
- [x] Backend and frontend executable checks pass.

## Next phase and known limitations

Phase 11 packages the existing system as a reproducible deployable unit and adds operational delivery
checks. JobKorea may change its upload UI or later publish a partner write API; either change requires a
new contract review and ADR before altering this boundary.
