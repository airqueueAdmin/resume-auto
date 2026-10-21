# ADR 0014: Link Self-Introduction Questions to Job Postings

## Status

Accepted

## Context

The first self-introduction answer flow stored a question under a Resume but did not associate it
with a saved company posting. Generation searched with only the question and then added all of the
user's indexed career chunks, so the answer could not be tailored to the actual role.

## Decision

Each application question references one saved JobPosting. The API validates that both the Resume
and JobPosting belong to the authenticated user. The Resume screen requires a posting selection
before saving or generating an answer.

Generation loads the saved question and its associated active JobPosting on the server. Retrieval is
focused on the question and role title so a long posting does not drown out the candidate evidence
query. The full saved posting is still passed as tailoring context with only retrieved user-owned
Experience/Project evidence. The posting itself is not a citation or proof of a candidate claim. AI
output remains a preview until the user reviews and saves it.

The migration leaves `job_posting_id` nullable for existing questions. New and updated API requests
require a posting; legacy questions must be linked before generation.

## Consequences

- Answers can target a particular saved company and role while one Resume can hold questions for
  multiple applications.
- Retrieval context is bounded by top-K and the existing context-character limit instead of appending
  every indexed career chunk.
- Updating or deleting a JobPosting remains governed by the existing soft-delete behavior. A deleted
  posting cannot be used to generate a new answer.
- The prompt version advances to `self-introduction-v4` to enforce evidence-only claims and
  structured sentence-level citation IDs rendered inline by the server.
