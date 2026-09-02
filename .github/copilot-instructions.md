# RoleDock Copilot Instructions

RoleDock is a job-application workspace built as a monorepo:

- `backend/`: Java 21 / Spring Boot REST API
- `web/`: Angular 21 application
- `docs/`: product and architecture documentation

Before proposing or reviewing changes:

- follow the closest applicable `AGENTS.md`;
- respect path-specific instructions under `.github/instructions/`;
- keep work inside the requested feature scope;
- prefer simple, MVP-oriented solutions;
- reject speculative infrastructure and abstractions;
- ensure frontend code does not duplicate backend business rules;
- ensure no secret or real personal candidate/application data is committed;
- require tests for meaningful business rules;
- treat AI-generated data as untrusted and validate it;
- never fabricate candidate experience or skills;
- keep final compatibility scores deterministic and explainable.

When reviewing pull requests, prioritize:

1. correctness;
2. business-rule integrity;
3. security and privacy;
4. test coverage of meaningful behavior;
5. maintainability;
6. unnecessary complexity.

Do not request stylistic changes that provide no material improvement.
