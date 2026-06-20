# AGENTS.md

## Cursor Cloud specific instructions

This repo is a single product split into two deployables:

- **Backend**: Spring Boot 3.3 REST API (Java 17 source level), Maven. Root of repo (`src/`, `pom.xml`). Default dev port **8081**.
- **Frontend**: Angular 19 SPA in `Gerenciamento_de_estoque_front/`. Dev server on port **4200**, talks to the backend at `http://localhost:8081` (`src/environments/environment.ts`).
- **Database**: PostgreSQL (required). See `docker-compose.yml` / `.env.example` for the canonical config.

The update script already runs `npm install` (frontend) and `./mvnw dependency:resolve` (backend) on startup. The notes below are durable, non-obvious caveats for starting/running things.

### Services & how to run them (dev)

| Service | Start command | Notes |
|---------|---------------|-------|
| PostgreSQL | `sudo pg_ctlcluster 16 main start` | Installed via apt and baked into the VM snapshot; it does NOT auto-start, so start it each session. DB `gerenciamento_estoque` (+ `..._test`) and user `postgres`/`postgres` already exist in the snapshot's data dir. |
| Backend | `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` | Reads `.env` (loaded via dotenv in `main()`). Local profile uses Hibernate `ddl-auto=update` and **Flyway disabled**, so tables are auto-created from entities. Seeds users on boot (see below). |
| Frontend | `cd Gerenciamento_de_estoque_front && npm start` | `ng serve` on port 4200. Node 22 (the VM default) works fine despite the `engines: 20.x` warning. |

### Required environment / `.env`

- The backend needs a `.env` at repo root (gitignored). Copy from `.env.example`. At minimum set `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, and `APP_PUBLIC_URL`.
- Non-obvious: even with the `local` profile, the base `application.properties` references `${SMTP_USERNAME}`, `${SMTP_PASSWORD}`, and `${EMAIL_FROM}` with **no defaults**. These must be present in `.env` (dummy values are fine) or the context fails to start. Mail features are not exercised in local dev.

### Seeded login credentials (created on every backend boot by `InitialDataLoader`, `@Profile("!test")`)

- `admin` / `admin123` — SUPER_ADMIN, subscription bypass.
- `William` / `William@2026` — ROLE_USER, subscription bypass.

Both bypass the SaaS subscription gate, so they can use the app without configuring a payment provider. Login is at `POST /auth/login` with `{ "username", "senha" }` (no org field). Creating a product requires an existing **Categoria** first (`/dashboard/categorias` then `/dashboard/produtos`).

### Tests

- Backend: `./mvnw test`. **Important gotcha**: the `@SpringBootTest` tests do NOT load `.env`, so you must export `APP_PUBLIC_URL` (e.g. `export APP_PUBLIC_URL=http://localhost:8081`) before running, otherwise ~16 tests fail with `Could not resolve placeholder 'APP_PUBLIC_URL'`. Even then there is a set of **pre-existing failing tests** (unrelated to environment): mockito `UnnecessaryStubbing`, `NullPointer` for unmocked services, `OrganizacaoNaoEncontradaException`, missing SaaS plan data, and a few status-code assertion mismatches. ~114/137 pass.
- Frontend: `CHROME_BIN=/usr/bin/google-chrome-stable npx ng test --watch=false --browsers=ChromeHeadless`. ChromeHeadless works, but there is a **pre-existing TypeScript compile error** in `src/app/guards/admin.guard.spec.ts` (`canActivate` called with 2 args) that currently blocks the suite from running.

### Build / lint

- Backend build: `./mvnw -DskipTests clean package` → `target/demo.jar`.
- Frontend build: `cd Gerenciamento_de_estoque_front && npx ng build` (only Sass `@import` deprecation warnings).
- No linter is configured for either project (no `lint` npm script, no checkstyle/spotless).

### Misc

- The repo root contains Windows artifacts that are not used on Linux and can be ignored: `jdk-21_windows-x64_bin/`, `hs_err_pid*.log`, `*.ps1`, `how origin`.
- Redis is optional (cache falls back to in-memory `simple`). `/actuator/health` may report `DOWN` because the Redis health indicator pings a non-running Redis; this does not affect API functionality.
