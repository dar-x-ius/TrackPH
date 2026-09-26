# Milestone & Timeline Monitoring — Agent Context

Spring Boot microservice (port 8081) implementing TrackPH milestone and timeline tracking.

See [../docs/dev-guide.md](../docs/dev-guide.md) for platform-wide conventions.

Key facts: `AuditInterceptor` auto-logs mutations (never call `AuditLogService` from controllers). `AlertScheduler` runs at midnight — dev trigger at `POST /api/v1/dev/alerts/trigger`. Port 8081.
