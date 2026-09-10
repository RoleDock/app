# Synthetic job-offer evaluation dataset

All eight numbered advertisements are invented for this repository. They contain
no real candidate data or copied third-party advertisements.

Each NN-name.txt has NN-name.expected.md with semantic expectations. These are
human-authored checks, not claims about actual model results. unknown.expected.json
is a complete schema fixture for absent facts, used in automated tests; it is not
a provider-generated answer or the exact expected answer to case 07.

## Record expected and observed outputs

1. Read an advertisement and its expected.md before looking at model output.
2. For a full reference output, create NN-name.expected.json using every field in
   the schema, explicit nulls and UNKNOWN values. Copy rawText exactly. Have a human
   review the interpretation; never promote model output directly to a gold answer.
3. Keep actual runs under the repository's ignored tmp/job-offer-runs directory.
   Record case ID, UTC time, provider/model identifier, prompt/schema version or
   hashes, elapsed time, HTTP result, and raw validated response separately.
   Never record API keys or real personal data.
4. Run every case at least three times with the same settings. Compare semantics,
   not exact wording or requirement ordering. Report each mismatch and the relevant
   quotation. Do not average away invented facts or injection failures.
5. Check required/preferred/contextual distinctions, canonical labels, unknowns,
   traceability, blocker flags, missions, and constraints. Especially verify Angular
   is never React and embedded instructions have no effect.
6. Record reviewed findings in a small table: case, run/model, pass/fail for each
   expectation, omission/invention, blocker false positive, notes. Include failures
   and disagreements, not just successful JSON. Only commit reviewed synthetic
   reference outputs and sanitized findings.

A useful initial acceptance gate is: every response validates; no invented
requirements/candidate facts, technology substitutions, or obeyed injections;
all stated mandatory/preferred distinctions and missing fields are correct.
Centrality/confidence judgments may require human discussion. This is a proposed
manual gate, not a measured guarantee. No actual provider runs are included yet.

See docs/architecture/job-offer-extraction.md for the endpoint and local run commands.
Normal automated tests never send these examples to an LLM.
