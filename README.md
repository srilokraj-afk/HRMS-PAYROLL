# Payroll Management System

A basic real-world payroll management module built with Java, Spring Boot, Maven, PostgreSQL, JPA/Hibernate, React, Docker, Swagger/OpenAPI, JWT authentication, RBAC, validation, global exception handling, logging, dynamic search, sorting, pagination, and unit tests.

## Technology Stack
- Java 17
- Spring Boot 3.5.4
- Maven
- Spring Web / Spring Data JPA / Hibernate
- PostgreSQL 16
- Spring Security + JWT
- Swagger/OpenAPI
- JUnit 5 + Mockito
- React + Vite
- Docker Compose

> **Database note:** This project intentionally uses **PostgreSQL**, not MySQL. PostgreSQL is the project's database and is configured in Docker and Spring profiles.

## Main Entities
### Employee
Stores employee information such as name, email, department, basic salary, allowances, and deductions.

### PayrollRecord
Represents payroll information associated with an employee.

Relationship:
`Employee (1) -> PayrollRecord (many)`

The relationship uses JPA `@OneToMany` / `@ManyToOne`, lazy fetching, cascading, and orphan removal.

## API Flow
`Client -> JWT Authentication -> Security Filter -> Controller -> Service -> Repository -> JPA/Hibernate -> PostgreSQL -> Response`

## Authentication
Login endpoint:
`POST /auth/login`

Example request:
```json
{
  "username": "admin",
  "password": "admin123"
}
```

The response contains a JWT. Send it on protected requests:
`Authorization: Bearer <token>`

Roles:
- `ADMIN`: create, update, and delete employees
- `USER`: authenticated read/payroll access

Credentials are supplied through environment variables. Do not commit real passwords or JWT secrets.

## Employee APIs
| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/auth/login` | Get JWT token |
| GET | `/employees` | Search/filter/sort/paginate employees |
| GET | `/employees/all` | Get all employees |
| GET | `/employees/{id}` | Get employee by ID |
| POST | `/employees` | Create employee (ADMIN) |
| PUT | `/employees/{id}` | Update employee (ADMIN) |
| DELETE | `/employees/{id}` | Delete employee (ADMIN) |
| GET | `/payroll/{employeeId}` | Calculate/view payroll |

## Advanced Search
`GET /employees` supports:
- name
- email
- department
- minBasicSalary
- maxBasicSalary
- salary
- page
- size
- sortBy
- direction

Example:
`GET /employees?department=IT&page=0&size=10&sortBy=name&direction=asc`

## Validation and Errors
Requests use Jakarta Bean Validation. Global error handling is implemented using `@RestControllerAdvice` and custom exceptions. Responses use a consistent API/error structure with HTTP status codes and error IDs where appropriate.

## Swagger
After starting the backend:
`http://localhost:8080/swagger-ui.html`

OpenAPI JSON:
`http://localhost:8080/v3/api-docs`

## Profiles
- `dev`: local development defaults
- `prod`: environment-variable-based configuration

Important environment variables:
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `ADMIN_USERNAME`
- `ADMIN_PASSWORD`
- `USER_USERNAME`
- `USER_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRATION_MINUTES`

## Run Locally
1. Start PostgreSQL and create database `payroll_db`.
2. Configure the environment variables.
3. Run:
```bash
cd backend
mvn spring-boot:run
```
4. Start the frontend:
```bash
cd frontend
npm install
npm run dev
```

## Run with Docker
Copy `.env.example` to `.env`, set a strong `DB_PASSWORD` and `JWT_SECRET`, then run:
```bash
docker compose up -d --build
```

Services:
- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`
- PostgreSQL: `localhost:5432`

Stop:
```bash
docker compose down
```

## Testing
Backend unit tests use JUnit 5 and Mockito:
```bash
cd backend
mvn test
```

Test positive and negative service scenarios before creating a pull request.

## Git Workflow
Use feature branches rather than committing directly to `main`:
```text
main -> feature branch -> implementation -> tests -> commit -> push -> pull request -> review -> merge
```

Keep commits small and meaningful.

## Project Structure
```text
backend/
  src/main/java/com/payroll/application/
    config/
    controller/
    dto/
    exception/
    model/
    repository/
    security/
    service/
    specification/
  src/main/resources/
frontend/
docker-compose.yml
.env.example
README.md
```


## Payroll Enhancements

The updated build includes:
- Payroll history stored in `payroll_records`
- Role-based dashboards for `ADMIN`, `USER/HR`, and `EMPLOYEE`
- Monthly payroll processing for all employees
- Approval workflow: `CALCULATED -> APPROVED -> PAID`
- Payslip PDF generation/download
- Audit log for payroll workflow actions
- Dashboard analytics for department payroll and trends

### Demo roles
- `admin` / `admin123` — full access including Audit Log
- `user` / `user123` — HR/payroll operations
- `employee` / `employee123` — employee portal and own payroll history

For the employee portal, set `EMPLOYEE_USERNAME` to the employee email when you want `/payroll/history/me` to resolve to a specific employee profile.
