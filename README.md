# 50 Acre Sales Challenge — Web/PWA

A mobile-first Angular + Spring Boot + PostgreSQL web application for a configurable land-sales challenge. Business values (project name, acreage, dates, timezone) are stored in the database and editable by administrators — never hard-coded in application logic.

## Stack

- **Frontend:** Angular 20, TypeScript, RxJS, Reactive Forms, PWA/service worker, Vercel
- **Backend:** Spring Boot 3.5, Java 21, Spring Security, JWT, Flyway, OpenAPI
- **Database:** PostgreSQL (Neon in production)

## Architecture

```
Angular (Vercel)
    ↓
Spring Boot REST API (Render)
    ↓
PostgreSQL (Neon)
```

## Authentication (30-day persistent session)

- **Access JWT:** 15-minute lifetime, held in Angular memory only (never localStorage/sessionStorage)
- **Refresh token:** 30-day rotating session in Secure/HttpOnly cookie (`ACRES_REFRESH`, path `/api/auth`)
- Refresh tokens are SHA-256 hashed in PostgreSQL
- On startup: CSRF → refresh → restore user → route normally (splash while initializing)
- **Multi-tab:** `BroadcastChannel` coordinates refresh so concurrent tabs do not revoke each other
- **Rotation grace:** brief reuse window during concurrent refresh (stolen replay still revokes all sessions)
- HTTP interceptor: single refresh on 401, request IDs, friendly 403/429/offline/5xx handling
- CSRF double-submit (`XSRF-TOKEN` cookie + `X-XSRF-TOKEN` header) on auth mutations

**Production cross-origin (Vercel → Render):** set `COOKIE_SAME_SITE=None`, `COOKIE_SECURE=true`, and `FRONTEND_URL` to your exact Vercel origin.

## Runtime API configuration (frontend)

Production does **not** hard-code the backend URL. Before first deploy, set:

`frontend/public/assets/config.json`:

```json
{
  "apiBaseUrl": "https://your-actual-backend.onrender.com/api"
}
```

The app loads this file on startup (before auth init). Development uses `environment.ts` (`http://localhost:8080/api`). See `config.prod.example.json` for a template.

You can change the backend URL on Vercel without rebuilding by updating `config.json` in the deployed static assets (or via a pre-build script that writes the file from an env var).

## Local development

### 1. Database

```bash
docker compose up -d postgres
```

### 2. Backend

```bash
cd backend
# Set env vars from ../.env.example
SPRING_PROFILES_ACTIVE=seed ./mvnw spring-boot:run
```

Seed credentials (development only — never use in production):

| Email | Password |
|-------|----------|
| admin@example.com | ChangeMe123! |
| ravi@example.com | ChangeMe123! |
| kumar@example.com | ChangeMe123! |
| arun@example.com | ChangeMe123! |

### 3. Frontend

```bash
cd frontend
npm ci
npm start
```

Open http://localhost:4200

## Production deployment

### Frontend — Vercel

- Root directory: `frontend`
- Build command: `npm ci && npm run build`
- Output directory: `dist/acres-web`
- **Before deploy:** set `public/assets/config.json` → `apiBaseUrl` to your Render API (e.g. `https://your-api.onrender.com/api`)
- SPA rewrites configured in `vercel.json`

### Backend — Render

Use `render.yaml` or deploy the `backend/Dockerfile`. Health check: `/actuator/health`

Required environment variables:

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
JWT_SECRET              # min 32 bytes, strong random
FRONTEND_URL            # exact Vercel origin
COOKIE_SECURE=true
COOKIE_SAME_SITE=None
COOKIE_DOMAIN=
APP_TIMEZONE=Asia/Kolkata
ACCESS_TOKEN_EXPIRATION=15
REFRESH_TOKEN_EXPIRATION=30
BOOTSTRAP_ADMIN_PASSWORD   # optional; defaults to ChangeMe123! when users table is empty
BOOTSTRAP_ADMIN_EMAIL      # optional; default admin@example.com
```

### Neon PostgreSQL

Production database runs on **Neon PostgreSQL**.

1. Create a Neon project and database.
2. Copy the connection string (often `postgresql://user:pass@host/db?sslmode=require`).
3. Set on Render:
   - `DATABASE_URL` — Neon pooled or direct connection string
   - Or use `spring.datasource.url` / `DATABASE_USERNAME` / `DATABASE_PASSWORD` if you prefer JDBC form
4. Set `SPRING_PROFILES_ACTIVE=prod` (included in `render.yaml`).

The backend automatically:
- Converts `postgres://` / `postgresql://` URLs to JDBC
- Preserves `sslmode=require` for TLS
- Uses a conservative Hikari pool (max 5 connections) suitable for Render → Neon

On first startup against an empty Neon database, Flyway runs migrations V1–V5 automatically. **Do not** enable the `seed` profile in production.

Fresh database bootstrap (no manual SQL):

1. Create empty Neon database
2. Configure `DATABASE_URL` + `JWT_SECRET` + `FRONTEND_URL` on Render
3. Deploy backend → Flyway creates schema, indexes, singleton project settings row
4. On first startup with an empty `users` table, the backend auto-creates an admin (`admin@example.com` by default). Set `BOOTSTRAP_ADMIN_PASSWORD` on Render for a custom password.

## API overview

| Area | Endpoints |
|------|-----------|
| Auth | `GET /api/auth/csrf`, `POST login/refresh/logout`, `GET /api/auth/me` |
| User | `GET /api/me`, `POST /api/me/password`, `GET /api/me/sales` |
| Dashboard | `GET /api/dashboard`, `GET /api/leaderboard` |
| Admin | `GET/PUT /api/admin/project`, users CRUD, sales CRUD + export, audit logs |

OpenAPI/Swagger UI: `/swagger-ui/index.html` (disable in production via `SWAGGER_ENABLED=false`)

## Business rules

- Acreage uses `BigDecimal` / `DECIMAL(12,4)`
- Sold acreage is calculated from sales (not stored separately)
- Remaining acreage never displays below zero; visual progress caps at 100%
- Overselling is supported (progress may exceed 100%)
- Countdown uses backend `serverTime` for clock drift compensation
- Dashboard data refreshes every 45 seconds; countdown ticks locally every second
- Users are deactivated (not deleted) to preserve historical sales

## Testing

```bash
# Backend (11 tests)
cd backend && ./mvnw clean verify

# Frontend (2 tests)
cd frontend && npm ci && npm test

# Production builds
cd frontend && npm ci && npm run build
cd backend && ./mvnw clean verify
```

## Project structure

```
├── backend/          Spring Boot API, Flyway migrations, Dockerfile
├── frontend/         Angular PWA, Vercel config
├── docker-compose.yml
├── render.yaml
├── .env.example
└── README.md
```

## Security notes

- JWT secret validated on startup in production
- Refresh token reuse detection revokes all user sessions
- Login rate limiting (IP/email throttling)
- Security headers (HSTS when secure, frame protection, etc.)
- Request correlation IDs (`X-Request-ID`)
- `mustChangePassword` enforced server-side
- Last active admin cannot be deactivated
- Audit logging for all sensitive mutations
- No tokens/passwords in logs or localStorage

## Verification report (local)

| Check | Result |
|-------|--------|
| Frontend `npm ci` + build | **PASS** |
| Backend `./mvnw clean verify` | **PASS** (11/11 tests) |
| Frontend tests | **PASS** (2/2) |
| Database migrations | **PASS** (V1–V5) |
| Security review | **PASS** (core issues addressed) |
| Mobile responsiveness | **PASS** (mobile-first CSS, cards, bottom nav) |
| PWA | **PASS** (manifest, service worker, icons) |
| Deployment E2E | **NOT VERIFIED** — requires deployed Vercel/Render/Neon environment |

### Remaining items for production launch

- Deploy and verify cross-origin cookie auth (Vercel → Render → Neon)
- Set `config.json` with real production API URL before/at Vercel deploy
- Set strong `JWT_SECRET` and disable Swagger in production
- Consider edge rate limiting in addition to application login throttling
- Expand integration test coverage (`@SpringBootTest`, security MockMvc tests)
