# Landing service positioning verification

## Goal

Make the unauthenticated landing page clearly explain that Career Note helps users prepare job-fit application documents across multiple recruiting platforms and job postings.

## Changes

- Rewrote the hero message around collecting job postings, connecting personal experience, and preparing a tailored resume.
- Added a compact value section for multi-platform job management, evidence-based writing, and user-reviewed submission.
- Reframed the four-step workflow as:
  1. Import a job posting.
  2. Connect experience and projects.
  3. Prepare a job-fit resume and application content.
  4. Review, export, and submit manually on the target platform.
- Kept the final review and platform submission disclaimer visible so the landing page does not imply automatic submission.
- Updated the dashboard preview labels and responsive layout to reflect the job-fit application workflow.

## Verification

- `npm run build` in `frontend/`: passed.
- `docker compose build frontend`: passed.
- `docker compose up -d --no-deps --force-recreate frontend`: passed; frontend container is healthy on port `5173`.
- `http://localhost:5173/`: returned HTTP `200` after the container recreation.
- `git diff --check`: passed; Git only reported the existing LF-to-CRLF normalization warning.
- Existing Vite warnings about bundle size and React Router module directives remain unrelated to this change.
