# Phase 13 Step 1 - Field Contract and Detection Prototype

## Status

Complete as an initial prototype. Phase 13 remains in progress.

## Objective and scope

Establish the smallest executable extension boundary for JobKorea: recognize the target host, inspect
visible application fields on the active page, and display a normalized summary. No backend, AI,
authentication, autofill, or submission behavior is included.

## Implemented

- Added a Manifest V3 extension under `extension/`.
- Selected JobKorea as the initial target site and added host recognition without broad host permissions.
- Limited permissions to `activeTab` and `scripting`.
- Detects visible inputs, textareas, selects, and contenteditable controls.
- Reads labels, ARIA labels, placeholders, legends, required state, field type, and maximum length.
- Added a static application form fixture and local loading instructions.
- Recorded the architecture in ADR 0020 and the Phase 13 plan.

## Verification

- `npm run build` for the existing frontend passed.
- The extension files are plain HTML/JavaScript and can be loaded through Chrome's **Load unpacked** flow.
- The static fixture covers labeled text, textarea length, fieldset/select, and file input cases.
- `git diff --check` passed.

## Known limitations

- The prototype does not yet call the backend or generate answers.
- It does not fill fields or submit forms.
- Detection is heuristic and may need site-specific adapters for dynamic controls and rich-text editors.

## Next step

Define the short-lived authentication and normalized backend response contract after representative target
application pages have been selected.
