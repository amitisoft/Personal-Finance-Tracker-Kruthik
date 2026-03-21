# Personal Finance Tracker

Full-stack personal finance tracker built with React + TypeScript + Vite + Tailwind on the frontend and Spring Boot + JPA + PostgreSQL on the backend.

## Structure

- `backend`: Spring Boot API with JWT auth, layered architecture, PostgreSQL, recurring scheduler, seeding, reports.
- `frontend`: Vite React app with Context API auth state, Axios services, Recharts dashboards, responsive pages.

## Backend setup

1. Create PostgreSQL database `FinanceTracker`.
2. Confirm credentials in [application.yml](backend/src/main/resources/application.yml):
   - username: `postgres`
   - password: `Kru@123`
   - port: `5432`
3. Run the backend from [pom.xml](backend/pom.xml) with Maven or your IDE.
4. Swagger UI will be available at `http://localhost:8080/swagger-ui/index.html`.

Demo user seeded on startup:
- email: `demo@financetracker.app`
- password: `DemoPass123`

## Frontend setup

1. Open `frontend`.
2. Install dependencies with `npm install`.
3. Start with `npm run dev`.
4. The Vite dev server proxies `/api` to `http://localhost:8080`.

## Key features

- JWT auth with access + refresh tokens
- Accounts, categories, transactions, budgets, goals, recurring items
- Dashboard summary, recent transactions, budget status, recurring preview
- Reports with filters, charts, and CSV export
- User-scoped APIs and UUID primary keys
- Seeded sample data and PostgreSQL schema reference in [schema.sql](backend/src/main/resources/schema.sql)

## Notes

- `forgot-password` and `reset-password` are implemented as a V1 demo-safe flow using the reset token `DEMO-RESET-TOKEN`.
- Scheduler runs hourly and creates due recurring transactions automatically.
- Spring JPA is configured with `ddl-auto: update`; the included schema file is provided as a reference and manual bootstrap option.
