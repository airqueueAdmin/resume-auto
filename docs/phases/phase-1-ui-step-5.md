# Phase 1 UI Step 5 — JobPosting Management

## Goal

Connect saved JobPosting records to the frontend without introducing AI behavior. The screen
stores the source job description that later phases will analyze.

## Implemented scope

- JobPosting collection screen
- Paginated list using `page` and `size`
- Create JobPosting
- Edit JobPosting
- Soft-delete JobPosting
- Required-field validation
- Optional source URL validation for HTTP/HTTPS
- Empty, loading, API error, and retry states
- Source URL link display
- Responsive layout
- Workspace sidebar navigation

AI analysis is intentionally not shown as an active feature. JobPosting is currently a plain source
record, consistent with the Backend Phase 1 boundary.

## API contract used

```text
GET    /api/v1/job-postings?page=0&size=5
POST   /api/v1/job-postings
PUT    /api/v1/job-postings/{jobPostingId}
DELETE /api/v1/job-postings/{jobPostingId}
```

Create and update send:

```json
{
  "title": "Senior Backend Engineer",
  "companyName": "Acme Inc.",
  "description": "...job description...",
  "sourceUrl": "https://jobs.example.com/posting"
}
```

The frontend does not send a user ID. The Backend derives ownership from the authenticated JWT
subject.

## Design decisions

- Job descriptions are shown with a visual preview clamp in the collection view, while the full
  text remains stored and editable through the form.
- Source URL is optional and opens in a new tab when present.
- The UI does not call an LLM, extract skills, calculate a match, or create citations. Those actions
  belong to later phases and will be added only after the direct LLM API contract exists.
- The same pagination and error-state patterns are used across Experience, Project, and JobPosting.

## Verification

- `npm run build`: passed.
- Vite development server returned HTTP 200 for the frontend entry point.
- Vite `/api` proxy returned the Backend response for a JobPosting endpoint preflight.
- Authenticated CRUD should be verified manually from the browser after logging in.

## Completion criteria

- [x] JobPosting list is connected to the paginated API.
- [x] JobPosting create and update are connected.
- [x] JobPosting soft-delete is connected.
- [x] Source URL behavior is represented.
- [x] AI analysis remains excluded from Phase 1.
- [x] Documentation is updated.

## Next step

The Phase 1 frontend resource screens are now covered. Before starting Phase 2, manually verify
the complete browser flow for authentication, Experience, Project, Resume/ResumeVersion, and
JobPosting against the running Backend.
