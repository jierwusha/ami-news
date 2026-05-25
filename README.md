# AmiNews

AmiNews is a full-stack news aggregation platform with AI-assisted capabilities.

## Repository structure

- `aminews-frontend`: Vue 3 + TypeScript frontend (Vite)
- `aminews-backend`: Spring Boot backend (Java 17, Maven)
- `docker-compose.yml`: local full-stack deployment (frontend, backend, MySQL, Redis, Elasticsearch, RSSHub, Nginx)

## Prerequisites

- Node.js 20+ and npm
- Java 17
- Docker and Docker Compose (for full stack startup)

## Quick start (Docker)

From the repository root:

```bash
docker compose up --build
```

Then open:

- `http://localhost` (Nginx entrypoint)

## Local development

### Frontend

```bash
cd aminews-frontend
npm install
npm run dev
```

Default dev URL: `http://localhost:5173`

### Backend

```bash
cd aminews-backend
chmod +x mvnw
./mvnw spring-boot:run
```

Backend uses Spring profiles/config under:

- `aminews-backend/src/main/resources/application.yaml`
- `aminews-backend/src/main/resources/application-prod.yaml`

## Validation commands

### Frontend

```bash
cd aminews-frontend
npm run lint
npm run build
npm run test:unit
```

### Backend

```bash
cd aminews-backend
./mvnw test
```

## Notes

- Reverse proxy settings are in `nginx.conf`.
- Full service wiring is defined in `docker-compose.yml`.
- Do not commit real credentials or API keys in config files.
