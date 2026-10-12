# VolunteerHub

A full-stack volunteer management system: **React** frontend, **Spring Boot** REST API, **MySQL** database, secured with **JWT authentication** and **role-based access control (RBAC)**.

## 1. Overview and features

**Implemented**
- Registration (Volunteer or Organizer) with validation, BCrypt password hashing and duplicate-email protection
- Login that returns a signed, expiring JWT; the same generic `401` is returned for an unknown email and a wrong password
- Three roles: `ADMIN`, `ORGANIZER`, `VOLUNTEER`, enforced on the backend
- View and update your own profile (name only)
- Admin user management: list users, change role, activate / deactivate / suspend, delete, with last-active-admin protection
- Volunteering **opportunities**: organizers create and manage their own; every logged-in user can read them
- Centralised JSON error handling, Bean Validation, Flyway database migrations, configurable CORS
- Automated tests: backend unit + MockMvc/security + integration tests, frontend component tests

**Not implemented (yet):** volunteers *applying* to opportunities, organizers managing applications, email notifications, password reset, refresh tokens, rate limiting.

## 2. Technology stack

| Layer | Technology |
|---|---|
| Frontend | React 19, React Scripts 5 (Create React App), Jest + React Testing Library |
| Backend | Java 17+, Spring Boot 3.5.x, Spring Web, Spring Data JPA (Hibernate), Spring Security 6, Bean Validation |
| JWT | Spring Security OAuth2 Resource Server (Nimbus JOSE), HS256 |
| Database | MySQL 8+, Flyway migrations (tests use in-memory H2) |
| Backend tests | JUnit 5, Mockito, Spring Boot Test, MockMvc |

> The original code already used `jakarta.persistence`, which requires Spring Boot 3.x, so the Java baseline is 17 (not 11). Bump the Boot version in `backend/pom.xml` to the latest 3.5.x patch when you build.

## 3. Architecture and request flow

```
React (port 3000) --HTTP/JSON + Authorization: Bearer <JWT>--> Spring Boot (port 8080)

Security filter chain:
  CORS -> BearerTokenAuthenticationFilter (verify signature, expiry, issuer)
       -> JwtAuthConverter (load user from DB: must exist and be ACTIVE; role comes from the DB)
       -> URL rules (/api/admin/** = ADMIN) -> @PreAuthorize on controller methods
Controller (thin, DTOs + @Valid) -> Service (business rules, @Transactional) -> Repository (JPA) -> MySQL
Errors: GlobalExceptionHandler -> consistent JSON {timestamp,status,error,message,path,fieldErrors?}
```

Backend packages (`com.volunteerhub`): `controller`, `service`, `repository`, `model`, `dto`, `exception`, `security`, `config`. Schema changes live in `backend/src/main/resources/db/migration` (+ `backend/src/main/java/db/migration` for a Java migration).

## 4. Prerequisites

- JDK 17 or newer, Maven 3.9+
- MySQL 8.0 or newer
- Node.js 18+ and npm

## 5. MySQL database setup

```sql
CREATE DATABASE volunteerhub CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'volunteerhub'@'localhost' IDENTIFIED BY 'choose-a-strong-password';
GRANT ALL PRIVILEGES ON volunteerhub.* TO 'volunteerhub'@'localhost';
```

Tables are created and upgraded automatically by Flyway on startup (Hibernate `ddl-auto` is `none`).

### Upgrading an existing database (from the plaintext-password version)
Point the new backend at your existing `volunteerhub` database. Flyway adopts it as version 1, then:
1. normalises e-mails (trim + lower-case) and fixes `NULL` names;
2. converts roles to `VOLUNTEER` / `ORGANIZER`. **Anything else, including a legacy "Admin", becomes `VOLUNTEER`**, because the old API let anyone self-register as any role. Use the admin bootstrap below to create the real admin;
3. adds `status`, `created_at` and a **unique index on email**;
4. hashes every legacy plaintext password with BCrypt (existing users can keep logging in with the same password).

If two legacy rows share an e-mail, step 3 fails and the app refuses to start. Fix the duplicates manually and start again. **Back up the database before the first run.**

## 6. Environment variables

Spring Boot reads real environment variables (it does not load `.env` files itself). See `.env.example`.

| Variable | Required | Purpose |
|---|---|---|
| `JWT_SECRET` | **yes** | HMAC signing secret, at least 32 characters. The app refuses to start without it. Generate: `openssl rand -base64 48` |
| `JWT_EXPIRATION` | no (`1h`) | Token lifetime, e.g. `30m`, `8h` |
| `DB_PASSWORD` | **yes** | MySQL password |
| `DB_URL` / `DB_USERNAME` | no | Defaults: `jdbc:mysql://localhost:3306/volunteerhub` / `volunteerhub` |
| `CORS_ALLOWED_ORIGINS` | no | Comma-separated origins, default `http://localhost:3000`. Set your real frontend URL in production |
| `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD`, `BOOTSTRAP_ADMIN_NAME` | no | First-admin bootstrap (section 13) |

Frontend: `REACT_APP_API_URL` (default `http://localhost:8080`), see `frontend/.env.example`.

## 7. Run the backend

```bash
cd backend
export JWT_SECRET="$(openssl rand -base64 48)"   # keep it stable between restarts or all tokens become invalid
export DB_PASSWORD='your-mysql-password'
mvn spring-boot:run          # http://localhost:8080
```
Windows PowerShell: `$env:JWT_SECRET = "..."; $env:DB_PASSWORD = "..."`.

## 8. Run the frontend

```bash
cd frontend
npm install
npm start                    # http://localhost:3000
```

## 9. API endpoints

All JSON. Protected endpoints need `Authorization: Bearer <token>`.

| Method | Path | Auth | Roles | Purpose |
|---|---|---|---|---|
| POST | `/api/auth/register` | No | public | Create a Volunteer or Organizer account (`201`, `400`, `403` if role=ADMIN, `409` duplicate email) |
| POST | `/api/auth/login` | No | public | Exchange email + password for a JWT (`200`, `401`) |
| GET | `/api/users/me` | Yes | any | Current user's safe profile |
| PUT | `/api/users/me` | Yes | any | Update own name (role/status/email/id in the body are ignored) |
| GET | `/api/admin/users` | Yes | ADMIN | List all users |
| GET | `/api/admin/users/{id}` | Yes | ADMIN | Get one user (`404` if missing) |
| PATCH | `/api/admin/users/{id}/role` | Yes | ADMIN | Change role (`409` if it would remove the last active admin) |
| PATCH | `/api/admin/users/{id}/status` | Yes | ADMIN | Set `ACTIVE` / `DEACTIVATED` / `SUSPENDED` (same last-admin rule) |
| DELETE | `/api/admin/users/{id}` | Yes | ADMIN | Delete user (`204`; same last-admin rule) |
| GET | `/api/opportunities` | Yes | any | List opportunities |
| GET | `/api/opportunities/{id}` | Yes | any | Get one opportunity |
| POST | `/api/opportunities` | Yes | ORGANIZER | Create (owner = caller) |
| PUT | `/api/opportunities/{id}` | Yes | ORGANIZER (owner only) | Update own opportunity |
| DELETE | `/api/opportunities/{id}` | Yes | ORGANIZER (owner) or ADMIN | Delete |

Status codes: `401` missing/invalid/expired token or failed login, `403` authenticated but not allowed, `404` unknown resource, `409` conflict, `400` invalid input, `500` unexpected (generic message, details only in server logs).

**Register**
```http
POST /api/auth/register
{ "name": "Jane Doe", "email": "jane@example.com", "password": "Sup3rSecret!", "role": "VOLUNTEER" }
```
```json
201 { "id": 7, "name": "Jane Doe", "email": "jane@example.com", "role": "VOLUNTEER", "status": "ACTIVE", "createdAt": "2026-10-12T10:00:00Z" }
```
**Login**
```http
POST /api/auth/login
{ "email": "jane@example.com", "password": "Sup3rSecret!" }
```
```json
200 { "token": "eyJhbGciOi...", "tokenType": "Bearer", "expiresInSeconds": 3600,
      "user": { "id": 7, "name": "Jane Doe", "email": "jane@example.com", "role": "VOLUNTEER", "status": "ACTIVE", "createdAt": "..." } }
```
**Errors**
```json
400 { "timestamp": "...", "status": 400, "error": "Bad Request", "message": "Validation failed", "path": "/api/auth/register",
      "fieldErrors": { "email": "Email must be valid", "password": "Password must be 8-72 characters" } }
401 { "timestamp": "...", "status": 401, "error": "Unauthorized", "message": "Invalid email or password", "path": "/api/auth/login" }
```

## 10. Authentication and JWT flow

1. `POST /api/auth/login` -> `DaoAuthenticationProvider` loads the user by e-mail and checks the BCrypt hash. Failures (unknown e-mail, wrong password, suspended account) all return the same `401` message.
2. The server signs a JWT (HS256) with claims `sub` (user id), `role`, `iat`, `exp`, `iss=volunteerhub`. No password or e-mail inside.
3. The frontend sends `Authorization: Bearer <token>` on every protected call (`frontend/src/api/client.js`).
4. For each request Spring verifies signature, expiry (zero clock skew) and issuer, then **loads the user from the database**: unknown or non-`ACTIVE` users are rejected with `401`, and authorities come from the database, not from the token. Role changes and suspensions therefore apply immediately, even to tokens already issued. (Cost: one primary-key lookup per request.)
5. A `401` on an authenticated frontend request clears the stored token and returns the user to the login form with a "session expired" notice.

**Token storage trade-off.** The token is kept in `sessionStorage` (cleared when the tab closes, not shared between tabs) and the user profile in memory only; the role shown in the UI always comes from the server (`GET /api/users/me`), never from storage. Any JavaScript running on the page, i.e. an XSS bug, could read the token. The safer alternative is an `HttpOnly; Secure; SameSite` cookie, but that needs CSRF tokens, cookie-aware CORS and same-site hosting, so it was not chosen for this architecture. Mitigations: React escapes output by default, short token lifetime, no password storage, no `dangerouslySetInnerHTML`.

**CSRF.** CSRF protection is switched off *deliberately and only because* the API is stateless and authenticates exclusively with an explicit `Authorization` header (no cookies, no sessions), which cross-site requests cannot forge. If tokens ever move into cookies, CSRF protection must be re-enabled (see the comment in `SecurityConfig`).

## 11. Role-permission matrix

| Capability | ADMIN | ORGANIZER | VOLUNTEER |
|---|:-:|:-:|:-:|
| Register publicly | no (created via bootstrap) | yes | yes |
| View / edit own profile (name) | yes | yes | yes |
| List / view opportunities | yes | yes | yes |
| Create opportunity | no | yes | no |
| Edit opportunity | no | own only | no |
| Delete opportunity | any | own only | no |
| List users, change roles/status, delete users | yes | no | no |
| Change own role | no (use another admin) | no | no |

Applications to opportunities are **not implemented**, so there is no application permission yet.

## 12. Run the tests

```bash
# Backend (JUnit 5 + Mockito + Spring Boot Test/MockMvc against in-memory H2; no MySQL needed)
cd backend && mvn test

# Frontend (Jest + React Testing Library, fetch is mocked; no backend needed)
cd frontend && CI=true npm test -- --watchAll=false
cd frontend && npm run build        # production build (also runs ESLint)
```

Backend test classes: `AuthServiceTest`, `UserServiceTest` (service layer, mocked dependencies), `JwtServiceTest`, and `ApiIntegrationTest` (real filter chain, real JWTs, real repositories on H2: registration, hashing, login, 401/403/409, expired/forged/malformed tokens, role escalation attempts, ownership rules, last-admin protection, CORS).
H2 runs in MySQL compatibility mode; Flyway migrations are not exercised by `mvn test` (they were checked separately against MySQL 8). A Testcontainers-MySQL test is a sensible future addition.

## 13. Configure the initial Admin account safely

Public registration can never create an admin. To create the first one, set both variables for **one startup**:

```bash
export BOOTSTRAP_ADMIN_EMAIL='you@yourdomain.com'
export BOOTSTRAP_ADMIN_PASSWORD='a-long-unique-password-12+chars'
mvn spring-boot:run
```
It only acts when **no ADMIN exists** (so it is a no-op on later restarts), requires a password of 12+ characters and never logs it. If a user with that e-mail already exists (e.g. a migrated account) it is promoted to ADMIN and its password reset. Afterwards **unset both variables**, then manage further admins through `/api/admin/users`.

## 14. Known limitations and future improvements

- No refresh tokens, token revocation list or "log out everywhere": logout only discards the token client-side (suspending the account does invalidate it server-side).
- No rate limiting / account lockout on login; add before exposing the API publicly (e.g. Bucket4j or a reverse proxy).
- Last-admin protection is checked in the service layer; two admins removing each other at the exact same instant could race (a DB lock would close it).
- Registration reveals whether an e-mail is already registered (the `409` the requirements ask for). No e-mail verification or password reset yet.
- Token in `sessionStorage` is exposed to XSS (see section 10). Add a Content-Security-Policy header when deploying.
- Frontend has no router; screens are tab-based. Role checks in the UI are cosmetic, the backend is the authority.
- Applications to opportunities, notifications and pagination of the admin user list are future work.
- `npm audit` reports advisories in Create React App's build-time dependencies (react-scripts 5 is unmaintained); consider migrating to Vite.
- Add HTTPS and secure headers at the reverse proxy for production, and store `JWT_SECRET`/`DB_PASSWORD` in a secret manager.
