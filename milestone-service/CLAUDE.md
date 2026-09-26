# Milestone & Timeline Monitoring — Claude Code Context

Spring Boot microservice (port 8081) implementing the 5 DFD processes from the TrackPH Milestone & Timeline Monitoring spec.

See [../docs/dev-guide.md](../docs/dev-guide.md) for platform-wide conventions.

## Service-Specific Notes

- **Audit logging is automatic** — `AuditInterceptor` in `WebConfig` logs all `/api/**` mutations. Do not call `AuditLogService` from controllers.
- **Alert scheduling** — `AlertScheduler` runs at midnight. Dev trigger: `POST /api/v1/dev/alerts/trigger` (dev profile only).
- **5 DFD processes mapped to controllers**:
  1. Manage Projects & Users → `ProjectController`, `AuthController`
  2. Customize Dashboard → `DashboardController`
  3. Log User Actions → `AuditLogController` (reads) + `AuditInterceptor` (writes)
  4. Submit Project Report → `MilestoneController` `/report` endpoint
  5. Generate Alerts → `AlertController` + `AlertScheduler`
- **Public access** — `GET /api/v1/projects` and `GET /api/v1/projects/{id}` are public. `ProjectResponse.publicView()` strips budget data for PUBLIC role.
- **Port**: 8081
