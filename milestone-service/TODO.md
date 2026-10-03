# TODO

## 1. Tech-stack cleanup (do first)

Make sure everything in this service is on the correct/intended tech stack before touching feature gaps below.

## 2. Close gaps vs. the research paper's DFD (after cleanup)

Compared `research-paper/FINAL (Compiled) - Milestone & Timeline Monitoring.pdf` (Level 0/1 DFDs, §3.2.2-3.2.3) against the actual code. Two sub-processes are drawn in the paper's diagrams but don't exist yet:

- **3.3 "Analyze Behavior Patterns" (Figure 3.3, Log User Actions)** — paper shows audit logs feeding a pattern/trend-analysis step that compiles reports for AGENCY. `AuditLogService` currently only does raw paginated reads (`findAll`/`findByUser`/`findByEntity`) — no analysis, no compiled report.
- **5.2 "Notify Stakeholders" (Figure 3.5, Generate Alerts)** — paper draws active push arrows to both USER and AGENCY. `AlertController` only exposes `GET /alerts` + unread count — alerts just sit in the table for polling, there's no actual notify mechanism (email/websocket/etc).

Minor, lower priority: 4.3 "Flag Delays or Changes" (Figure 3.4) is its own diagrammed sub-process feeding AGENCY directly, but in code that logic is folded into `AlertScheduler.checkOverdueMilestones()` under process 5.0 instead of kept distinct. Functionally present, just merged differently than the diagram shows.

Everything else (1.0, 2.0, 3.1/3.2, 4.1/4.2, 5.1/5.3) matches the diagrams and prose closely — see `AuthController`/`ProjectController`, `DashboardController`, `AuditInterceptor`, `MilestoneController`'s `/report` endpoint, `AlertScheduler`.
