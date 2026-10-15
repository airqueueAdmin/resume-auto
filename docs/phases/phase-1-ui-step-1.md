# Phase 1 UI Step 1 — Frontend Foundation

## Goal

Add a real browser client without mixing frontend rendering into the Spring Boot application.
This step connects the existing Phase 1 authentication contract and establishes a UI foundation
for the Career resource screens.

## Technology decision

- React with TypeScript: component-based UI and explicit client-side types.
- Vite: fast local development server and production bundling.
- React Router: URL-level separation for login, signup, and the protected workspace.
- Native `fetch`: keep the API boundary visible before introducing a data-fetching framework.
- Vite development proxy: route `/api` to the local Spring Boot server and avoid local CORS setup.

## Implemented scope

- Login screen: `POST /api/v1/auth/login`
- Signup screen: `POST /api/v1/auth/signup`, followed by login
- Logout: `POST /api/v1/auth/logout`
- Access JWT stored in `sessionStorage` for the current browser session
- Refresh Session token stored in `localStorage` for the current local-development flow
- Protected dashboard shell with the authenticated user identity
- Responsive layout for desktop and mobile widths
- Disabled navigation placeholders for Experience, Project, Resume, and Job Posting

The disabled navigation items are intentional. They make the future product navigation visible,
but do not pretend that CRUD UI has already been implemented.

## Run

From the repository root:

```powershell
cd frontend
Copy-Item .env.example .env
npm install
npm run dev -- --host=127.0.0.1
```

Open `http://localhost:5173` while the Spring Boot server is running on port `8080`.

## Verification

- `npm run build`: passed.
- Vite development server: returned HTTP 200 for `/`.
- `/api/v1/auth/login` proxy preflight: returned HTTP 204 from the backend.

## Important security note

The frontend currently stores the Refresh Session token in `localStorage` because the Phase 1
backend accepts it in a JSON request body and does not yet issue an HttpOnly cookie. This is
acceptable only for the current local learning flow. Before production deployment, the token
transport should be revisited together with CSRF protection, XSS hardening, and a secure cookie
strategy.

## Completion criteria

- [x] Frontend project exists under `frontend/`.
- [x] TypeScript production build passes.
- [x] Login and signup call the existing backend contract.
- [x] Logout revokes the Refresh Session through the backend.
- [x] Protected dashboard is not accessible without client authentication state.
- [x] Local development proxy is documented.

## Next UI step

Implement the Experience and Project list/create/edit/delete screens against the existing paginated
APIs, including loading, empty, validation, unauthorized, and API error states.
