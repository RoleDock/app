# RoleDock Agent Instructions

## Project

RoleDock is a job application workspace.

Its main purpose is to help candidates:
- analyze job offers against a structured candidate profile;
- understand matching strengths and gaps;
- identify relevant CV keywords;
- generate truthful application content;
- track applications;
- retrieve a concise recruiter brief.

## Current scope

Work only on MVP v0.1 unless explicitly instructed otherwise.

Out of scope:
- CV parsing/import;
- job board scraping;
- LinkedIn/Indeed integration;
- Gmail integration;
- autonomous agents;
- automatic follow-ups;
- mobile application.

## Architecture

Monorepository:

- `backend/`: Java / Spring Boot REST API
- `web/`: Angular application
- `docs/`: architecture and product documentation

Business rules belong in the backend.

The frontend must not duplicate domain rules such as compatibility scoring.

## AI principles

AI is a tool used by the product, not the source of truth.

Never invent:
- candidate skills;
- candidate experience;
- education;
- job requirements not present in the offer.

Unknown information must remain unknown.

Compatibility scores must be deterministic and explainable.
The LLM must not directly choose the final score.

## Development principles

Prefer simple solutions suitable for the current MVP.

Do not introduce abstractions for hypothetical future requirements.

Do not add dependencies without a concrete reason.

Do not implement features outside the requested task.

Keep changes small and reviewable.

## Quality

Every business rule should be testable.

Run the relevant tests before considering work complete.

Do not commit secrets or personal candidate/application data.

## Documentation

When an architectural decision materially changes the project,
update the relevant documentation.

Do not update documentation merely to repeat implementation details.