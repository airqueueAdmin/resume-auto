# Phase 1 UI Step 6 — Unified Career Navigation

## Goal

Reduce workspace navigation duplication by grouping Experience and Project management under one
Career menu without changing their backend resource boundaries.

## Design reason

Experience and Project are both career evidence used by retrieval and RAG. They belong together in
the user's navigation, while remaining separate domain resources because their fields, validation,
ownership checks, and API contracts differ.

## Implemented scope

- Replaced the separate Experiences and Projects sidebar entries with one Career entry.
- Added Experiences and Projects tabs inside the Career screen.
- Added canonical frontend paths `/career/experiences` and `/career/projects`.
- Kept `/experiences` and `/projects` as compatible paths so existing bookmarks still render the
  corresponding Career tab.
- Updated the Overview guidance text to point to the Career menu.
- Left the Experience and Project API calls and backend models unchanged.

## Verification

- `npm run build`: passed.
- TypeScript compilation and Vite production bundling completed successfully.
- Vite emitted informational `use client` directive warnings from `react-router`; these originate
  in the dependency bundle and did not fail or alter the build.

## Completion criteria

- [x] The sidebar presents one Career menu.
- [x] Experience and Project management remain reachable through tabs.
- [x] Existing frontend paths remain compatible.
- [x] Backend resource boundaries and API contracts are unchanged.
- [x] The frontend production build passes.
