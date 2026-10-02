# TrackPH

A modular platform for monitoring Philippine government project implementation (flood control, confidential funds, and similar), built as independent Spring Boot microservices. BSIT 3-1 capstone project, SY 2025-2026.

## Repo Layout

```
TrackPH/
├── docs/dev-guide.md              ← full developer guide: architecture, conventions, auth, Flyway, theme, anti-patterns
├── trackph-microservice-template/ ← clone this to start a new service
├── milestone-service/             ← Milestone & Timeline Monitoring (port 8081) — first deliverable
└── <future-services>/
```

Each microservice folder is a complete, self-contained Maven project — no shared parent POM.

## Getting Started

Read [docs/dev-guide.md](docs/dev-guide.md) before writing any code. It covers the tech stack, Eclipse/Maven setup, inter-service API contract, and the step-by-step for adding a new entity or cloning the template for a new service.

## Tech Stack

Java 21 · Spring Boot 3.x · MySQL 8 · Flyway · Thymeleaf · JWT auth · SpringDoc OpenAPI

## AI Coding Assistants

This repo is tool-agnostic — `CLAUDE.md`, `AGENTS.md`, `GEMINI.md`, `.cursorrules`, and `.github/copilot-instructions.md` all point back to `docs/dev-guide.md`, which is the single source of truth. See its "Maintaining This Documentation" section before editing any of these.
