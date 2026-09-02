# RoleDock — Proposed MVP Data Model

This document is a design reference for MVP v0.1. It is not a committed physical
schema and may evolve as each feature is implemented and tested.

## Candidate profile

### `CandidateProfile`

- `id`
- `title`
- `location`
- `summary`

### `Experience`

- `id`
- `candidate_profile_id`
- `company`
- `position`
- `start_date`
- `end_date`
- `description`

### `Education`

- `id`
- `candidate_profile_id`
- `institution`
- `title`
- `start_date`
- `end_date`

### `Skill`

- `id`
- `name`
- `category`

### `ProfileSkill`

- `candidate_profile_id`
- `skill_id`

### `ExperienceSkill`

- `experience_id`
- `skill_id`

### `Language`

- `id`
- `candidate_profile_id`
- `name`
- `level`

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
