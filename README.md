# TrackPH

A modular platform for monitoring Philippine government project implementation (flood control, confidential funds, and similar), built as independent Spring Boot microservices. BSIT 3-1 capstone project, SY 2025-2026.

**Status:** `milestone-service` is the first deliverable and is feature-complete. Later deliverables will land as their own service folders alongside it.

## Contents

- [Repo Layout](#repo-layout)
- [Front-End & Design System](#front-end--design-system)
- [Getting Started](#getting-started)
- [Roles](#roles)
- [Seed Data](#seed-data)
- [Tech Stack](#tech-stack)

## Repo Layout

```
TrackPH/
├── trackph-microservice-template/        ← clone/hand off this to start a new service
│   ├── docs/dev-guide.md                 ← full developer guide: architecture, conventions, auth, Flyway, theme, anti-patterns
│   ├── src/main/resources/static/css/
│   │   └── theme.css                     ← canonical stylesheet; every service's copy must match it byte for byte
│   └── research-paper/                   ← this service's capstone research paper
├── milestone-service/                    ← Milestone & Timeline Monitoring (port 8081), the first deliverable
│   └── research-paper/                   ← this service's capstone research paper
└── scripts/
    └── sync-theme.sh                     ← pushes theme.css/theme-toggle.js from the template to every service
```

Each microservice folder is a complete, self-contained Maven project with no shared parent POM. The dev guide lives inside the template folder, not at the repo root, so the template stays self-contained when handed off to another team on its own.

## Front-End & Design System

No JS framework and no build step. Pages are server-rendered Thymeleaf templates styled with a single hand-written stylesheet, `theme.css`, which lives in the template folder and gets copied byte-for-byte into every service by `scripts/sync-theme.sh`. If you're changing styles, edit the template's copy and run that script rather than editing a service's copy directly.

The stylesheet is built on CSS custom properties (colors, spacing, shadows) so a page's look comes from reusing those variables and the existing component classes, not from hardcoded values. It supports light and dark mode, toggled from the navbar and persisted per browser.

To see every shared component rendered live, in both themes, run the template service itself (`cd trackph-microservice-template && mvn spring-boot:run`, port 8080) and open `/style-guide`. It's a reference page only: not linked from any navbar and not something an end user would ever reach.

## Getting Started

Read [trackph-microservice-template/docs/dev-guide.md](trackph-microservice-template/docs/dev-guide.md) before writing any code. It covers the tech stack, Eclipse/Maven setup, inter-service API contract, and the step-by-step for adding a new entity or cloning the template for a new service.

To run a service locally, you only need a MySQL 8 server running (see "Seed Data" below; the database itself is created automatically):

```
cd milestone-service
mvn spring-boot:run
```

Opens at `http://localhost:8081`; Swagger UI (API docs) at `http://localhost:8081/swagger-ui.html`.

## Roles

| Role | Can do |
|---|---|
| `PUBLIC` | View projects and milestones (budget hidden), browse the public audit trail |
| `AGENCY` | All of the above, plus create/edit/report on projects and milestones it owns |
| `ADMIN` | All of the above, across every agency, plus manage accounts and restore archived records |

## Seed Data

Point a service at a running MySQL 8 server and start it; the database, schema, and seed data all appear automatically, with no manual step. `milestone-service` seeds 7 demo projects (covering every status) and 3 demo logins, password `Demo@12345` for all three:

| Username | Role | Represents |
|---|---|---|
| `admin_demo` | ADMIN | Platform admin |
| `doh_agency` | AGENCY | Department of Health |
| `citizen_demo` | PUBLIC | Citizen/public viewer |

This only runs outside the `dev` profile (`dev` uses an in-memory H2 database instead, with no seed data). See dev-guide.md's "Database Migrations → Seed Data" section for how to force a full reseed.

## Tech Stack

Java 21 · Spring Boot 3.x · MySQL 8 · Flyway · Thymeleaf · JWT auth · SpringDoc OpenAPI
