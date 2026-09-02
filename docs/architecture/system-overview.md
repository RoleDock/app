# RoleDock — System Overview

## System context

```mermaid
flowchart LR
    User[Candidate] --> Web[Angular Web App]
    Web -->|REST /api| API[Spring Boot API]
    API --> DB[(PostgreSQL)]
    API --> LLM[LLM Provider]

    Mobile[Future Mobile Client] -. future .-> API
```

The Angular-to-Spring Boot-to-PostgreSQL path is the current technical
foundation. The LLM provider is a planned MVP integration boundary and is not
part of the bootstrap yet. The mobile client is a possible future consumer, not
an MVP component.

## Current components

### Angular web application

Owns presentation and user interaction. It collects user input, presents
results, manages screen-level state, and calls the backend through the REST API.

### Spring Boot backend

Owns the REST API, domain rules, persistence orchestration, compatibility
scoring, and AI integration. AI-provider integration will be added only when an
MVP feature requires it.

### PostgreSQL

Stores persistent RoleDock data. Liquibase versions every schema change.

### LLM provider

Will be used only for tasks where probabilistic language processing adds value.
It is an external assistant to the workflow, not a system of record or rule
engine.

## Architectural boundaries

- The UI does not own scoring rules.
- The LLM does not own candidate truth.
- The LLM does not own the final compatibility score.
- PostgreSQL schema changes are versioned by Liquibase.
- The backend API should remain client-agnostic so a future mobile application
  can consume it.

## Current deployment status

RoleDock supports local development only. No hypothetical cloud architecture is
currently a decided part of the system.
