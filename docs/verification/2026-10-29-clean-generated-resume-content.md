# Clean generated resume content — 2026-10-29

This records an AI-output presentation correction. It is not a phase completion record.

## Objective

Keep citation identifiers such as `[C1]` and `[c2]` out of user-facing generated resume and
self-introduction content while preserving grounding validation and source provenance.

## Implemented

- Resume draft approval still requires structured citations for every generated item, but no longer
  writes citation identifiers into `ResumeVersion.content`.
- Self-introduction generation returns clean prose while retaining its structured `citations` list.
- Provider-supplied inline markers are removed case-insensitively before generated prose is returned.
- The frontend removes legacy inline citation markers from existing resume content and saved company
  answers, including markers embedded before punctuation or written in lowercase.
- Career evidence remains available in the review UI without exposing internal citation identifiers in
  the editable answer.

## Verification

- `frontend`: `npm run build` passed with TypeScript checking and Vite production compilation.
- Backend focused tests: `ResumeDraftApprovalServiceTest`, `SelfIntroductionAnswerServiceTest`, and
  `AiControllerTest` passed.
- Existing Vite module-directive, bundle-size, JDK native-access, and Mockito agent warnings remain;
  they are unrelated to this change.

## Provenance note

Removing citation labels from presentation text does not remove provenance. Resume drafts and generated
answers retain validated structured source citations separately from the user-facing prose.
