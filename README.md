# TrackPH

A modular platform for monitoring Philippine government project implementation (flood control, confidential funds, and similar), built as independent Spring Boot microservices. BSIT 3-1 capstone project, SY 2025-2026.

## Repo Layout

```
TrackPH/
├── trackph-microservice-template/ ← clone/hand off this to start a new service
│   ├── docs/dev-guide.md          ← full developer guide: architecture, conventions, auth, Flyway, theme, anti-patterns
│   └── research-paper/            ← this service's capstone research paper
├── milestone-service/             ← Milestone & Timeline Monitoring (port 8081) — first deliverable
│   └── research-paper/            ← this service's capstone research paper
└── <future-services>/
```

Each microservice folder is a complete, self-contained Maven project — no shared parent POM. The dev guide lives inside the template folder (not at the repo root) so the template stays self-contained when handed off to another team on its own.

## Getting Started

Read [trackph-microservice-template/docs/dev-guide.md](trackph-microservice-template/docs/dev-guide.md) before writing any code. It covers the tech stack, Eclipse/Maven setup, inter-service API contract, and the step-by-step for adding a new entity or cloning the template for a new service.

## Seed Data

Point a service at a running MySQL 8 server and start it — the database, schema, and seed data all appear automatically, no manual step. `milestone-service` seeds 7 demo projects (covering every status) and 3 demo logins, password `Demo@12345` for all three:

| Username | Role | Represents |
|---|---|---|
| `admin_demo` | ADMIN | Platform admin |
| `doh_agency` | AGENCY | Department of Health |
| `citizen_demo` | PUBLIC | Citizen/public viewer |

This only runs outside the `dev` profile (`dev` uses an in-memory H2 DB instead, with no seed data). See dev-guide.md's "Database Migrations → Seed Data" section for how to force a full reseed, and that service's own `CLAUDE.md` for anything seed-related specific to it.

## Tech Stack

Java 21 · Spring Boot 3.x · MySQL 8 · Flyway · Thymeleaf · JWT auth · SpringDoc OpenAPI
