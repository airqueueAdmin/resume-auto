# First-use entry and guided workspace tour — 2026-10-28

This records a frontend onboarding improvement. It is not a phase completion record.

## Objective

Help a first-time user understand what Career Note does before authentication and show the first
actions to take after login without changing backend contracts or user-owned data behavior.

## Implemented

- Added a public `/` landing page for unauthenticated users with a concise value proposition,
  four-step workflow, privacy/review reminder, and links to signup, login, and the privacy notice.
- Kept authenticated `/` as the workspace dashboard.
- Added a first-login guided tour that highlights the live dashboard action and sidebar menu items in
  this order: career entry, career data, job postings, and resume management.
- Stored tour completion per user in `localStorage` so the tour is shown once per account on the
  current browser, while still providing `사용 안내` entry points to replay it.
- Added keyboard Escape dismissal, skip/close controls, progress indication, responsive layout, and
  fallback positioning when a target is unavailable.

## Explicitly excluded

- No backend endpoint, database migration, authentication behavior, or user data model change.
- No automatic navigation or data creation from the tour; the user decides when to follow each menu.
- No third-party onboarding library dependency.

## Verification

- `frontend`: `npm run build` passed with TypeScript checking and Vite production compilation.
- Existing Vite module-directive and bundle-size warnings remain; they are unrelated to this change.

## Known limitation

Tour completion is browser-local. A user who changes browser/device will see the first-use tour again.
A future server-backed onboarding preference should only be added if cross-device continuity becomes a
measured product requirement.
