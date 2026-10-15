# Phase 1 UI Step 2 — Experience Management

## Goal

Connect the first user-owned Career resource to the frontend. This step uses the existing
Experience API and keeps the browser behavior aligned with the Backend Foundation contract.

## Implemented scope

- Experience collection screen
- Paginated list using `page` and `size`
- Create Experience
- Edit Experience
- Soft-delete Experience
- Empty, loading, API error, and retry states
- Client-side required-field validation
- Start/end date range validation
- Backend validation error display when `fieldErrors` is returned
- Responsive list and form layout
- Navigation from the workspace sidebar

## API contract used

```text
GET    /api/v1/experiences?page=0&size=5
POST   /api/v1/experiences
PUT    /api/v1/experiences/{experienceId}
DELETE /api/v1/experiences/{experienceId}
```

Create and update send:

```json
{
  "companyName": "Acme Inc.",
  "role": "Backend Developer",
  "startDate": "2024-01-01",
  "endDate": null,
  "description": "Implemented ..."
}
```

The frontend does not send a user ID. The Backend derives ownership from the authenticated JWT
subject, preserving the user data isolation rule.

## Design decisions

- The form is shown on the same page as the list to keep the first CRUD workflow easy to inspect.
- The frontend validates obvious input errors for fast feedback, but the Backend remains the
  source of truth for validation and ownership.
- Delete is a soft delete on the Backend; the UI presents it as removal from the active list.
- Page size is five for the UI so pagination can be exercised locally with a small dataset.
- The API client remains a small native `fetch` wrapper. A query/cache framework is intentionally
  deferred until multiple screens demonstrate the need for it.

## Verification

- `npm run build`: passed.
- Vite development server returned HTTP 200 for the frontend entry point.
- Vite `/api` proxy returned the Backend response for an Experience endpoint preflight.
- Authenticated CRUD should be verified manually from the browser after logging in because the
  test command does not create a browser session.

## Completion criteria

- [x] Experience list is connected to the paginated API.
- [x] Experience create and update are connected.
- [x] Experience soft-delete is connected.
- [x] Client and server error states are represented.
- [x] UI is responsive at narrow widths.
- [x] Documentation is updated.

## Next UI step

Implement Project management using the same proven API boundary, while extracting only the UI
components that are genuinely shared by both resources.
