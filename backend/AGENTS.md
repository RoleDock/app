# RoleDock Backend Agent Instructions

These instructions extend the repository-wide guidance in `/AGENTS.md` for work
under `backend/`.

## Scope

The backend owns:

- domain and business rules;
- persistence;
- the REST API;
- compatibility scoring;
- AI-provider integration;
- validation of structured AI output.

The frontend must not duplicate backend business rules.

## Technical stack

- Java 21
- Spring Boot 3.5.x
- Maven
- Spring Data JPA
- PostgreSQL
- Liquibase
- Bean Validation
- JUnit and Spring Boot Test

H2 may be used only for lightweight tests where PostgreSQL-specific behavior is
irrelevant. When PostgreSQL-specific behavior matters, prefer PostgreSQL
integration tests rather than attempting to emulate it in H2. Do not introduce
Testcontainers until a real test requirement justifies it.

## Architecture

Prefer a simple feature- or domain-oriented architecture. Keep domain logic out
of controllers. Controllers should mainly handle HTTP concerns, request
validation, and mapping to or from application/domain operations. Put business
rules in services or domain components that can be tested independently.

Do not introduce without a concrete requirement:

- generic repository abstractions over Spring Data;
- base service classes;
- single-implementation interfaces;
- hexagonal or clean-architecture ceremony;
- microservices;
- event buses;
- CQRS;
- messaging infrastructure.

## REST API

- Use `/api/...` routes.
- Use explicit request and response DTOs when exposing JPA entities would couple
  persistence to the public API.
- Validate backend inputs.
- Use appropriate HTTP status codes.
- Do not expose secrets, internal exception traces, or persistence details.

## Persistence

- Make every schema change through Liquibase.
- Do not rely on Hibernate automatic schema creation for development or
  production schema evolution.
- Prefer database constraints when they protect important invariants.
- Do not add speculative tables or columns for future features.

## AI

LLMs are not trusted data sources. Every structured AI response must follow an
explicit schema, be parsed and validated, support unknown or null values, and
never silently become a candidate fact.

The LLM must never determine the final compatibility score. Never invent
candidate skills, experience, education, languages, or certifications. Never
invent job requirements absent from the source offer.

## Tests

Business rules require tests. Give particular attention to:

- scoring;
- requirement classification;
- application status transitions;
- AI-output validation.

Tests should describe observable behavior rather than implementation details.
Run the relevant Maven tests before declaring backend work complete.

## General

- Prefer readable code and descriptive names over clever code.
- Avoid premature abstractions.
- Do not add dependencies without explaining the concrete need.
- Never commit credentials or personal candidate data.
