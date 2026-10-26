# ADR 0020: Use a Thin Chrome Extension for Application Assistance

## Status

Accepted for the browser-extension product track. The first implementation step is detection-only.

## Context

Company application pages often use non-standard questions and form layouts. Keeping users on the
existing web service is still the right experience for Career data, JobPosting data, ResumeVersion
management, AI generation, provenance, and export. A browser extension can reduce the repeated copying
between that service and a third-party application page without replacing the service or automating the
final submission. JobKorea is the first target because the existing service already supports JobKorea-
compatible ResumeVersion review and file export.

## Decision

Build the extension as a thin Manifest V3 companion client. It may inspect visible fields and the current
posting in the active JobKorea tab, request a review-only resume draft or answer grounded in Experience/
Project evidence held by the existing backend, show the generated content and provenance for review, and
fill only explicitly approved values. The current posting remains ephemeral request context: the extension
does not automatically create a JobPosting, ResumeDraft, Resume, or ResumeVersion. The panel must show the
posting source, captured character counts, and expandable full text so the user can verify the extraction.
The user submits the completed form on JobKorea's site. The direct web application continues to work
unchanged.

The first step is a detection-only prototype with `activeTab` and `scripting` permissions. It defines a
normalized field contract and validates it with a static form fixture before authentication, AI calls,
site adapters, or autofill are added.

## Rejected alternatives

- A second full resume-management UI inside the extension would duplicate the existing web application.
- Background scraping across all sites would require broad permissions and be fragile for dynamic pages.
- Automatic submission, login automation, CAPTCHA handling, or cookie collection would create security,
  privacy, and site-policy risks.

## Consequences

- The web service remains the source of truth and continues to work unchanged when opened directly.
- Generation readiness requires Career source data and active retrieval evidence; the extension must show
  actionable guidance rather than presenting unrelated saved resumes or postings.
- The extension requires site-specific handling as supported pages evolve.
- JobKorea's page structure may change; the initial adapter must prefer semantic labels and visible fields
  over private APIs or brittle pixel-level selectors.
- Users retain final review and submission responsibility.
- Future authentication must use short-lived, least-privilege access and must not embed backend secrets.
