CREATE TABLE IF NOT EXISTS alerts (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id   BIGINT        NOT NULL,
    milestone_id BIGINT,
    type         ENUM('DEADLINE_MISSED','DELAY_DETECTED','STATUS_CHANGE','REPORT_SUBMITTED') NOT NULL,
    message      TEXT          NOT NULL,
    severity     ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL DEFAULT 'LOW',
    is_read      BOOLEAN       NOT NULL DEFAULT FALSE,
    triggered_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_alert_project   FOREIGN KEY (project_id)   REFERENCES projects(id)   ON DELETE CASCADE,
    CONSTRAINT fk_alert_milestone FOREIGN KEY (milestone_id) REFERENCES milestones(id) ON DELETE SET NULL
);

CREATE INDEX idx_alerts_project  ON alerts(project_id);
CREATE INDEX idx_alerts_is_read  ON alerts(is_read);
CREATE INDEX idx_alerts_severity ON alerts(severity);
