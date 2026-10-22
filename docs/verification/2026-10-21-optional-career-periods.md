# Optional career periods — 2026-10-21

This is a feature verification record, not a phase completion record.

## Objective

Allow users to record optional periods for career history and projects. Preserve the previous compact
display when no period is entered, and represent current employment explicitly instead of inferring it
from a missing end date.

## Design

- `startDate` and `endDate` are nullable for both career history and projects.
- Career history stores `currentlyEmployed` separately so an unknown end date is not confused with
  current employment.
- Current employment cannot have an end date.
- Date order is validated only when both dates are present.
- Existing clients may omit the new fields; omitted `currentlyEmployed` defaults to `false`.
- Period text is omitted from lists, export descriptions, and retrieval source documents when no
  period or current-employment status exists.

## Implementation

- Added Flyway migration `V16__make_career_periods_optional.sql`.
- Extended career-history API, domain, persistence, tool arguments, and responses with optional dates
  and current-employment status.
- Relaxed the project start-date requirement across the database, API, domain, tools, and frontend.
- Added optional date controls and a `재직 중` checkbox to the career-history form.
- Updated TXT import to accept career periods and `종료일: 재직 중`.
- Added domain and authenticated API coverage for omitted periods and current employment.

## Verification

- `npm run build`
- `./mvnw test`
