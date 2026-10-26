# Phase 13 Step 4 - JobKorea Semantic Adapter Baseline

## Status

In progress. This record captures the first measured static baseline; live-site coverage is not yet
complete.

## Objective and scope

Improve the JobKorea-first field contract without relying on private APIs or brittle CSS selectors. Use
visible labels and common accessibility metadata to classify fields, expose the assistant in a controlled
bottom-right page panel, and verify the supported fill boundary with representative static fixtures.

## Implemented baseline

- Added semantic roles for common application fields: `NAME`, `EMAIL`, `PHONE`, `PORTFOLIO`,
  `CAREER_DESCRIPTION`, `SELF_INTRODUCTION`, and `UNKNOWN`.
- Added a JobKorea representative static fixture covering profile, contact, self-introduction, career,
  portfolio, select, and submit controls.
- Kept field classification based on visible labels, `name`, `id`, and `autocomplete` hints.
- Kept approved fill limited to visible text inputs, textareas, and contenteditable controls.
- Documented the normalized role contract and known ambiguity in `extension/field-contract.md`.
- Added a Shadow DOM-isolated Career Note panel that is injected only on JobKorea pages and keeps final
  submission manual.
- Styled the launcher as a ChannelTalk-like circular bottom-right button with a separate expandable panel.
- Opened the service connection page directly from the user click, avoiding a silent no-op when an
  asynchronous extension message is treated as a popup-blocked action.
- Added a background-service-worker bridge for scoped service API calls and one-time connection flow.
- Added silent web-session restoration for the service's `/extension/connect` route so opening the route in
  a new tab does not lose the existing login session when the refresh session is still valid.
- Allowed the configured service web origin on extension API CORS in addition to Chrome extension origins;
  this prevents the reverse-proxied `/extension/connect` page from receiving `Invalid CORS request` (403).
- Replaced saved ResumeVersion and JobPosting selection with extraction of the currently open JobKorea
  posting. The extracted company, title, description, and URL are request-scoped and are not persisted.
- Added an extension readiness endpoint that reports active Experience, Project, and indexed-evidence counts.
  The panel blocks generation and links to Career management until source data and retrieval evidence exist.
- Added a current-page answer endpoint that retrieves only the connected user's active career evidence and
  returns review-required output with citations and an explicit `persisted: false` marker.
- Reused the grounded resume-draft validation pipeline for a current unsaved JobKorea posting through a
  review-only page-draft endpoint. It returns the structured headline, summary, self-introduction topics,
  career bullets, project bullets, skills, citations, and provider metadata without creating a JobPosting,
  ResumeDraft, Resume, or ResumeVersion.
- Added an explicit page-draft approval endpoint. After the user reviews the generated draft, the extension
  can create one new Resume with its first immutable ResumeVersion and open the service Resume Workspace's
  JobKorea export flow. The current JobPosting is still not saved.
- Removed generated-content copy actions. The service export flow remains responsible for profile review,
  PDF generation, and opening the JobKorea upload page; file selection and final submission remain manual.
- Added posting read verification in the page panel: selected extraction source, structured/page character
  counts, captured character count, and an expandable full-text preview are visible before generation.
- Removed the separate toolbar popup; the toolbar action now toggles the same in-page panel.
- Normalized JSON Schema before Gemini requests by removing unsupported `additionalProperties` keywords,
  resolving the provider's `400 INVALID_ARGUMENT` rejection without weakening server-side response validation.

## Quality boundary

The fixture is a local contract test, not evidence that JobKorea's private DOM or dynamic flow is stable.
Before production distribution, representative JobKorea pages must be reviewed manually and tested against
field extraction, role mapping, character limits, re-render recovery, and failed-fill behavior. No automatic
submission, login, file upload, CAPTCHA handling, or anti-automation bypass is allowed.

The service connection route still requires a logged-in web-service user. The extension receives only the
short-lived access token returned by the one-time code exchange; refresh tokens, cookies, and JobKorea
credentials remain outside the extension.

The current posting extraction is intentionally non-persistent. Reloading or navigating the JobKorea tab
replaces that context, while Experience/Project records and their indexed evidence remain service-owned.

## Next step

Run the fixture and a manually reviewed JobKorea page through the same scan flow, record role-mapping and
fill failures, then add focused regression fixtures before publishing the extension.
