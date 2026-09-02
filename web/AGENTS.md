# RoleDock Web Agent Instructions

These instructions extend the repository-wide guidance in `/AGENTS.md` for work
under `web/`.

## Scope

The Angular application is responsible for:

- displaying data;
- collecting and validating user input;
- user navigation;
- presentation state;
- calling the RoleDock REST API.

Business rules such as compatibility scoring belong to the backend.

## Technical stack

- Angular 21.x
- standalone components
- TypeScript strict mode
- Angular Router
- SCSS
- Angular HttpClient

Do not add NgRx, Redux-like state libraries, UI component frameworks, CSS
frameworks, or additional data-fetching libraries unless a concrete requirement
justifies them.

## Angular practices

- Prefer standalone Angular APIs.
- Prefer modern Angular 21 features when they simplify the implementation.
- Keep components focused.
- Put HTTP communication in dedicated services.
- Do not make HTTP calls directly in presentation components when a service
  provides a clearer boundary.
- Do not wrap Angular APIs without a concrete reason.
- Avoid giant shared modules and generic `utils` dumping grounds.

## State

Use the simplest state mechanism appropriate to the feature. Keep local
component state local. Do not introduce global state management for data that
belongs to one screen.

## Forms

Choose the Angular forms approach that fits the form's complexity. Validate
input in the frontend for user experience and in the backend for correctness.
Frontend validation is never a security or business-rule boundary.

## UX

RoleDock should remain usable as a productivity application. Prioritize clarity,
quick access to information, readable layouts, responsive behavior, reasonable
keyboard usability, and accessibility. Avoid visual complexity before the
functionality is validated.

## API

- Prefer relative `/api/...` URLs.
- Continue using the Angular development proxy.
- Do not hardcode production backend URLs in components.
- Type API responses.
- For real features, explicitly handle loading, empty, and failure states.

## Tests

Test meaningful UI behavior. Avoid tests that only assert Angular implementation
details. Run relevant frontend tests and build checks before declaring web work
complete.

## General

- Do not invent UI features outside the requested task.
- Avoid speculative reusable components.
- Prefer feature-local components until reuse is demonstrated.
- Never store secrets in the frontend.
