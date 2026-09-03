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

- `id`
- `company`
- `position`
- `location`
- `original_text`
- `source_url`
- `analyzed_at`

### `JobRequirement`

- `id`
- `job_offer_id`
- `normalized_skill_id` (nullable)
- `label`
- `type`
- `category`

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
