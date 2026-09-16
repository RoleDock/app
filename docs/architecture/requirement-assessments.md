# Requirement assessments v0

Implemented independently of extraction. `RequirementAssessmentService` consumes
one saved requirement projection, the current profile projection, review status
and an explicit evaluation date. It makes no provider calls and never changes
the profile or review state. Identical inputs, including the date, yield identical
results. `JobOfferAssessmentService` loads both aggregates in a read-only,
repeatable-read transaction and supplies the UTC date.

## API and lifecycle

`GET /api/job-offers/{id}/requirement-assessments` returns
`{ assessedOn: "YYYY-MM-DD", assessments: [...] }`. Unknown offer: 404; malformed
UUID: 400. No profile produces UNKNOWN results (context remains NOT_APPLICABLE).
No requirements produces an empty list. DTOs never expose JPA entities.

Results are recomputed, not persisted. Profile edits and offer corrections therefore
need no cache invalidation or assessment versioning. The saved-offer screen loads
these results and offers an explicit refresh. There is no global score, weighting,
eligibility aggregation, recommendation or percentage.

## Result contract

- `requirementId`: persisted requirement UUID.
- `status`: MATCH, PARTIAL, MISSING, UNKNOWN, NOT_APPLICABLE.
- `transferRelation`: EXACT, EQUIVALENT, ADJACENT, PREREQUISITE, NONE.
- `evidenceStrength`: STRONG, MODERATE, WEAK, NONE.
- `assessmentConfidence`: HIGH, MEDIUM, LOW; distinct from extraction confidence.
- `eligibilityEffect`: BLOCK, POSSIBLE_BLOCK, NONE; applies to this requirement only.
- `evidence`: small `{type, id, label}` references, without entity duplication.
- `rationale` and nullable `attention`: French explanations for inspection.

Current emitted evidence types: PROFILE_SKILL, EXPERIENCE, EDUCATION, LANGUAGE,
CERTIFICATION and CERTIFICATION_LIST (the profile UUID identifies its explicit
completeness declaration). IDs come from persisted candidate items. Identical references are
deduplicated within each result. SIGNIFICANT_PROJECT is reserved in the contract:
projects have no structured skill associations today. Achievements are ordered
strings, not independently identified entities; v0 does not fabricate their IDs or
extract evidence from mission/project prose.

## Supported rules and deliberate limits

| Category | Rule |
| --- | --- |
| TECH_SKILL / DOMAIN_KNOWLEDGE | Full canonical label equality with an explicit profile skill; linked experiences add stronger evidence. Additional constraints remain UNKNOWN. |
| EXPERIENCE | AT_LEAST, positive numeric years (up to 100), canonical label equal to a profile skill. Use dated experiences explicitly linked to that skill. Other labels, ranges or operators remain UNKNOWN. |
| EDUCATION | Exact declared degree title with no extra constraint. No inferred level or international equivalence. |
| LANGUAGE | Exact language (small French/English name alias table), one unambiguous profile entry, explicit A1–C2 levels, AT_LEAST and CEFR/CECRL or unspecified unit. Free-form proficiency remains UNKNOWN. |
| CERTIFICATION | Exact explicit title, no additional constraint or recognized alternative/generic wording. Present and not known expired/future-issued: MATCH. Expired/future-issued: UNKNOWN. No expiry means only “no known expiration”, not independent verification. |
| CONTEXTUAL kind | NOT_APPLICABLE. |
| Other categories | UNKNOWN, including soft skills, title/seniority, location, work authorization, availability and contract preferences. |

Normalization is centralized in `MatchingLabels`: Unicode NFC, locale-independent
lowercase, whitespace normalization. Punctuation and accents are retained. The only
skill aliases are JS/JavaScript, TS/TypeScript, Postgres/PostgreSQL and
SpringBoot/Spring Boot. These names share EXACT semantics; no substring matching.
Java/JavaScript, C/C++, .NET/ASP.NET and SQL/NoSQL remain distinct.

No equivalence, adjacency or prerequisite graph is implemented. React/Angular and
Docker/Kubernetes do not match. Future technology transfer mappings would be
explicit product hypotheses, not scientific facts.

### Duration hypothesis

The union of valid linked employment intervals provides a proxy for relevant
experience. Overlapping jobs are not added twice. Ongoing jobs stop at the explicit
evaluation date; missing, future or invalid dates contribute no duration. Completed
job intervals use elapsed days (end exclusive), converted at 365.2425 days/year,
with approximately one day of tolerance at annual boundaries.

Meeting the minimum: MATCH; a positive demonstrated duration within approximately
one year of it: PARTIAL. Both have MEDIUM confidence because a skill link does not
measure full-time technology usage. Greater shortfalls remain UNKNOWN because the
career history is not declared exhaustive. Missing dates or links also remain
UNKNOWN. The rationale explicitly describes this approximation. This is a product
calibration hypothesis, not a factual measurement of time spent using a skill.

### MISSING versus UNKNOWN

Missing profile evidence never proves inability. Skills, career history and
education have no completeness declaration. Their absent matches remain UNKNOWN.

`CandidateProfile.certificationsComplete` is an explicit candidate declaration,
editable as a checkbox in the profile. It defaults to false for existing records
and omitted API values, via additive Liquibase migration 0005. A complete list may
be empty. Only an unambiguous certification absent from this declared complete
list yields MISSING. The checkbox must be kept current when editing the profile.

Another supported MISSING case is an explicitly declared CECRL language level below
the requested minimum. UNKNOWN is displayed as “À vérifier”, never “Échec”.

### Blocker safety

Non-blocker and non-REQUIRED requirements always yield NONE. MATCH and
NOT_APPLICABLE do not block. Other blocker results yield POSSIBLE_BLOCK unless all
of the following hold: MISSING, HIGH assessment confidence, EXPLICIT requirement,
CONFIRMED/CORRECTED review, and blockerCondition equals the canonical label after
text normalization. That last narrow rule avoids interpreting arbitrary conditional
prose. Only then can this requirement yield BLOCK. Ambiguous conditions and
UNREVIEWED offers (including bypassed offers) always require verification.

## Validation cases

Only fictional fixtures are used. Focused tests were written first and failed
before the engine existed, then passed with implementation.

| Fictional case | Expected inspection |
| --- | --- |
| Java declared and linked to a developer role | MATCH / EXACT; skill and experience UUIDs explain why. |
| React declared, Angular requested | UNKNOWN / NONE; no invented technology transfer. |
| Two years of Java-linked posts, three requested | PARTIAL; overlap-safe proxy and moderate confidence are visible. |
| No dated linked experience | UNKNOWN, not MISSING. |
| Credential Alpha present | MATCH with its certification UUID. |
| Credential Alpha absent, list declared complete | MISSING; completeness explains absence. |
| Same absent certification, unreviewed blocker | POSSIBLE_BLOCK, never BLOCK. |

Tests also cover language comparisons, unsafe substrings, repeated evidence,
conditional blockers, exact education and recomputation after profile updates.
The API integration tests use a fake extractor and verify no extractor interaction
during assessments. PostgreSQL validation runs against a dedicated test database,
never the development profile database.

Manual UI inspection on 2026-09-15 used six fictional saved requirements and a
fictional profile in an isolated PostgreSQL database: Java MATCH with experience
evidence, Angular UNKNOWN with only React available, two years versus three PARTIAL,
work authorization UNKNOWN, Credential Alpha MATCH and absent Credential Beta
MISSING with POSSIBLE_BLOCK while unreviewed. Layout was inspected at mobile width
and 1440px, in light and dark themes. No real provider was configured or called.
