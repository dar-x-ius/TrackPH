# TrackPH Developer Guide

A reference for developers working on any microservice in this repo.

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
| Maps | Leaflet + OpenStreetMap, geocoded via Nominatim (see "Maps & Geocoding") |

---

## Repo Layout

```
TrackPH/
├── trackph-microservice-template/        ← copy this folder to start a new service
│   ├── docs/                             ← you are here
│   └── src/main/resources/static/css/
│       └── theme.css                     ← canonical stylesheet; see "Theme & UI" below
├── milestone-service/                    ← Milestone & Timeline Monitoring
└── scripts/
    └── sync-theme.sh                     ← pushes theme.css/theme-toggle.js to every service
```

Each microservice folder is a complete, self-contained Maven project. This guide lives inside the template (not at the repo root) so the template folder is self-contained when handed off to another team on its own — see "Maintaining This Documentation" below. For the full, current layout (every service folder, what each one contains), see the root `README.md`'s own "Repo Layout" section — this copy is kept intentionally short, just enough to orient a reader of this guide.

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
- **Roles**: `ADMIN`, `AGENCY`, `PUBLIC`
- Role-based access rules live **only** in `SecurityConfig.java`. Do not add `@PreAuthorize` annotations elsewhere — keep access control in one place.
- Public endpoints (no token required) are listed in the `permitAll` section of `SecurityConfig`.

---

## Audit Logging

- `AuditInterceptor` automatically logs every `POST`, `PUT`, and `DELETE` request.
- It extracts the user from the JWT, records the action, entity type, entity ID, and IP address in `audit_logs`.
- **Do NOT call `AuditLogService` from your controllers** — the interceptor already handles it.
- A service can attach a before/after diff to the row the interceptor is about to write, without becoming a second writer: inject `AuditContext` and call `recordChange(entityType, entityId, before, after)` (or `recordDeletion(...)` for a delete) before the method returns. The interceptor picks these request attributes up if present; if a service doesn't call it, the row is still written, just without a `details` payload.
- **Default audit logs to public, not `ADMIN`/`AGENCY`-only.** This is a transparency platform, and an audit trail only staff can see doesn't actually constrain staff: nothing stops a bad actor from acting with no outside witness.
- Withhold only two things from a non-`ADMIN` reader, not the whole log: the IP address (a privacy concern, not a transparency one), and rows that are account-management noise rather than project activity, like sign-ins, registrations, and "marked an alert read." Everything else, who changed what project or milestone and what the before/after values were, stays visible to the public. See `milestone-service`'s `AuditLogController`/`AuditLogRepository` for a worked example.

---

## Alert System

- `AlertScheduler` runs a cron job daily at midnight (`0 0 0 * * ?`).
- It scans milestones whose `due_date` has passed without a `completed_date` and creates `Alert` records.
- Severity is derived from delay duration: 1–3 days → LOW, 4–7 → MEDIUM, 8–14 → HIGH, 15+ → CRITICAL.
- **Dev-only trigger**: `POST /api/v1/dev/alerts/trigger` (only available when `spring.profiles.active=dev`).

---

## Maps & Geocoding

- No Google Maps, Mapbox, or any other paid mapping service — there's no billing account behind this project. Maps are **Leaflet** (a free JS library) tiled from the public **OpenStreetMap** server, and a location string is turned into coordinates via **Nominatim**, OSM's free geocoder. None of this needs an API key.
- `milestone-service/static/js/maps-widget.js` is the frontend helper for this — not yet synced across services the way `theme.css` is (see "Theme & UI"), so a second service that wants a map should start by copying it over. It lazy-loads Leaflet (plus the `leaflet.heat` plugin for heat layers) from a CDN the first time a page actually calls `TrackPHMaps.load()`, so a page that never shows a map pays nothing for it. It also exposes `initPinPicker(elementId, initialLatLng, onChange)` for a click-to-place marker (used on a create/edit form) and `baseLayer()` for the shared OSM tile layer, so a new page doesn't have to re-implement either from scratch.
- Geocoding a free-text address happens server-side and in the background, not on the request that saves the record — Nominatim's public server is rate-limited to about one request per second, which is far too slow to hold a save request open for. The pattern: give the entity a `latitude`/`longitude` pair plus a `geocodeStatus` enum (`PENDING` / `DONE` / `FAILED`), set it to `PENDING` whenever the address text changes, and let a scheduler pick up pending rows and fill them in.
- `GeocodingScheduler` is that scheduler for `milestone-service`'s projects, built on the same `@Scheduled` pattern as `AlertScheduler` above, polling every 60 seconds. It's deliberately *not* `@Transactional`: geocoding a batch means sleeping ~1.1 seconds between calls to stay under Nominatim's rate limit, and holding a database transaction open for that whole sleep would tie up a connection for no reason. It geocodes the batch first with no transaction open, then saves the results in one short write.
- `GeocodingService.geocode()` sends a descriptive `User-Agent` header identifying the app, which Nominatim's usage policy requires of anyone calling its public server. Don't strip that header if you touch this class — a generic or missing one risks getting the server's IP blocked.

---

## Database Migrations (Flyway)

- All schema changes go through Flyway migrations in `src/main/resources/db/migration/`.
- File naming: `V{n}__{snake_case_description}.sql` (two underscores).
  - Example: `V3__create_milestones.sql`
- **Never edit a committed migration file.** If you need to change a schema, add a new migration (`Vn+1__...`).
- **Squashing old migrations into one (e.g. folding a later patch back into the seed migration it's fixing) is fine, but only once you've confirmed no one has actually run that migration against a real database yet.** Flyway tracks what ran by filename + checksum in `flyway_schema_history`; deleting or renaming an applied migration makes Flyway refuse to start for anyone whose database still has the old record, until they drop and recreate their schema. On a team project, that means asking before squashing, not just checking your own machine — a teammate's local MySQL DB is just as real as yours here. In `dev` (H2, `create-drop`) this is a non-issue since nothing persists between restarts.
- In the `dev` profile, H2 is used with `ddl-auto=create-drop` — Flyway is disabled there.

### Seed Data

- A service may ship a seed migration (e.g. `V8__seed_demo_data.sql`) that inserts demo rows — users, sample records — instead of leaving a fresh database empty. It's a normal numbered migration, nothing special: plain `INSERT` statements, following the same `Vn__description.sql` naming and "never edit a committed one" rule as a schema migration.
- Because it's Flyway-managed, it runs automatically and exactly once, on anything the `dev` profile doesn't touch — point a fresh MySQL database at the service and start it; no manual step, no seed script to remember to run.
- **To force a full reseed** (e.g. to verify seed data still works end-to-end, or to wipe out accumulated manual-testing cruft): dropping and recreating the schema is the only way — Flyway won't re-run a migration it's already recorded as applied. This is destructive to whatever is currently in that database, so don't do it to a database with real work in it without being sure; a disposable throwaway schema is a safer way to test a seed migration in isolation.
- **If you edit a seed migration's `INSERT` values after it's already been applied somewhere** (only safe before it's committed — see the rule above), Flyway will refuse to start with a checksum mismatch on any database that already recorded the old version. Either reseed that database (see above), or run `flyway:repair` (or update that migration's `checksum` column in `flyway_schema_history` to match the new file) to bring the recorded checksum back in line — repair only fixes the bookkeeping, it doesn't retroactively insert the new rows, so a database fixed this way keeps whatever the old migration inserted until it's reseeded or backfilled by hand.
- Demo account credentials and what's seeded are service-specific, not part of this shared guide — see the root `README.md`'s "Seed Data" section.

---

## Theme & UI

- CSS variables are defined in `static/css/theme.css`. Use only these variables in your styles — no hardcoded hex
  colors. The full set, with live swatches, is on `/style-guide` (see below); the ones worth knowing by name:
  - `--primary` / `--primary-dark` / `--primary-light` (TrackPH red `#DA1219` and its hover/gradient shades)
  - `--text-accent` — heading/link/stat-number color on top of `--bg`/`--surface`. Same as `--primary` in light
    mode, but swaps to `--primary-light` in dark mode, since full-saturation red is harsh on a near-black page.
    Use this, not `--primary` directly, for text that sits straight on the page — `--primary` itself stays correct
    for anything with its own background (a button, a badge).
  - `--bg` / `--surface` / `--surface-raised` — the base lightness steps a theme offers
  - `--page-bg` / `--card-bg` — which of the above the page canvas vs. a `.card` actually uses. Kept as separate
    tokens (not just "page = `--bg`, card = `--surface`") because the two themes need the relationship in opposite
    directions for a card to read as "elevated": white-on-light-gray in light mode, light-gray-on-near-black in
    dark mode. Use `.card`/`.card-table`/`.stat-tile` rather than wiring these up yourself.
  - `--text`, `--text-muted`, `--border`
  - `--success` / `--warning` / `--danger` / `--info`, each with a `-bg`/`-text`/`-border` triple for badges and
    alerts. `--warning` also doubles as the landing page's single decorative accent color (feature icons, the
    hero's highlighted word, the navbar brand's hover state) — reuse it there rather than adding a second accent.
- Dark mode is toggled by setting `data-theme="dark"` on `<html>`. The `theme-toggle.js` script handles this and persists to `localStorage` under the `trackph-theme` key.
- **Avoiding a flash of the wrong theme on page load**: this is a server-rendered multi-page app, so every navigation is a full page load/repaint, not client-side routing. `theme-toggle.js`'s `initTheme()` only runs on `DOMContentLoaded`, which is too late to prevent a flash of the default light `--bg` before it fires. `layout.html`'s `<head>` therefore has a small inline `<script>`, placed before the `theme.css` link, that reads `localStorage` synchronously and sets `data-theme` on `<html>` before first paint. Do not remove it or move it after the CSS link.
- **Do not add a second theme toggle** — `layout.html` already has one in the navbar.
- All Thymeleaf pages must extend `layout.html` via `th:replace` or `th:insert`. Do not write standalone HTML pages.
- **Cross-service consistency**: `trackph-microservice-template/src/main/resources/static/css/theme.css` and `theme-toggle.js` are the canonical copies — every service's copy must be byte-identical, there's no legitimate reason to diverge. Edit the template's copy, then run `./scripts/sync-theme.sh` from the repo root to push the change to every service. Run `./scripts/sync-theme.sh --check` to verify no service has drifted (fails with a diff if one has).
- **`layout.html` is shared structure, not a shared file** — it is deliberately *not* byte-synced, because a service is allowed to add its own global script to it (e.g. `milestone-service` appends an alert-badge poller after `initTheme()`). What must stay identical is the *structure*, which `sync-theme.sh --check` also verifies:
  - It must declare `th:fragment="layout(pageTitle, content)"` on the `<html>` tag — without this the service's own pages fail to resolve the fragment at all.
  - Its navbar links must come from `th:replace="~{fragments/nav :: links}"`, not be hardcoded inline. Each service owns its own `templates/fragments/nav.html` (not synced — it's meant to differ) defining its actual links; the template's copy ships a generic sign-in/sign-out-only default. **Do not hardcode nav `<li>` entries directly into `layout.html` again** — that was the original bug that made the template's navbar break for any team whose service doesn't have `/dashboard`, `/projects`, `/alerts`, `/admin/audit-logs` routes.
  - Brand name and footer tagline come from the `appName` / `appTagline` model attributes (used via `${appName} ?: 'TrackPH'` so a missing bean still degrades gracefully) rather than hardcoded text, supplied per service by a small `@ControllerAdvice` class — see `config/BrandConfig.java`. Edit its two constants for your service instead of editing strings in `layout.html`.
- **Landing pages / public marketing content are not part of the shared template.** `theme.css` ships generic, reusable primitives for building one (`.hero`, `.features`, `.recent-projects`, `.cta-band`, etc.) but the template's own `index.html` stays a minimal generic placeholder. A service's actual landing page (see `milestone-service/.../index.html` for an example) will usually fetch from that service's own domain API, so it isn't portable as-is — build your own content on top of the shared CSS classes rather than copying another service's `index.html` wholesale.
- **Hero photos are service-owned, not synced.** If a service's landing page uses a photo background (`.hero-photo` in `theme.css`), the image file lives at `<service>/src/main/resources/static/images/`, created by that service, not pushed by `sync-theme.sh` (only `theme.css`/`theme-toggle.js` are). Unlike the project's hand-rolled SVGs, a sourced photo has a real license to track — add a short `CREDITS.md` next to it recording where it came from and under what license.
- **`/style-guide` is the living reference for every shared primitive** — colors, typography, buttons, badges, cards, stat tiles, tables, forms, alerts, the page-header/filter-bar/back-link/empty-state patterns, the milestone timeline, and the misc components (info tooltip, password-field toggle). It's template-only: defined in this service's own `PageController`/`style-guide.html`, permitted in `SecurityConfig`, and deliberately not linked from any navbar or synced to other services — open `/style-guide` directly on a running instance. Toggle dark mode from the navbar while you're on it to check both themes at once. When you add a new shared class to `theme.css`, add a matching example here in the same change; this page existing is also why a new component variant should be rare — check it before inventing a new class.

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

## Copying the Template for a New Microservice

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
- Do not gate a whole feature behind `ADMIN` just because it touches account or security data — gate the specific field or row that's actually sensitive (see "Audit Logging" above) and leave the rest visible.
- Do not create a new `theme.css` — every service shares the same one from the template.
- Do not hardcode navbar links directly into `layout.html` — add them to your service's `templates/fragments/nav.html`.
- Do not hardcode the brand name or footer tagline into `layout.html` — edit the constants in `config/BrandConfig.java`.
- Do not add Lombok — write getters, setters and constructors by hand.
- Do not reach for Google Maps, Mapbox, or another paid/API-key mapping service — see "Maps & Geocoding" for the free Leaflet + OpenStreetMap + Nominatim stack this project already uses.

---

## Maintaining This Documentation

- **This file is the single source of truth for anything platform-wide**, and the only doc file tracked in git besides the root `README.md`. Everything that applies to every service goes here, once — not copied into each service's own notes. It lives inside the template, not at the repo root, so that handing off just the `trackph-microservice-template/` folder to another team gives them the whole guide too — no separate `docs/` folder to remember to include. Other services (e.g. `milestone-service/`) link to it with a relative path (`../trackph-microservice-template/docs/dev-guide.md`); they do not get their own copy.
- **Service-specific facts** (port number, DFD-to-controller mapping, demo credentials) belong in the README's "Seed Data" section or inline in that service's own code/config — not duplicated here.
- **`research-paper/`** (per-service, e.g. `milestone-service/research-paper/`) holds that service's capstone research paper — a separate deliverable from this dev guide, not covered by the single-source-of-truth rule above. Each service's paper is its own content; don't merge or link them together.
