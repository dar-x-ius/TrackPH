# TrackPH Developer Guide

A reference for humans and AI coding assistants working on any microservice in this repo.

---

## What is TrackPH?

TrackPH is a modular platform for monitoring Philippine government project implementation. Each folder in this repo is an **independent Spring Boot microservice** — no shared parent POM. Each service runs on its own port and can be developed, tested, and deployed independently.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.x |
| Build | Maven |
| Database | MySQL 8 |
| ORM | Spring Data JPA / Hibernate |
| Migrations | Flyway |
| UI | Thymeleaf (server-side rendering) |
| Auth | Stateless JWT (`jjwt`) |
| API Docs | SpringDoc OpenAPI (Swagger UI at `/swagger-ui.html`) |
| Dev DB | H2 in-memory (activated by `dev` profile) |

---

## Repo Layout

```
TrackPH/
├── trackph-microservice-template/ ← clone this to start a new service
│   └── docs/                      ← you are here
├── milestone-service/             ← Milestone & Timeline Tracking
└── <future-services>/
```

Each microservice folder is a complete, self-contained Maven project. This guide lives inside the template (not at the repo root) so the template folder is self-contained when handed off to another team on its own — see "Maintaining This Documentation" below.

---

## Eclipse Setup

No plugins needed — the project does **not** use Lombok, so write getters, setters, constructors and loggers by hand.

1. **File → Import → Existing Maven Projects**, one service at a time (not the repo root).
2. Select the project → **Maven → Update Project** (Alt+F5, tick *Force update*).
3. Run the `@SpringBootApplication` class with **Run As → Java Application** (JDK 21+ under Window → Preferences → Java → Installed JREs).
4. If you see `Unresolved compilation problem` at runtime, delete the service's `target/` folder and rebuild.

Not using Eclipse? `mvn spring-boot:run` in the service folder works too.

---

## Inter-Service Contract

- Services talk to each other via **REST only**.
- Every HTTP response **must** use the `ApiResponse<T>` wrapper:
  ```json
  { "status": "success", "message": "...", "data": { ... } }
  { "status": "error",   "message": "...", "data": null }
  ```
- **Never** return a raw JPA entity from a controller. Always map to a DTO first.

---

## Auth Model

- Stateless JWT. No sessions.
- Token is passed as `Authorization: Bearer <token>`.
- JWT payload contains `userId` (Long) and `role` (String).
- **Roles**: `ADMIN`, `AGENCY`, `MANAGER`, `PUBLIC`
- Role-based access rules live **only** in `SecurityConfig.java`. Do not add `@PreAuthorize` annotations elsewhere — keep access control in one place.
- Public endpoints (no token required) are listed in the `permitAll` section of `SecurityConfig`.

---

## Audit Logging

- `AuditInterceptor` automatically logs every `POST`, `PUT`, and `DELETE` request.
- It extracts the user from the JWT, records the action, entity type, entity ID, and IP address in `audit_logs`.
- **Do NOT call `AuditLogService` from your controllers** — the interceptor already handles it.
- Audit logs are write-only from the application; only `ADMIN`/`AGENCY` roles can read them via the API.

---

## Alert System

- `AlertScheduler` runs a cron job daily at midnight (`0 0 0 * * ?`).
- It scans milestones whose `due_date` has passed without a `completed_date` and creates `Alert` records.
- Severity is derived from delay duration: 1–3 days → LOW, 4–7 → MEDIUM, 8–14 → HIGH, 15+ → CRITICAL.
- **Dev-only trigger**: `POST /api/v1/dev/alerts/trigger` (only available when `spring.profiles.active=dev`).

---

## Database Migrations (Flyway)

- All schema changes go through Flyway migrations in `src/main/resources/db/migration/`.
- File naming: `V{n}__{snake_case_description}.sql` (two underscores).
  - Example: `V3__create_milestones.sql`
- **Never edit a committed migration file.** If you need to change a schema, add a new migration (`Vn+1__...`).
- In the `dev` profile, H2 is used with `ddl-auto=create-drop` — Flyway is disabled there.

---

## Theme & UI

- CSS variables are defined in `static/css/theme.css`.
- Use only these variables in your styles — no hardcoded hex colors:
  - `--primary` (TrackPH red `#C0392B`)
  - `--primary-dark`
  - `--bg`, `--surface`, `--text`, `--border`
- Dark mode is toggled by setting `data-theme="dark"` on `<html>`. The `theme-toggle.js` script handles this and persists to `localStorage`.
- **Do not add a second theme toggle** — `layout.html` already has one in the navbar.
- All Thymeleaf pages must extend `layout.html` via `th:replace` or `th:insert`. Do not write standalone HTML pages.
- **Cross-service consistency**: `trackph-microservice-template/src/main/resources/static/css/theme.css` is the canonical copy. Every service's `theme.css` is a copy of it, not an independent file — edit the template's copy, then run `./scripts/sync-theme.sh` from the repo root to push the change to every service. Run `./scripts/sync-theme.sh --check` to verify no service has drifted (fails with a diff if one has).

---

## Page Routing (Thymeleaf)

- `PageController` is the only controller allowed to return view names (page routes like `/projects`, `/dashboard`).
- It injects nothing and calls no services — every page loads its data client-side via the REST API (`fetch` calls to `ApiResponse<T>` endpoints).
- Adding a page = add one `@GetMapping` method returning the template path, plus the Thymeleaf template itself under `templates/`.
- Do not put `@RestController` logic or DB/service calls in `PageController` — that belongs in the REST controllers.

---

## How to Add a New Entity (Step-by-Step)

Follow this order — skipping steps causes JPA startup failures:

1. Write a Flyway migration: `Vn__create_{entity}.sql`
2. Create the `@Entity` class in `model/`
3. Create a `JpaRepository` in `repository/`
4. Create a `@Service` class in `service/`
5. Create a `@RestController` in `controller/`
6. Create request/response DTOs in `dto/request/` and `dto/response/`
7. Annotate the controller with `@Tag` and each endpoint with `@Operation` (Swagger)

---

## Swagger / API Docs

- Swagger UI: `http://localhost:{port}/swagger-ui.html`
- OpenAPI JSON: `http://localhost:{port}/v3/api-docs`
- **Every controller must have `@Tag(name = "...")`.**
- **Every endpoint must have `@Operation(summary = "...")`.**
- This is the inter-team API contract. Other microservice teams depend on it.

---

## Testing

- Unit tests go in `src/test/java/`.
- Use `@SpringBootTest(properties = "spring.profiles.active=dev")` to boot with H2.
- Flyway is disabled in the `dev` profile — schema comes from `ddl-auto=create-drop`.
- Integration tests that need data: use `@Sql` to load fixtures, or `@BeforeEach` with repository saves.

---

## Cloning the Template for a New Microservice

1. Copy `trackph-microservice-template/` to a new folder (e.g., `budget-service/`).
2. Global replace: `ph.trackph.template` → `ph.trackph.{your-service}`.
3. Global replace: `TemplateApplication` → `{YourService}Application`.
4. Delete `Sample.java`, `SampleController.java`, `SampleService.java`, `SampleRepository.java`, `SampleRequest.java`, `SampleResponse.java`, and `V1__init.sql`.
5. Update `application.yml`: set `spring.application.name`, `server.port` (pick a unique port), and DB credentials.
6. Add your first entity following the step-by-step above.

---

## Anti-Patterns — Do Not Do These

- Do not add a new Maven dependency for something a few lines can do.
- Do not return raw JPA entities from controllers.
- Do not write SQL outside of Flyway migrations.
- Do not add a new role without updating `SecurityConfig`.
- Do not add `@Transactional` on controller methods — it belongs in the service layer.
- Do not write inline styles in Thymeleaf templates — use `theme.css` variables.
- Do not call `AuditLogService` from controllers — the interceptor handles it.
- Do not create a new `theme.css` — every service shares the same one from the template.
- Do not add Lombok — write getters, setters and constructors by hand.

---

## Maintaining This Documentation

- **This file (`trackph-microservice-template/docs/dev-guide.md`) is the single source of truth, and the only doc file tracked in git.** Everything platform-wide goes here, once. It lives inside the template, not at the repo root, so that handing off just the `trackph-microservice-template/` folder to another team gives them the whole guide too — no separate `docs/` folder to remember to include. Other services (e.g. `milestone-service/`) link to it with a relative path (`../trackph-microservice-template/docs/dev-guide.md`); they do not get their own copy.
- `CLAUDE.md` (root and per-service) is a **thin wrapper**: a few lines pointing back here, plus (for a service folder) a short "service-specific notes" section for facts that don't apply platform-wide (port number, DFD-to-controller mapping, etc). It exists on disk for local AI-assistant use only — it, along with `AGENTS.md`, `GEMINI.md`, `.cursorrules`, and `.github/copilot-instructions.md`, is gitignored (see `.gitignore`) so the repo doesn't track or visibly advertise which AI tools were used, and so it doesn't need re-syncing across machines.
- **When something platform-wide changes** (new convention, new anti-pattern, new step in a workflow): edit only this file. Do not copy the change into `CLAUDE.md`.
- **When something service-specific changes** (new endpoint, new scheduled job, new port): update that service's local `CLAUDE.md` "Service-Specific Notes" section. Keep it short — one line per fact.
- **When adding a new microservice**: base its `CLAUDE.md` on `trackph-microservice-template/`'s (has the clone checklist), not `milestone-service/`'s (has milestone-specific facts you don't want). Its link to the dev guide should read `../trackph-microservice-template/docs/dev-guide.md`, same as `milestone-service/CLAUDE.md` — unless your new service lives somewhere other than directly under the repo root, in which case adjust the `../` count.
- **Never** let `CLAUDE.md` grow past ~10 lines. If you need more than that, the content belongs in this file instead, with `CLAUDE.md` just linking to the relevant section.
