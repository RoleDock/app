# RoleDock — Proposed MVP Data Model

This document is a design reference for MVP v0.1. It is not a committed physical
schema and may evolve as each feature is implemented and tested.

## Candidate profile

The MVP stores one current candidate profile as a single aggregate. The REST API
loads or replaces that aggregate through `/api/profile`; child identifiers are
UUIDs and ordered collections persist an explicit position. Skills belong to the
profile independently and an experience links to them only through explicit
skill identifiers supplied by the candidate.

### `CandidateProfile`

- `id`
- `main_title`
- ordered target roles
- `current_location`
- `professional_summary`
- mobility and ordered desired locations
- ordered work modes and contract types
- `additional_information`
- `certifications_complete`: explicit completeness declaration, false by default;
  introduced by additive migration 0005 for conservative certification assessment.

### `Experience`

- `id`
- `candidate_profile_id`
- `company`
- `position`
- `location`
- `start_date`
- `end_date`
- `current_position`
- `description`
- ordered achievements
- ordered explicit links to profile skills

### `Education`

- `id`
- `candidate_profile_id`
- `institution`
- `title`
- `field_of_study`
- `start_date`
- `end_date`
- `description`

### `Skill`

- `id`
- `candidate_profile_id`
- `name`
- `category`

### `ExperienceSkill`

- `experience_id`
- `skill_id`
- `sort_order`

### `Language`

- `id`
- `candidate_profile_id`
- `name`
- `level`

### `Certification`

- `id`
- `candidate_profile_id`
- `name`
- `issuing_organization`
- `issue_date`
- `expiration_date`
- `credential_id`
- `credential_url`

### `Project`

- `id`
- `candidate_profile_id`
- `name`
- `project_role`
- `description`
- `start_date`
- `end_date`
- `project_url`

## Job offers and analysis

### `JobOffer`

Implemented as a draft aggregate in `job_offer`, with UUID identity, original
advertisement text (preserved without trimming), optional source URL, company,
position, source language, summary, city/region/country, contract type, work
arrangement type/remote area/onsite days, and server-generated analysis timestamp.
Ordered missions live in `job_offer_mission`; ordered requirements are children.

Three representations remain deliberately distinct:

- `original_text` is the source advertisement, preserved verbatim.
- `initial_extraction` is a JSON text snapshot of the initial validated public
  `JobOfferExtraction` contract, generated on the server. It contains no provider
  envelope, credentials, prompt or diagnostics. JPA marks it and the source text
  non-updatable. No revision or audit framework is introduced.
- Current structured columns and children initially equal the snapshot and can
  later be corrected independently.

`review_status` defaults to `UNREVIEWED`. An explicit review without changes sets
`CONFIRMED`; changing persisted structured values sets `CORRECTED`. Reconfirming
corrected data keeps `CORRECTED`. Nullable `review_bypassed_at` records an explicit
choice to continue without review; it does not change `UNREVIEWED`. Later review
takes precedence over that timestamp, which remains as provenance.
Draft describes this saved offer; it is not an application-tracking status.

### `JobRequirement`

Implemented in `job_requirement`: UUID, offer FK, order, raw quotation, canonical
label, category, requirement kind, centrality, explicitness, potential hard-blocker
flag and optional condition, optional constraint operator/value/unit, extraction
confidence. Enums retain the existing extraction contract names. Requirements expose stable UUIDs and a `source`: `LLM_EXTRACTED` (the default for
existing rows) or `USER_ADDED`. Extracted quotations and extraction confidence are
preserved across edits. Manual requirements have null quotation and confidence;
the product never fabricates an LLM citation for them. No candidate evidence,
matching or scoring fields are stored.

Liquibase migration `0003-job-offers.sql` adds these three tables. Foreign keys,
ordering uniqueness, review-state and onsite-day bounds, and constraint/blocker
consistency protect stored invariants. JSON is portable TEXT because the initial
snapshot is read as a whole, not queried.

Additive migration `0004-job-offer-review.sql` adds the bypass timestamp and
requirement source, allows null raw text/confidence for manual requirements only,
and enforces that provenance distinction with a database constraint. Migration
0003 and earlier migrations remain unchanged.

### `RequirementAssessment` (implemented, not persisted)

On-demand result keyed by requirement UUID, with status, transfer relation,
evidence strength, assessment confidence, eligibility effect, typed candidate
evidence references and explanations. See [deterministic assessments](requirement-assessments.md).
No assessment table or global score is introduced.

The following historical analysis/matching entities remain conceptual and unimplemented.

### `OfferAnalysis`

- `id`
- `job_offer_id`
- `score`
- `summary`

### `RequirementMatch`

- `id`
- `offer_analysis_id`
- `job_requirement_id`
- `result`
- `explanation`

## Applications and generated content

### `Application`

- `id`
- `job_offer_id`
- `offer_analysis_id`
- `status`
- `created_at`
- `sent_at`

### `GeneratedMessage`

- `id`
- `application_id`
- `content`
- `character_limit`
- `created_at`

### `MotivationLetter`

- `id`
- `application_id`
- `content`
- `created_at`

## Open storage decisions

Strengths, attention points, and CV keywords still need a final storage decision
during implementation. Reasonable options include normalized child tables,
structured JSON, or derived data. This choice has not been made and should be
driven by actual querying and lifecycle requirements.

## Design reference

The existing visual design is available in
[Lucidchart](https://lucid.app/lucidchart/8c5ff11e-4a04-4843-b922-15716d692e5e/edit).
