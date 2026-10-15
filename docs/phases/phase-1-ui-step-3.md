# Phase 1 UI Step 3 — Project Management

## Goal

Add Project CRUD to the frontend using the existing user-owned Project API. This step extends the
working Experience UI pattern with project-specific technical metadata.

## Implemented scope

- Project collection screen
- Paginated list using `page` and `size`
- Create Project
- Edit Project
- Soft-delete Project
- Required-field validation
- Start/end date range validation
- `repositoryUrl` HTTP/HTTPS validation
- Backend validation error display
- Empty, loading, API error, and retry states
- Technology stack badge and repository link display
- Responsive layout
- Workspace sidebar navigation

## API contract used

```text
GET    /api/v1/projects?page=0&size=5
POST   /api/v1/projects
PUT    /api/v1/projects/{projectId}
DELETE /api/v1/projects/{projectId}
```

Create and update send:

```json
{
  "name": "AI Career Platform",
  "role": "Backend Developer",
  "startDate": "2024-01-01",
  "endDate": null,
  "description": "Implemented ...",
  "techStack": "Java, Spring Boot, PostgreSQL",
  "repositoryUrl": "https://github.com/example/project"
}
```

The frontend does not send a user ID. Ownership continues to be derived by the Backend from the
authenticated JWT subject.

## Design decisions

- Project uses the same form/list interaction model as Experience to keep the first UI version
  consistent and easy to compare with the API contract.
- `repositoryUrl` is optional, but if supplied it must use `http` or `https` both in the browser
  and in the Backend validation layer.
- `techStack` remains a user-provided string in Phase 1. It will become useful metadata for
  future embedding and retrieval, but no AI behavior is introduced here.
- The UI reuses existing generic visual classes without introducing a component library yet.

## Verification

- `npm run build`: passed.
- Vite development server returned HTTP 200 for the frontend entry point.
- Vite `/api` proxy returned the Backend response for a Project endpoint preflight.
- Authenticated CRUD should be verified manually from the browser after logging in.

## Completion criteria

- [x] Project list is connected to the paginated API.
- [x] Project create and update are connected.
- [x] Project soft-delete is connected.
- [x] Project-specific URL and tech-stack behavior is represented.
- [x] Client and server error states are represented.
- [x] Documentation is updated.

## Next UI step

Implement Resume and immutable ResumeVersion screens. The UI must preserve the Backend rule that
editing content creates a new version rather than mutating an existing ResumeVersion.
