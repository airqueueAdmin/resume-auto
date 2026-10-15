# Phase 1 UI Step 7 — Career TXT Import

## Goal

Reduce repetitive manual entry by mapping labeled plain text into Experience and Project forms,
while keeping the user in control of the final persisted data.

## Design reason

The first import format is deterministic `항목명: 값` text. It is explainable, works offline, has no
token or provider cost, and does not guess facts from unstructured prose. Imported values populate
the form only; the user reviews and explicitly saves them through the existing API flow.

## Implemented scope

- TXT import popup on both Career tabs
- Direct text paste and local UTF-8 `.txt` file reading
- 1MB client-side file size limit
- Korean and English label aliases
- Multiline description parsing
- Automatic removal of Markdown bold markers such as `**회사명**: **Acme Inc.**`
- Date normalization for Project fields (`YYYY-MM-DD`, `YYYY.MM.DD`, and `YYYY/MM/DD`)
- `현재`, `진행중`, `재직중`, and corresponding English values for a Project open end date
- Clear errors for unknown labels, unsupported files, invalid Project dates, and file read failures
- Missing imported fields preserve values already present in the form
- Imported values require review and a separate explicit save action

Files are read only in the browser and are not uploaded as files. Experience JSON no longer includes
employment dates; Project JSON keeps its date fields.

## Accepted examples

Experience:

```text
회사명: Acme Inc.
직무: Backend Developer
설명:
Spring Boot API를 개발했습니다.
PostgreSQL 성능을 개선했습니다.
```

Project:

```text
프로젝트명: AI Career Platform
역할: Backend Developer
시작일: 2025-01-01
종료일: 현재
기술 스택: Java, Spring Boot, PostgreSQL
Repository URL: https://github.com/example/project
설명:
커리어 데이터를 관리하는 서비스를 개발했습니다.
```

## Explicit exclusions

- Natural-language fact inference through an LLM
- Multiple records in one TXT document
- Automatic database persistence after import
- PDF, DOCX, spreadsheet, and non-TXT file parsing

## Verification

- `npm run build`: passed.
- TypeScript compilation and Vite production bundling completed successfully.
- Vite emitted informational `use client` directive warnings from `react-router`; these originate
  in the dependency bundle and do not fail the build.

## Completion criteria

- [x] Experience TXT values map to the corresponding form fields.
- [x] Project TXT values map to the corresponding form fields.
- [x] Paste and file-selection flows are available.
- [x] Invalid input produces an actionable message before form application.
- [x] Import does not save data automatically.
- [x] Existing backend contracts remain unchanged.
