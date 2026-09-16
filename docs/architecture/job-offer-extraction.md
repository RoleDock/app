# Job-offer extraction spike

## Responsibility and boundary

Extraction answers only "What does this job advertisement say?" The backend exposes
`JobOfferExtractor.extract(String)` returning `JobOfferExtraction` in
`io.github.roledock.joboffer.extraction`. One OpenAI adapter sits behind that interface.
No provider SDK types cross the interface. The extractor has no profile access,
persistence, candidate assessment, matching, score or application workflow.
The product layer now uses it to preview and save draft offers (see below).

The experimental `POST /api/job-offers/extract` takes `{"text":"..."}` and returns
the validated extraction. Blank/null/missing text, malformed JSON and text longer
than 50,000 Java UTF-16 code units return 400 with the shared ApiError contract.
Missing provider configuration returns 503; provider errors, refusal, incomplete
output or failed output validation return 502. Errors do not contain upstream
bodies, credentials or advertisement content. There are no automatic retries.

## Contract

The machine-readable contract is
[`schema.json`](../../backend/src/main/resources/job-offer-extraction/schema.json).
All properties must be present, including nullable ones; unknown properties are
forbidden at every object level. Collections are non-null, may be empty and have
no null elements.

| Object | Fields |
| --- | --- |
| Extraction | company?, position?, location, workArrangement, contractType, sourceLanguage?, summary?, missions: string[], requirements: Requirement[] |
| Location | city?, region?, country? |
| WorkArrangement | type, remoteArea?, onSiteDaysPerWeek?: integer 0–7 |
| Requirement | rawText, canonicalLabel, category, requirementKind, centrality, explicitness, hardBlockerCandidate: boolean, blockerCondition?, constraint?, extractionConfidence |
| Constraint | operator, value: nonblank string, unit?: string |

Question marks mean JSON null is allowed, not that the property may be omitted.
Missions, rawText and canonicalLabel must be nonblank. Requirement rawText is an
exact contiguous source quotation; splitting requirements may reuse a quotation.
Canonical labels normalize obvious aliases (JS → JavaScript, Postgres → PostgreSQL,
SpringBoot → Spring Boot), never neighboring technologies (Angular ≠ React).

Constraint deliberately uses a string value: e.g.
`{"operator":"AT_LEAST","value":"3","unit":"years"}` or
`{"operator":"RANGE","value":"3-5","unit":"years"}`.
It is a description, not an executable expression or universal constraint language.
Unit is null when unspecified. sourceLanguage uses a language code such as fr/en,
or null when unclear; it is not a requirement to speak that language.
Summary, missions and labels retain the source language, except technology names.

## Enum meanings

| Enum | Values and meaning |
| --- | --- |
| WorkArrangementType | ONSITE: onsite; HYBRID: explicit mix; REMOTE: remote; UNKNOWN: absent or ambiguous |
| ContractType | PERMANENT: indefinite employment; FIXED_TERM: fixed duration; FREELANCE: independent work; INTERNSHIP: internship; APPRENTICESHIP: apprenticeship; TEMPORARY: temporary/agency work; OTHER: stated type outside these categories; UNKNOWN: undetermined |
| RequirementCategory | TECH_SKILL: technology; EXPERIENCE: prior practice/duration; DOMAIN_KNOWLEDGE: industry knowledge; TITLE_LEVEL: role/seniority; EDUCATION: studies; CERTIFICATION: license/credential; LOCATION: physical location; WORK_AUTHORIZATION: legal permission; LANGUAGE: language proficiency; AVAILABILITY: timing; CONTRACT: engagement condition; SOFT_SKILL: interpersonal ability; OTHER: another subject |
| RequirementKind | REQUIRED: mandatory; PREFERRED: bonus; CONTEXTUAL: environment without demanded proficiency; UNKNOWN: unclear |
| Centrality | CORE: central to duties; SUPPORTING: assists duties; INCIDENTAL: peripheral; UNKNOWN: unclear |
| Explicitness | EXPLICIT: directly stated; INFERRED: cautious interpretation backed by the quotation, not a new fact |
| ExtractionConfidence | HIGH, MEDIUM, LOW: confidence in the interpretation, never suitability or a calibrated probability |
| ConstraintOperator | AT_LEAST: lower bound; AT_MOST: upper bound; EQUALS: stated equality; RANGE: interval; OTHER: another stated relation |

Centrality and requirementKind are independent. A long stack description does not
make every technology mandatory. Previous conceptual documents use NICE_TO_HAVE
and CONTEXT; this extraction contract calls these PREFERRED and CONTEXTUAL.
No matching-stage mapping is implemented.

## Unknowns and blockers

Missing scalar facts stay null. Enums with UNKNOWN use it for missing/ambiguous
information. Category OTHER covers an unclassified subject; it must not manufacture
a requirement. Location/workArrangement containers remain present with null details.
Empty arrays mean no extracted items. Never infer a country from a city, a schedule
from "remote-friendly", or a contract type from a title.

hardBlockerCandidate flags an advertisement's explicit potentially disqualifying
condition; it never says someone fails. Default false. Typical cases are mandatory
work authorization, physical presence, licenses, and non-negotiable language levels.
A technology being required does not automatically make it a blocker.

The backend requires a true flag to have REQUIRED, EXPLICIT and a nonblank
blockerCondition. A false flag requires blockerCondition null. These checks enforce
internal consistency; deciding whether wording really is non-negotiable remains
an extraction judgment to review in the dataset.

## Untrusted input and validation

The advertisement is sent only as user-message data. Stable extraction instructions
are supplied separately. Embedded instructions must not override them, become
requirements, or be echoed in summaries. No tools, link fetching or candidate data
are provided to the model.

The OpenAI adapter uses Spring RestClient and the existing Jackson/Bean Validation
dependencies; no SDK or additional dependency is needed. Responses API requests
set `text.format.type=json_schema`, `strict=true`, and `store=false`.
See [OpenAI Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs).

The adapter accepts only a completed response with one output_text and no refusal.
Jackson parses the API envelope and deserializes the output into Java records;
there is no markdown stripping, substring-based JSON recovery or free-form fallback.
A private strict mapper rejects missing/extra properties, duplicate keys, trailing
JSON, unknown/numeric enums, scalar coercions, and fractional integer values.
Recursive Bean Validation checks required values, nested objects, list elements,
blank labels and day bounds. ExtractionValidator verifies source quotations and
blocker consistency. The endpoint also validates results from alternative/fake
extractors before returning them. Schema enum consistency is checked by tests.

This proves contract handling, not semantic accuracy. Schema-constrained output
cannot guarantee facts, correct canonicalization or immunity to prompt injection.
Exact quotations help traceability but do not prove that an interpretation is right.
Product drafts may be saved without human review and are explicitly UNREVIEWED.
Saving is not acceptance of the proposal as authoritative business truth.

## Manual provider evaluation (explicit opt-in)

Normal `./mvnw test` / `.\\mvnw.cmd test` and CI never call a real LLM. Adapter
tests bind a MockRestServiceServer; endpoint tests substitute a fake extractor.
No API key is required to build, test or start the application. There is no
live-provider Maven test hook that CI could accidentally enable.

To call the provider intentionally:

1. Start PostgreSQL from the repository root: `docker compose up -d postgres`.
2. In the backend terminal, export OPENAI_API_KEY securely and set
   JOB_OFFER_EXTRACTION_MODEL to an available Responses model supporting strict
   JSON Schema. No model is selected implicitly. The backend does not load .env
   automatically; .env.example only documents environment variables.
3. Start the backend with `.\\mvnw.cmd spring-boot:run` (or `./mvnw spring-boot:run`).
4. From another PowerShell terminal in backend, submit a synthetic example:

```powershell
$text = Get-Content -Raw -Encoding utf8 'src/test/resources/job-offers/01-java-spring-angular.txt'
$body = @{ text = $text } | ConvertTo-Json
$result = Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/job-offers/extract' -ContentType 'application/json; charset=utf-8' -Body ([System.Text.Encoding]::UTF8.GetBytes($body))
$result | ConvertTo-Json -Depth 20
```

For a real offer, point Get-Content at a local UTF-8 advertisement file. Only submit
content appropriate to send to the provider. Each manual request is a real API call.
Connection timeout is 10 seconds and read timeout 60 seconds; output is capped at
12,000 tokens. Long outputs may fail as incomplete; no partial extraction is returned.

Follow the [dataset evaluation instructions](../../backend/src/test/resources/job-offers/README.md).
No live-provider quality results are claimed by this implementation.

## Decisions still requiring evidence

The extractor contract guarantees validated results (or an ExtractionException), so
the controller does not validate the same result twice. Provider HTTP failures retain
only the numeric upstream status for internal logging. Each configured extraction
attempt logs a generated attempt identifier, the configured model identifier and
SHA-256 fingerprints of the loaded prompt and schema with CRLF normalized to LF.
These identify the loaded resources, not a retrospectively attested provider model
snapshot. No advertisement text or provider response body is included.

Extraction failures keep the existing public error body and HTTP status. The controller
logs one fixed internal reason code: NOT_CONFIGURED, PROVIDER_HTTP, TIMEOUT,
TRANSPORT, PROVIDER_INCOMPLETE, PROVIDER_REFUSAL, PROVIDER_FORMAT, INVALID_JSON,
INVALID_FIELDS, QUOTE_MISMATCH, BLOCKER_INCONSISTENT or UNEXPECTED. Provider bodies,
input text, exception causes and credentials are not logged or retained in the
public extraction exception. Inspect this code in the backend terminal after a failed
request. A QUOTE_MISMATCH identifies an inexact quotation, not which character caused
it; exact matching still distinguishes ordinary spaces from non-breaking spaces.
No automatic retry or prompt change is introduced.

Prompt snapshots and the candidate comparison protocol are documented in the
[prompt history](../prompts/job-offer-extraction/README.md). The v2 proposal is not
automatically activated: the backend still reads the runtime instructions file.

- Evaluate repeatability, alias fidelity, omission rate and false hard-blocker flags
  on the eight cases before building on this extraction.
- Mixed-language advertisements currently have one nullable sourceLanguage.
- Multiple offices, alternative contracts and contradictory schedules cannot be
  represented in full by this deliberately small contract; use unknown when needed
  and retain relevant wording in traceable requirements.
- RANGE values remain descriptive text; structured intervals can be considered only
  when a later consumer has a concrete need.
- Choosing a provider model and acceptable quality/latency thresholds requires
  manual measurements, not a default embedded in business code.

## Persistent draft product slice

The baseline stays OpenAI Responses with structured output and runtime prompt v4.
`POST /api/job-offers/extract` remains unchanged and stateless.

The product API separates analysis from saving:

- `POST /api/job-offers/analyze`: accepts `originalText` (nonblank, at most
  50,000 UTF-16 code units) and nullable `sourceUrl` (HTTP(S), at most 2,000
  characters, no embedded credentials). URLs are retained as references, never
  fetched. Returns `analysisId`, `analyzedAt`, and validated `extraction`.
- `POST /api/job-offers`: accepts only `analysisId` and saves the server-held
  proposal transactionally. Returns 201, a Location header and an explicit offer
  DTO containing original text, URL, timestamp, review status and current extraction.
- `GET /api/job-offers/{id}`: retrieves that DTO independently of the analysis
  session; unknown offers return 404.

One pending analysis is kept in the existing servlet HTTP session, using the
session cookie on both requests. It does not create a database row. A successful
new analysis replaces the pending one, including across tabs in that browser.
Session expiration or server restart discards unsaved analysis; Save then returns
409 and the form offers reanalysis without clearing the text. This deliberately
small, single-server MVP design avoids accepting client-edited content as original
LLM provenance. It is not authentication or a distributed analysis store.

The analysis UUID becomes the offer UUID. Saves are serialized within the session;
repeating Save for that pending analysis returns the existing offer. A failed
transaction retains the pending proposal for retry. Parent, missions and
requirements commit together. Failed extraction cannot persist a partial offer.

The Angular page `/job-offers/new` automatically saves a successful analysis and
opens `/job-offers/{id}/review`. It preserves input on extraction/persistence
failures, disables concurrent actions and invalidates the analysis when input
changes. A persistence retry reuses the analysis; a navigation retry reuses the
saved offer without saving or calling the extractor again. Reloading or returning
to the creation page does not automatically submit another analysis. Both the
review and saved-offer pages fetch the persisted offer again on reload.
New drafts show “Analyse automatique — vérification recommandée.”

Original text, initial validated extraction snapshot, current values and review
state are distinct; see [the persistence model](data-model.md).
## Human review of persisted offers

`/job-offers/{id}/review` displays the original advertisement alongside editable
current values. It reuses Candidate Profile layout, cards, controls and feedback
styles. Missions and requirements can be edited, added or removed in order.
Original quotations are read-only; manual requirements explicitly display their
user provenance. Navigation and browser-close warnings protect unsaved changes.
A fixed bottom toolbar keeps global saving and direct section/requirement jumps
available throughout long reviews. Checking a requirement collapses its card and
opens the next one. These session-only progress markers do not persist individual
review statuses; the global save still confirms or corrects the aggregate. Hidden
cards retain their values and form validation.
API failures preserve the draft. Confirmation submits all displayed values, so it
cannot silently discard pending corrections.

`PUT /api/job-offers/{id}/review` accepts one explicit command:

- `{"action":"SAVE","extraction":{...}}`: a complete current extraction, including
  requirement UUIDs and provenance from GET. New requirements use null `id`,
  `USER_ADDED` source and null `rawText`/`extractionConfidence`. The server validates
  enums, required values, blocker/constraint consistency, requirement ownership,
  duplicate IDs and immutable provenance before applying changes. No change sets
  `CONFIRMED`; a structured change sets `CORRECTED`. Reconfirming a corrected
  extraction keeps `CORRECTED`.
- `{"action":"BYPASS"}`: allowed only while `UNREVIEWED`, with no extraction
  payload. It sets `reviewBypassedAt` once and leaves the status `UNREVIEWED`.
  The UI shows a concise inline warning and continues to the saved offer with
  “Analyse automatique — non vérifiée”. Bypass cannot discard pending edits.
  If navigation fails, the review page keeps that unreviewed label and offers a
  navigation retry without another review write. Only SAVE displays validation
  success feedback.

Reviewed labels are “Analyse vérifiée” and “Analyse vérifiée et corrigée”. They
refer to the extraction review, not the advertisement's objective accuracy.
Later review overrides bypass in the UI while retaining its timestamp.

The saved-offer DTO now uses a separate current extraction projection, with
requirement `id` and `source` fields and nullable manual citation/confidence.
The analyze/extract DTOs, strict provider schema and runtime prompt v4 are unchanged.
The service has no extractor dependency. One transaction and a parent-row lock
protect the complete update; removed requirements are deleted, retained UUIDs
survive edits/reordering, and intermediate order changes avoid unique collisions.
Unknown offers return 404, invalid commands 400, and bypass of reviewed data 409.
Original advertisement and initial extraction snapshot are never replaced.

The extraction/review services have no profile access or scoring. The separate
[requirement assessment service](requirement-assessments.md) now performs on-demand
matching without provider calls. Matching must use
`reviewStatus` as the authority: `UNREVIEWED` data may continue with a warning, but
cannot alone justify definitive elimination. A blocker from unreviewed extraction
requires `VERIFY_FIRST`, not `SKIP_CONFIRMED_BLOCKER`, even after explicit bypass.
Individual blocker safety is implemented; global recommendation remains future work.
