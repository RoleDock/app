# RoleDock — Compatibility Scoring

## Goal

The implemented first slice is [individual requirement assessment](requirement-assessments.md).
Global scoring and aggregation below remain future design work. Missing evidence
in an incomplete profile produces UNKNOWN, not automatically MISSING; this
supersedes the shorthand absence examples below.

Produce a useful compatibility score without pretending that job compatibility
is objectively measurable to one decimal point. The result must remain
deterministic, explainable, and traceable to individual requirements.

## Pipeline

```text
Job offer
→ AI structured extraction
→ normalized requirements
→ deterministic comparison
→ requirement matches
→ weighted score
→ human-readable explanation
```

## Requirement importance

- `REQUIRED`: high impact
- `NICE_TO_HAVE`: lower impact
- `CONTEXT`: generally informational

## Match results

- `MATCH`
- `PARTIAL`
- `MISSING`
- `UNKNOWN`
- `NOT_APPLICABLE`

## Important rule

The LLM extracts and interprets language. Application code calculates the final
score.

## Examples

- Java required and demonstrated by candidate experience: `MATCH`.
- Kubernetes appreciated but absent: `MISSING`, with low impact.
- AWS mandatory but absent: `MISSING`, with significant impact.
- Three years requested and two years demonstrated: `PARTIAL`, not an automatic
  rejection.

These examples describe classification intent, not final numerical weights.

## Still to decide

Do not choose values until they can be evaluated against representative job
offers. Open decisions are:

- exact weights;
- experience-year calculation;
- contribution of location and preferences;
- contribution of contract type;
- skill-synonym normalization;
- scoring caps and floors.

These decisions should be made using real job-offer tests rather than arbitrary
theoretical values.
