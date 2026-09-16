# RoleDock — Offer analysis v0

## Implemented architecture and lifecycle

Extraction, individual `RequirementAssessmentService` matching and
`OfferAnalysisService` aggregation are separate layers. The assessment engine is
unchanged. `JobOfferAssessmentService` loads the current offer and profile in one
read-only REPEATABLE_READ transaction, computes assessments for an explicit UTC
evaluation date, then calls the pure aggregator with those projections and results.
No LLM call, persistence, schema change or caching is introduced. Recompute on
request after profile edits or offer corrections; the evaluation date is part of
the reproducible input (ongoing jobs and credentials can change with the date).
The aggregator rejects missing, extra or duplicate assessment IDs and inconsistent
assessments of exact duplicate requirements.

`GET /api/job-offers/{id}/analysis` returns a DTO with `assessedOn`, `reviewStatus`,
`coverageScore`, `eligibility`, `recommendation`, `criticalGaps`, `uncertainty`,
`requirementAssessments` and `contributions`. Each contribution exposes requirement
ID, weight, coverage, inclusion, duplicate reference and a French explanation.
404/400 follow the existing offer endpoint conventions. The existing
`/requirement-assessments` endpoint remains available.

## Implemented deterministic formula

```
weight = requirementKindWeight * centralityWeight
coverageScore = 100 * sum(included weight * coverage) / sum(included weight)
```

All numeric parameters are centralized in `ScoringRules`.

| Kind | Weight |
| --- | ---: |
| REQUIRED | 4 |
| PREFERRED | 1 |
| CONTEXTUAL | 0 |
| UNKNOWN | excluded |

| Centrality | Weight |
| --- | ---: |
| CORE | 2 |
| SUPPORTING | 1 |
| INCIDENTAL | 0.5 |
| UNKNOWN | excluded, never defaulted to CORE or SUPPORTING |

| Assessment | Coverage |
| --- | --- |
| MATCH | 1 |
| MISSING | 0, included when weight is positive |
| UNKNOWN | excluded from numerator and denominator; increases uncertainty |
| NOT_APPLICABLE | excluded |
| PARTIAL | rule below; unsupported partials are excluded and explained |

The centralized transfer mapping is EXACT=1, EQUIVALENT=0.75, ADJACENT=0.5,
PREREQUISITE=0.25, NONE=0. Transfer describes the relation, not necessarily complete
coverage of a quantitative constraint. The current engine emits PARTIAL/EXACT
for experience shortfalls: EXACT identifies the linked skill. For PARTIAL in
EXPERIENCE with AT_LEAST, referenced EXPERIENCE evidence and EXACT or NONE,
coverage is explicitly **0.75**. This is a conventional credit for the existing
engine's approximately one-year-short rule, **not** a computed duration ratio.
Other PARTIAL results use EQUIVALENT/ADJACENT/PREREQUISITE directly. PARTIAL with
EXACT or NONE outside that experience rule has no safe numeric interpretation and
is excluded with an uncertainty reason. No rationale text parsing or second
matching decision is performed. Transfer graphs are still unimplemented.

Zero denominator returns JSON null (UI: “Indisponible”), never 0 or NaN.
Contributions are summed in requirement UUID order. All current weights and
credits are binary-exact fractions; public scores are rounded HALF_UP to two
decimals. Recommendation thresholds use the unrounded ratio. The UI shows the
returned score and each contribution without calculating business rules.

## Exact duplicates

Collapse only requirements equal after NFC/case/whitespace normalization of label
and blocker condition, with identical category, kind, centrality, explicitness,
blocker flag, structured constraint, source and extraction confidence. UUID and raw
quotation are ignored. Constraint strings must be identical; no inferred unit or
numeric equivalence. The lowest UUID is the stable representative. Require the
same assessment status, transfer, evidence, confidence and eligibility effect.
All original assessments remain visible; duplicate contributions reference the
representative and are excluded. Uncertainty and gaps count representatives once.
No alias or semantic deduplication, redundancy group or similarity engine exists.
Synonyms and near-duplicates remain a known limitation.

## Critical gaps

Independently of total score, show REQUIRED + CORE + MISSING and REQUIRED + CORE
+ PARTIAL with known coverage <= 0.25 (the provisional severe-shortfall boundary).
Each gap reuses assessment requirement ID, label, status and rationale. UNKNOWN is
reported under uncertainty, not silently made a gap. A high score never suppresses
a critical gap or a confirmed blocker.

## Explainable uncertainty, not statistical confidence

Ignore CONTEXTUAL and NOT_APPLICABLE rows. Count unique UNKNOWN assessments (and
how many are REQUIRED), LOW assessment confidence, unknown kind/centrality, and
unsupported PARTIAL coverage. Add separate reasons for possible blockers,
UNREVIEWED extraction, absent profile and zero denominator.

HIGH if any: absent profile, zero denominator, unknown centrality on any applicable
non-contextual requirement (even MATCH), unresolved REQUIRED CORE, possible blocker,
eligibility-critical unknown, at least 3
UNKNOWN assessments or at least 3 LOW-confidence assessments. MEDIUM if another
reason exists; LOW otherwise. “Unresolved” means UNKNOWN or unsupported PARTIAL.
Unknown centrality remains excluded from coverage and forces VERIFY_FIRST even
when the known coverage is 100%. Unknown kind alone remains MEDIUM when no HIGH
condition applies. No numeric probability is calculated. MEDIUM assessment confidence alone does
not raise this information-completeness indicator; it remains visible in details.

## Eligibility, in precedence order

| Condition | Result |
| --- | --- |
| At least one safe BLOCK | NOT_ELIGIBLE |
| No profile, no requirements, or eligibility-critical unresolved requirement | UNKNOWN |
| POSSIBLE_BLOCK or a BLOCK that fails safety checks | ELIGIBLE_WITH_CONSTRAINT |
| Otherwise | ELIGIBLE |

A safe BLOCK must already be emitted by the assessment engine, be MISSING with
HIGH assessment confidence, refer to a REQUIRED EXPLICIT hard-blocker requirement
whose normalized condition equals its label, and use CONFIRMED/CORRECTED input.
The aggregator only checks provenance and safety, never creates another matching
decision. An unsafe BLOCK is downgraded to verification. Eligibility-critical
unknowns are REQUIRED unresolved hard-blockers or WORK_AUTHORIZATION, LOCATION,
AVAILABILITY, CONTRACT requirements. This category policy is a conservative v0
hypothesis; other mandatory skills affect coverage/uncertainty, not eligibility.
ELIGIBLE means no blocker identified within the modeled requirements, not an
external guarantee. Low coverage alone never creates NOT_ELIGIBLE.

## Recommendation, in precedence order

| Rule | Recommendation |
| --- | --- |
| NOT_ELIGIBLE | SKIP_CONFIRMED_BLOCKER |
| UNKNOWN or ELIGIBLE_WITH_CONSTRAINT eligibility, null coverage, or HIGH uncertainty | VERIFY_FIRST |
| Coverage >= 80, no critical gap and no REQUIRED MISSING/PARTIAL | APPLY_NOW |
| Coverage >= 60 and fewer than 2 critical gaps | APPLY_WITH_BRIDGE |
| Otherwise | STRETCH |

UNREVIEWED permits calculation and displays an explicit warning. With a possible
blocker, it always requires VERIFY_FIRST, including after review bypass. Without
blocker uncertainty, the unreviewed reason alone gives MEDIUM uncertainty and does
not force verification. Review status is never modified by analysis.

## Product hypotheses and limits

These weights, partial credits, severe-gap boundary, count boundaries, category
policy and 80/60 recommendation thresholds are provisional product hypotheses,
not scientifically validated measurements. Earlier documentation left weights
undecided; this slice explicitly implements the requested v0 hypothesis so it can
be tested against representative annotated offers. Future calibration should
compare annotated outcomes and sensitivity to weights, missing information,
experience proxies and duplicates. Coverage is not an interview or hiring
probability, ATS simulation or learned ranking. Bridge recommendations indicate
points to explain, not proof that a gap is remediable. Several uncertain excluded
requirements can coexist with 100% known coverage; uncertainty stays visible.

No semantic equivalence graph, free-text blocker interpretation, complete career
history inference or candidate fact invention is supported. See
[requirement assessments](requirement-assessments.md) for matching limitations.

## Validation of this slice (2026-09-16)

Aggregation tests were written first and failed before implementation. Final runs:
154 backend tests with H2, the same 154 tests against isolated PostgreSQL 17,
88 frontend tests and the Angular production build passed. Regression cases cover
all weights, exclusions, partial relations and the real experience engine,
unknown resolution, order independence, duplicate impact, high-score blockers,
low-score eligibility, unsafe blocker provenance, absent profiles and stale
assessment sets. API tests verify recomputation and zero extractor interaction.

The PostgreSQL run used an ephemeral `postgres:17-alpine` container named
`roledock-aggregation-test`, database `roledock_aggregation_test`, exposed only on
127.0.0.1:55432. Maven's test profile datasource URL, driver, username and password
were overridden with `-Dspring.datasource.*` properties. Never run these test
classes against development data: integration tests clear their fixture tables.
The temporary container was stopped after validation; development data was not
used or modified. No real candidate/offer data or provider calls were used.

Manual browser inspection used the real backend and six fictional offers:

| Case | Coverage | Result |
| --- | ---: | --- |
| Explicit Java skill and linked experience | 100 | ELIGIBLE / APPLY_NOW |
| Two years linked experience, three requested | 75 | ELIGIBLE / APPLY_WITH_BRIDGE |
| Three unknown required/core skills | unavailable | ELIGIBLE / VERIFY_FIRST; HIGH uncertainty |
| Ten matches and a missing required/core certificate | 90.91 | APPLY_WITH_BRIDGE; critical gap visible |
| Unreviewed missing blocking certificate | 50 | ELIGIBLE_WITH_CONSTRAINT / VERIFY_FIRST |
| Ten matches and a reviewed blocking certificate | 90.91 | NOT_ELIGIBLE / SKIP_CONFIRMED_BLOCKER |

The saved-offer summary and individual evidence were inspected in light/dark
modes, desktop and a 390px mobile viewport; no horizontal overflow was observed.
French decimal formatting is covered by a UI test. The stepper already used
“À suivre” before this slice and its existing regression test remains green.

### Review corrections (2026-09-16)

Unknown centrality on an applicable requirement now yields HIGH uncertainty even
when its assessment is MATCH and the remaining known coverage is 100%; contextual
and NOT_APPLICABLE rows remain ignored. Regressions cover both cases. Partial
transfer contribution rationales use French phrases instead of enum tokens.
The UI supplies neutral French labels for unrecognized enum values and displays
omitted/null scores as unavailable, while retaining evidence and critical gaps.
The extraction documentation now links to the implemented aggregation endpoint.
Validation after these corrections: 164 backend tests passed on both H2 and
isolated PostgreSQL 17; 90 frontend tests and the production build passed.
