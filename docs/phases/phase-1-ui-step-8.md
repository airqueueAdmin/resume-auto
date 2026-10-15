# Phase 1 UI Step 8 - JobPosting URL Import Preview

## Objective

Allow a user to provide a public recruitment-posting URL and map the page into the existing
JobPosting form before saving it.

## Design decisions

- Import is a preview operation. The URL is fetched and parsed, but no database row is created until
  the user applies the preview and presses the existing save button.
- Schema.org `JobPosting` JSON-LD is preferred because it provides stable semantic fields.
- Page metadata (`title`, Open Graph, Twitter) and visible HTML text are fallback sources for pages
  without usable JSON-LD.
- The backend only follows a small number of redirects, limits the response to 2 MB, accepts HTML
  content, and rejects non-public hosts to reduce SSRF risk.
- The existing JobPosting create/update API remains the only persistence path.

## Implemented

- `POST /api/v1/job-postings/import-preview`
- URL validation and authenticated access
- JSON-LD `JobPosting` extraction for title, company, and description
- Metadata/HTML fallback extraction with warnings for missing fields
- React modal with URL input, preview, warnings, and explicit form application
- Unit tests for JSON-LD extraction, fallback extraction, and private-host rejection
- Integration tests for authentication and URL validation behavior

## Explicitly excluded

- Automatic database save
- Site-specific adapters for authenticated or JavaScript-only pages
- CAPTCHA/login bypass
- LLM-based extraction
- Caching or background crawling

## Verification

- `mvnw.cmd -q -Dtest=JobPostingImportServiceTest test` passed.
- `npm run build` in `frontend/` passed.
- The full Maven test suite remains the final verification after the integration test run.

## Known limitations

Some recruitment platforms render the posting only after JavaScript executes or require login. Those
pages may produce an incomplete preview; the user can correct the form manually before saving.

## Next step

Use the imported JobPosting as the source for the existing analysis, retrieval, matching, and grounded
resume workflows. URL import itself does not change the AI/RAG source-of-truth rules.
