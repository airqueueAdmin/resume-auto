# Phase 1 UI Step 4 — Resume and ResumeVersion

## Goal

Connect the Resume and immutable ResumeVersion APIs to a browser workflow. The UI must make the
versioning rule visible: Resume metadata can be renamed, but existing ResumeVersion content is
read-only and editing content creates a new version.

## Implemented scope

- Resume list
- Resume creation with the initial ResumeVersion 1
- Resume metadata rename
- Resume soft-delete
- ResumeVersion history list
- Read-only version snapshot preview
- New ResumeVersion creation
- Current latest version indicator
- Empty, loading, and API error states
- Responsive two-panel resume workspace

## API contract used

```text
GET    /api/v1/resumes?page=0&size=20
POST   /api/v1/resumes
PUT    /api/v1/resumes/{resumeId}
DELETE /api/v1/resumes/{resumeId}
GET    /api/v1/resumes/{resumeId}/versions?page=0&size=20
POST   /api/v1/resumes/{resumeId}/versions
```

Resume creation sends:

```json
{
  "name": "Backend Developer Resume",
  "content": "..."
}
```

New version creation sends only the new content:

```json
{
  "content": "...updated resume content..."
}
```

The frontend does not send a user ID. The Backend derives ownership from the authenticated JWT
subject and validates the resume relationship for every version request.

## Design decisions

- Creating a Resume uses the Backend transaction that creates Resume and Version 1 together.
- Rename is kept separate from content editing because Resume metadata and ResumeVersion content
  have different mutability rules.
- Existing version content is rendered in a read-only `<pre>` snapshot.
- The new-version form is separate from the snapshot to prevent accidental mutation semantics.
- Version history is ordered by the Backend's latest-version-first contract.
- Resume content is currently plain text. Rich text, Markdown preview, and AI-generated content are
  intentionally deferred until after the LLM phases define content and provenance requirements.

## Verification

- `npm run build`: passed.
- Vite development server returned HTTP 200 for the frontend entry point.
- Vite `/api` proxy returned the Backend response for a Resume endpoint preflight.
- Authenticated CRUD and version creation should be verified manually from the browser after
  logging in.

## Completion criteria

- [x] Resume list and creation are connected.
- [x] Resume rename and soft-delete are connected.
- [x] ResumeVersion history is connected.
- [x] Existing versions are presented as read-only snapshots.
- [x] New versions are created through a separate form.
- [x] Documentation is updated.

## Next UI step

Implement JobPosting management. AI analysis actions should remain visibly unavailable until the
Backend Phase 2 LLM API is introduced.
