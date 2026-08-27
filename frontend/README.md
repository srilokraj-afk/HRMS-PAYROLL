# Payroll React Frontend

React + Vite frontend for the enhanced Spring Boot Payroll backend.

## Backend

Default API base URL:

`http://localhost:8080`

To change it, create a `.env` file:

```env
VITE_API_BASE=http://localhost:8080
```

## Enhanced Employee Search

The Employees screen now uses the backend's single advanced endpoint:

`GET /employees`

Supported query parameters:

- `name`
- `email`
- `department`
- `minBasicSalary`
- `maxBasicSalary`
- `page`
- `size`
- `sortBy`
- `direction`

Example:

`http://localhost:8080/employees?name=Srilok&department=ece&page=0&size=10&sortBy=name&direction=asc`

The frontend also understands the backend's standardized `ApiResponse` wrapper and Spring Data `Page` response (`data.content`, `totalElements`, `totalPages`, `number`, `size`).

## Run

```bash
npm install
npm run dev
```

Then open the Vite URL shown in the terminal.
