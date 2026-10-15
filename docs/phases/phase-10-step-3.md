# Phase 10 Step 3 - User-Controlled File Export MVP

## Status

Complete for the user-controlled file export slice. Phase 10 is complete at the supported public
integration boundary; see [phase-10.md](phase-10.md).

## Objective

Start from a saved JobPosting, generate a grounded draft, let the user approve it into an immutable
`ResumeVersion`, generate one JobKorea-compatible file, and keep the final upload inside the user's
JobKorea session.

## Design reason

The current backend stores `ResumeVersion` as an immutable plain-text snapshot and does not yet have a
portable Korean font asset or a PDF rendering boundary. A browser-side PDF renderer uses the user's
available Korean font stack, avoids server-side document storage, and matches the currently observed
JobKorea upload contract without sending the file to JobKorea.

## Implemented

- Added a `JobKorea 파일 생성` action to the selected immutable ResumeVersion in the Resumes screen.
- Added an `이 공고로 이력서 생성` action to every saved JobPosting.
- Added a Career-screen `경력 데이터 인덱싱` action for rebuilding the authenticated user's
  Experience/Project retrieval index after career data changes.
- Connected the saved JobPosting action to the existing grounded resume-draft API.
- Added explicit draft review with citation IDs before approval.
- Added automatic Resume container and Version 1 creation for the first approved draft.
- Opened the generated Version 1 directly in the JobKorea export modal after approval.
- Added a review modal with a versioned, JobKorea-compatible single-page layout.
- Added editable required-profile fields for name, contact, email, address, birth date, gender, desired
  work conditions, education, and certificates.
- Expanded the export form to reflect the observed JobKorea sections: personal information and photo,
  desired-job keywords, up to 20 skills, repeatable education and career records, career description,
  military information, repeatable self-introduction sections, industry keywords, optional activities,
  training, certificates, awards, overseas experience, languages, portfolios, and desired work conditions
  with up to three locations.
- Prefilled only account-confirmed name and email. Removed synthetic birth date, phone, address, salary,
  and education defaults so unverified personal data cannot be exported accidentally.
- Added export-time validation for JobKorea-required personal fields, desired-job keywords, desired
  locations, skill count, and required fields within each entered education or career record.
- Added an explicit review confirmation before PDF download and kept all export edits local to the browser;
  the immutable ResumeVersion is not modified.
- Set Gemini as the default provider for the explicit JobPosting-to-resume-draft action through
  `AI_RESUME_DRAFT_PROVIDER=gemini`; the global `AI_PROVIDER` remains available for job analysis and
  matching, with a bounded fallback for resume drafts.
- Changed approved draft formatting to the JobKorea field vocabulary: `희망직무`, `자기소개서`,
  `경력기술서`, and `스킬`. Experience and project evidence are combined under `경력기술서` so the
  JobKorea exporter does not expose the earlier generic `Headline / Summary / Experience / Projects`
  layout.
- Added JobKorea career controls for importing all Career Experience records, company-name privacy,
  selectable/direct-entry job categories, and separate overseas-work records. Career fields that do
  not exist in the source data remain explicit user inputs.
- Updated the default Gemini resume prompt to synthesize all indexed Career evidence into a tailored
  3–5 sentence JobKorea 자기소개서 body for the selected JobPosting. The generated text remains
  citation-grounded and is reviewable before export.
- Updated the export preview to the `JOBKOREA_STANDARD_V2` section order and labels instead of the
  earlier generic resume layout.
- Parsed the current JobKorea labels and retained backward compatibility for older generic snapshot
  labels.
- Removed internal citation labels from the user-facing export while retaining them in the source
  `ResumeVersion`.
- Generated a browser-side A4 PDF download with a deterministic filename.
- Enforced the observed 5 MB maximum before download.
- Added an explicit action that opens the JobKorea upload page in a new tab.
- Kept JobKorea credentials, cookies, upload sessions, and remote API calls outside the application.

## Verification

- `frontend/npm run build`: passed (`tsc` and Vite production build).
- Backend `ResumeDraftApprovalServiceTest`: 4 tests passed.
- Backend `AiCareerPlatformApplicationTests`: 31 tests passed, including auto-created Resume ownership
  isolation.
- Full backend `mvnw test`: 91 tests passed with PostgreSQL/Testcontainers integration coverage.
- Official public evidence confirms that final resume registration remains a user action on JobKorea and
  that the published API does not provide a personal-resume write contract.

## Known limitations

- PDF is the selected renderer; PNG/JPG/JPEG downloads are intentionally not exposed.
- The current `ResumeVersion` content is plain text, so unsupported profile fields remain empty until the
  user enters them rather than being inferred.
- Structured export profile edits are currently not persisted as a reusable profile; closing the modal
  discards them by design.
- The browser must support Canvas rendering. The export uses `html2canvas` with tainted-canvas
  protection and accepts only local JPG, PNG, or WEBP profile photos.
- The application does not observe or confirm the final JobKorea upload result.

## Next step

Phase 10 continues with privacy-safe export auditing and bounded retention, then closes at the
user-controlled handoff boundary. Direct upload requires a future written JobKorea partner contract.
