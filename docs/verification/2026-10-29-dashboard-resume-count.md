# Dashboard resume count alignment — 2026-10-29

This records a frontend count correction. It is not a phase completion record.

## Objective

Show the same resume count on the dashboard as the count shown in the resume menu's left panel.

## Implemented

- Changed the dashboard resume count from the raw resume-row total to the number of groups returned by
  the existing `groupResumes` rule.
- Loaded all resume pages in both the dashboard calculation and resume menu so the two counts remain
  aligned beyond the first API page.
- Kept experience, project, and job-posting counts unchanged.

## Verification

- `frontend`: `npm run build` passed with TypeScript checking and Vite production compilation.
- Existing Vite module-directive and bundle-size warnings remain; they are unrelated to this change.
