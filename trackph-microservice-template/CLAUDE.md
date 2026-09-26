# TrackPH Microservice — Claude Code Context

Spring Boot microservice for the TrackPH government project monitoring platform.

See [../docs/dev-guide.md](../docs/dev-guide.md) for the full developer guide: architecture, auth, audit logging, Flyway rules, theme system, and anti-patterns.

## Quick: Clone Checklist

1. Rename package `ph.trackph.template` → `ph.trackph.<your-service>` (global replace).
2. Rename `TemplateApplication` → `<YourService>Application`.
3. Delete `Sample*` files and `V1__init.sql` — they are examples only.
4. Update `application.yml`: `spring.application.name`, `server.port`, DB name.
5. Add your first entity following the step-by-step in dev-guide.md.
