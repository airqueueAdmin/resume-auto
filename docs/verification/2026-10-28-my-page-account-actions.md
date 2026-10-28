# My Page account actions — 2026-10-28

This records a frontend navigation refinement. It is not a phase completion record.

## Objective

Keep the workspace sidebar focused on career work while giving account-level actions a clear home.

## Implemented

- Removed `사용 안내` and `회원탈퇴` controls from the lower-left sidebar.
- Made the lower-left user avatar/profile area navigate to `/my-page`.
- Added a My Page with account summary, `사용 안내 다시 보기`, and `회원탈퇴 진행`.
- Added a Chrome extension guide covering installation, one-time service connection, JobKorea usage,
  review-before-fill behavior, and manual final submission.
- Expanded the installation step for non-developers with ZIP extraction, `chrome://extensions`,
  Developer mode, Load unpacked, `manifest.json` folder selection, and toolbar pinning instructions.
- Kept the detailed installation instructions inside the service instead of linking users directly to a
  repository source-document path.
- Moved the extension download to the service-hosted `/downloads/career-note-extension-v0.3.4.zip`
  artifact and added `scripts/package-extension.ps1` for repeatable packaging. The UI can be pointed at
  a CDN URL through `VITE_EXTENSION_DOWNLOAD_URL` at frontend build time.
- Added the final upload handoff to `https://resume.jobkorea.co.kr/resume/upload`: the export modal
  enables the upload-page action only after a PDF is generated and shows the generated filename plus
  the manual upload steps.
- Kept the existing confirmation, session clearing, and login redirect behavior for account deletion.
- Removed the duplicate dashboard-top usage-guide action; replay is available from My Page.
- Kept the automatic first-login tour unchanged.

## Verification

- `frontend`: `npm run build` passed.
- `git diff --check` passed for the changed frontend files.
- `docker compose up -d --build` completed and recreated the frontend container.
- Backend, frontend, and PostgreSQL containers reported `healthy`.
- `http://localhost:5173/` returned `200 OK`.

## Scope

No backend endpoint, database migration, authentication contract, or account deletion behavior was
changed.
