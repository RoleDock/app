# RoleDock — AI Strategy

## Principle

Use AI for fuzzy language tasks. Use deterministic application code for rules
where deterministic behavior is possible.

## Planned MVP AI responsibilities

- job-offer structured extraction;
- classification of job requirements;
- offer summarization;
- CV keyword suggestions;
- application-message generation;
- cover-letter generation;
- recruiter-brief generation.

## Not delegated to AI

- the authoritative candidate profile;
- the final compatibility score;
- persistence rules;
- application status rules;
- authorization or security decisions.

## Structured extraction

Prefer explicit structured schemas. Unknown fields must support null or unknown
values. Backend validation is mandatory before extracted data is persisted or
used.

## Hallucination protection

Generated text may treat facts as factual claims only when they are present in:

- the candidate profile;
- the stored job offer.

AI must not fill gaps with invented candidate or offer facts.

## Prompt injection

Job offers are untrusted external text. Instructions contained inside a pasted
job offer must be treated as job-offer content, not as instructions controlling
RoleDock or the LLM workflow.

## Observability

Later versions should make it possible to identify:

- the model and provider used;
- the prompt or prompt-template version;
- failures;
- validation errors;
- approximate usage or cost where useful.

Do not build an elaborate observability platform in the MVP.
