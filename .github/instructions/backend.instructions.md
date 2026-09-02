---
applyTo: "backend/**"
---

# Backend instructions

- Follow `/backend/AGENTS.md`.
- Keep business logic out of controllers.
- Require Liquibase for every schema change.
- Verify REST DTO and API boundaries.
- Verify backend input and AI-output validation.
- Ensure compatibility scoring remains deterministic and explainable.
- Flag unvalidated LLM output.
- Flag unnecessary dependencies and abstractions.
