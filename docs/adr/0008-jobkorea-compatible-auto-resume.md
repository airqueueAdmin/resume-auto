# ADR 0008: JobKorea-Compatible Auto-Created Resumes

## Status

Accepted and implemented through the Phase 10 user-controlled export boundary.

## Context

The current UI asks users to create an empty Resume before an approved AI draft can become a
`ResumeVersion`. This adds a document-management step before the user has a document to manage. The
user's career facts and the saved JobPosting already provide the inputs for a tailored resume.

JobKorea describes its own resume as a platform-provided resume form and distinguishes it from a
company-specific application form. Its current FAQ also describes multiple saved resumes and a selected
basic resume, which supports a resume-library workflow rather than one manually authored document.

The external form and any personal-resume API may change. The platform must not treat a scraped JobKorea
screen or an unverified upload contract as an internal domain schema.

## Decision

- The Resumes menu becomes a resume library: approved versions, target job, version history, review state,
  rename, and export/handoff actions. It will not prominently expose a blank “create resume” form.
- The first approved draft may atomically create a Resume container and its immutable Version 1. If the
  user selects an existing Resume, approval creates the next immutable version there instead.
- Generated content uses a structured internal template identified by a versioned key such as
  `JOBKOREA_STANDARD_V1`. This means JobKorea-compatible field mapping, not pixel-level cloning of the
  current JobKorea UI.
- The canonical document keeps user-confirmed profile fields, generated sections, target-job metadata,
  template version, and provenance separately. Missing facts remain explicitly empty or require user input;
  the generator must not invent JobKorea fields.
- Existing plain-text `ResumeVersion` rows remain readable as legacy versions. New structured versions and
  export adapters must preserve immutable snapshots and approval metadata.
- Phase 10 owns the verified mapping from the internal template to a JobKorea-compatible file export
  contract. The supported boundary is a user-controlled preview/download/handoff; the platform does not
  upload or publish the file to JobKorea.
- The existing backend Resume creation API may remain for compatibility, but the primary UI flow will not
  depend on it.

## Consequences

Positive:

- Users can go from a grounded draft review directly to a usable Resume.
- Resume versions remain immutable and can represent multiple job-targeted documents.
- A versioned internal schema isolates AI generation from changes in JobKorea's UI or external contract.
- User-owned facts and derived resume wording remain distinguishable for review and provenance.

Trade-offs:

- The Resume model must add structured sections and a template/schema version; plain text alone is not a
  sufficient long-term representation.
- Profile fields not currently modeled by the platform require explicit user input before export.
- Existing direct-create UI and the Phase 5 approval contract require a compatibility transition.
- JobKorea export still requires re-verifying fields, file constraints, privacy warnings, and the user's
  final upload action.

## Rejected alternatives

- Keeping mandatory blank Resume creation was rejected because it makes the user create an empty container
  before seeing a useful result.
- Hardcoding the current JobKorea HTML or private API was rejected because it is brittle and may violate
  service terms.
- Generating a PDF and calling it a native JobKorea resume was rejected because a file export and a
  platform-native structured resume are different contracts.

## References

- [JobKorea individual-member FAQ](https://www.jobkorea.co.kr/help/faq/user?tab=2)
- [ADR 0006: JobKorea integration boundary](0006-jobkorea-integration-boundary.md)
