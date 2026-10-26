# Phase 13 - Browser Extension Application Assistance Plan

## Objective

Reduce repetitive work in non-standard company application forms while keeping the existing web service
as the source of truth and leaving final submission to the user on the target site.

## Initial target

JobKorea is the first supported site. The existing web service remains the direct-entry experience and
continues to generate reviewed JobKorea-compatible files. The extension adds JobKorea page detection and
future form-assistance capabilities; it does not automate JobKorea login, upload, or final submission.

## Planned steps

### Step 1 - Field contract and detection prototype

- Inventory representative JobKorea application forms.
- Define a normalized question/field model.
- Add a Manifest V3 prototype using least-privilege active-tab access.
- Recognize JobKorea pages and validate visible field detection with a static fixture.

### Step 2 - Service authentication and answer context

- Define a short-lived extension-to-service authentication flow.
- Allow the user to select an existing ResumeVersion.
- Return grounded answer context and provenance from the existing backend.

### Step 3 - Review and user-approved fill

- Show generated answers beside each detected field.
- Enforce user approval before filling.
- Handle character limits, required fields, and supported rich-text controls.

### Step 4 - Site adapters and quality evaluation

- Add adapters only for measured target sites.
- Test extraction, field mapping, answer length, provenance, and failure recovery.
- Keep automatic submission, login automation, CAPTCHA handling, and JobKorea upload automation excluded.

## Explicitly excluded from the phase

- Replacing the current web application.
- Browser-side storage of backend secrets, passwords, cookies, or refresh tokens.
- Automatic final submission on any company site.
