# Role selection onboarding handoff — 2026-10-29

This records a frontend onboarding correction. It is not a phase completion record.

## Objective

Treat the initial career-role selection as the end of automatic onboarding so the workspace opens
without an additional step-by-step guide or another role-selection screen.

## Implemented

- Removed automatic guided-tour opening from the authenticated dashboard. The guide remains available
  as an explicit action in My Page.
- Redirected users who already have a selected role away from `/choose-role`.
- Kept role changes available only through the explicit `직군 변경` action in My Page.

## Verification

- `frontend`: `npm run build` passed with TypeScript checking and Vite production compilation.
- Existing Vite module-directive and bundle-size warnings remain; they are unrelated to this change.

## Known limitation

The selected role remains browser-local. Cross-device continuity would require a server-backed user
preference and is outside this correction.
