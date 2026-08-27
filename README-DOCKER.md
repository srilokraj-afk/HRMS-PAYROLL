# Payroll Service - Docker Setup

This folder contains the Payroll Spring Boot backend, React frontend, and PostgreSQL database in one Docker Compose project.

## Services

- Frontend: http://localhost:5173
- Backend API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- PostgreSQL: localhost:5432

## Run everything

Open a terminal in this folder and run:

```bash
docker compose up --build
```

Then open:

http://localhost:5173

The first startup builds the backend and frontend images and creates the PostgreSQL container.

## Stop

```bash
docker compose down
```

To stop containers and also remove the PostgreSQL data volume:

```bash
docker compose down -v
```

## Run in background

```bash
docker compose up --build -d
```

## Check containers

```bash
docker compose ps
```

## View logs

```bash
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f postgres
```

## Important

The browser calls the backend through `http://localhost:8080`. Docker container-to-container database communication uses the service name `postgres`, so the backend connects to:

`jdbc:postgresql://postgres:5432/payroll_db`

Do not change that database hostname to `localhost` inside the Docker container.

## Security

Spring Security is enabled with stateless JWT authentication. Obtain a token from `POST /auth/login` and send it as `Authorization: Bearer <token>` on protected API requests.

Roles:
- `ADMIN`: full employee management (GET/POST/PUT/DELETE).
- `USER`: read employee and payroll APIs.
- Swagger/OpenAPI endpoints are public for API documentation.

For Docker/production, credentials are supplied through environment variables. Copy `.env.example` to `.env` and replace every `change-me` value before running.

Security filter flow:
Request -> SecurityFilterChain -> BasicAuthenticationFilter -> UserDetailsService -> Authentication -> Authorization -> Controller.

Example credentials for local `dev` profile only:
- ADMIN: `admin` / `admin123`
- USER: `user` / `user123`
