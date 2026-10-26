# Phase 13 Step 3 - Review and User-Approved Fill

## Status

Complete for the review-only answer proposal and supported text controls. Phase 13 remains in progress.

## Objective and scope

Use the JobKorea-first field detector to request a grounded answer proposal for a selected ResumeVersion
and saved job posting. The user must be able to edit and review the result before an explicit action
inserts it into one selected visible text field. The extension never submits the application.

## Implemented

- Added `/api/v1/extension/application-answer`, which validates the selected ResumeVersion ownership,
  uses the existing grounded self-introduction generation path, and returns citations/provenance.
- Kept generated answers ephemeral; the extension API does not create or mutate a ResumeVersion or saved
  application question.
- Added max-length-aware `suggestedValue`, truncation metadata, and `requiresReview` response fields.
- Added JobKorea popup controls for selecting a saved job posting, selecting a ResumeVersion, editing the
  proposed answer, and explicitly filling the currently selected field.
- The fill operation supports visible text inputs, textareas, and contenteditable controls only. It rejects
  file, checkbox, radio, select, hidden, and submit controls.
- Final submission remains a manual action on the JobKorea page.

## Verification

- `SelfIntroductionAnswerApiTests#extensionAnswerProposalReturnsReviewMetadataAndLengthBoundedSuggestion`
  passed with PostgreSQL Testcontainers and the test AI provider.
- Cross-user ResumeVersion access to the extension answer endpoint is covered by the integration test.
- `node --check extension/popup.js` passed.
- `npm run build` passed.

## Known limitations

- Field identity is based on the latest visible-field scan index and can become stale if a dynamic page
  rerenders; the user should rescan before approving a fill.
- Semantic JobKorea adapters for rich editors, custom dropdowns, and multi-step forms are not complete.
- Answer generation requires a saved JobPosting, indexed career evidence, and an available AI provider.

## Next step

Measure representative JobKorea form variants and add semantic field adapters plus extraction/fill failure
tests without adding broad host permissions or automatic submission.
