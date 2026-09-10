# RoleDock

Analyze. Apply. Track.

RoleDock is a job application workspace. This repository currently contains the
minimal Angular, Spring Boot, and PostgreSQL foundation for local development.

## Status

Early development — MVP v0.1.

## Repository

- `backend/` — Spring Boot REST API
- `web/` — Angular web application
- `docs/` — architecture and product documentation

## Documentation

- [Product vision](docs/product/vision.md)
- [Business rules](docs/product/business-rules.md)
- [System overview](docs/architecture/system-overview.md)
- [Proposed data model](docs/architecture/data-model.md)
- [Compatibility scoring](docs/architecture/scoring.md)
- [AI strategy](docs/architecture/ai-strategy.md)
- [Job-offer extraction spike and manual evaluation](docs/architecture/job-offer-extraction.md)

## Local development

### Prerequisites

- Java 21
- Docker with Docker Compose
- Node.js 20.19+ supported by Angular 21 and npm

### Run the stack

From the repository root, start PostgreSQL:

```powershell
docker compose up -d postgres
```

In a second terminal, start the backend:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

On macOS or Linux, use `./mvnw spring-boot:run` instead. In a third terminal,
start the frontend:

```powershell
cd web
npm install
npm start
```

Open <http://localhost:4200>. The backend health endpoint is available at
<http://localhost:8080/api/health>. Angular proxies `/api` requests to the
backend during local development.

The checked-in defaults match `docker-compose.yml`. To change them, use the
environment variables documented in `.env.example`; never commit a real `.env`.

### Tests

```powershell
cd backend
.\mvnw.cmd test

cd ..\web
npm test -- --watch=false
npm run build
```
