# RoleDock — Business Rules

## Candidate profile

The candidate profile is the source of truth about the candidate. AI may select,
summarize, or reformulate facts from the profile, but it must never add facts
absent from that profile.

## Job offer analysis

Requirements extracted from an offer must distinguish:

- `REQUIRED`
- `NICE_TO_HAVE`
- `CONTEXT`

Missing information remains unknown. Unknown information must not be silently
converted into false or inferred facts.

Automatic extraction is a proposal. A candidate may save it as a draft without
review; the product displays “Analyse automatique — vérification recommandée.”
New drafts are `UNREVIEWED`. Explicit review without structured changes sets
`CONFIRMED`; corrections set `CORRECTED`. Later unchanged review preserves an
existing `CORRECTED` status. Review is recommended, never mandatory.

Continuing without review leaves `UNREVIEWED` and records `reviewBypassedAt`.
Without that timestamp the user has not made a review decision; with it the user
has intentionally bypassed review. Later confirmation/correction is shown as
reviewed regardless of the retained timestamp. Manually added requirements have
`USER_ADDED` provenance and no automatic quotation or extraction confidence.

Future matching may use unreviewed data with a warning, but unreviewed extraction
alone must never produce a definitive elimination recommendation. An unreviewed
blocker requires `VERIFY_FIRST`, not `SKIP_CONFIRMED_BLOCKER`. Bypassing does not
relax this rule. No matching or scoring is implemented in this slice.

The original advertisement remains intact, the initial validated extraction is
retained separately, and current structured values can evolve later without
overwriting either source. Saving a proposal does not make it authoritative and
does not perform candidate matching or scoring.

## Compatibility

Each meaningful requirement should be comparable with the candidate profile.
The current match states are:

- `MATCH`
- `PARTIAL`
- `MISSING`
- `NOT_APPLICABLE`

The final score must be deterministic and explainable. An LLM must not directly
output the authoritative final score. A missing nice-to-have must not carry the
same impact as a missing required skill.

## CV keywords

RoleDock may recommend emphasizing a keyword already supported by the candidate
profile. It must not recommend lying by adding a skill the candidate does not
possess.

## Generated content

Messages and cover letters may reformulate candidate facts. They must not
fabricate:

- experience;
- skills;
- education;
- responsibilities;
- certifications.

Generated content remains editable before use.

## Application lifecycle

Initial statuses are:

- `DRAFT`
- `SENT`
- `RECRUITER_CONTACT`
- `INTERVIEW`
- `REJECTED`
- `OFFER_RECEIVED`
- `ABANDONED`

Marking an application as `SENT` records its sending date. Changing a status
must not delete application information.

## Historical context

RoleDock stores the original offer content because the source advertisement may
later disappear. A saved application may contain:

- the original offer;
- the structured analysis;
- the generated message;
- the cover letter;
- the status;
- the sending date.

## Recruiter brief

The recruiter brief must prioritize information usable during a live call:

- company;
- role;
- location;
- major technologies;
- key missions;
- candidate strengths used for the application;
- attention points.

It should remain concise.
