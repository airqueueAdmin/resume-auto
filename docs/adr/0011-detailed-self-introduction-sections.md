# ADR 0011: Structured Detailed Self-Introduction Sections

## Status

Accepted

## Context

The resume-draft response previously placed the whole JobKorea 자기소개서 in one short `summary`
line. That made the generated content difficult to review and too easy for a provider to truncate.

## Decision

Add a versioned structured-output field named `selfIntroductionSections`. Each section maps to one
repeatable JobKorea `Header` and `Contents` pair and contains a concrete Korean appeal-topic title,
detailed content, and resolved source citations. The provider must return exactly three distinct
sections by default. The application rejects a different generated count, titles over 50 characters,
duplicate titles, short content, or uncited content. The output budget is increased to support three
multi-sentence sections.

The existing `summary` remains as a short overview for compatibility. Approval writes the overview
and every appeal topic into the immutable ResumeVersion using explicit numbered markers. The
JobKorea export parser restores each marker as a separate title/content pair. The editor starts with
the three generated items and allows the user to add or delete items, so the final form can contain N
sections without changing the generation default.

## Consequences

- Self-introduction quality is reviewable at the topic level instead of as one paragraph.
- Citation grounding applies to each detailed topic, so elaboration cannot silently become invented
  career evidence.
- Responses use more output tokens and may take longer; the budget remains configurable.
- `resume-draft-v5` and `resume-draft-v6` remain available as historical prompts, while the default
  is `resume-draft-v7`.
