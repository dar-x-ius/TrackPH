# TrackPH Developer Guide

A reference for humans and AI coding assistants working on any microservice in this repo.

---

## What is TrackPH?

TrackPH is a modular platform for monitoring Philippine government project implementation. Each folder in this repo is an **independent Spring Boot microservice** — no shared parent POM. Each service runs on its own port and can be developed, tested, and deployed independently.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
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
├── docs/                          ← you are here
├── trackph-microservice-template/ ← clone this to start a new service
├── milestone-service/             ← Milestone & Timeline Tracking
└── <future-services>/
```

Each microservice folder is a complete, self-contained Maven project.

---

## Eclipse Setup (Lombok required)

The code uses [Lombok](https://projectlombok.org) (`@Getter`, `@Data`, `@RequiredArgsConstructor`, `@Slf4j`, ...), which generates getters, setters, constructors and `log` at compile time. Maven handles this through `pom.xml`, but **Eclipse does not until Lombok is installed into it**. Without it you get hundreds of false errors such as `log cannot be resolved`, `getName() is undefined`, and `blank final field may not have been initialized` — and Eclipse writes broken classes into `target/classes`, which then crash the app at startup with `Unresolved compilation problem`.

One-time setup per machine:

1. Close Eclipse.
2. Run the installer (uses the jar Maven already downloaded; run `mvn compile` once first if it is missing):
   ```bash
   java -jar ~/.m2/repository/org/projectlombok/lombok/1.18.48/lombok-1.18.48.jar install "<path-to>/eclipse.exe"
   ```
   Or run the jar with no arguments for the GUI and use "Specify location". This adds a `-javaagent:lombok.jar` line to `eclipse.ini`.
3. Start Eclipse, import each service via **File → Import → Existing Maven Projects** (not the repo root).
4. Select the projects → **Maven → Update Project** (Alt+F5, tick *Force update*), then **Project → Clean**.
5. Run the `@SpringBootApplication` class with **Run As → Java Application** (needs a JDK 17+ registered under Window → Preferences → Java → Installed JREs).

If you still see `Unresolved compilation problem` at runtime, delete the service's `target/` folder and rebuild. Not using Eclipse? `mvn spring-boot:run` in the service folder needs none of this.

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
