CREATE TABLE IF NOT EXISTS milestones (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id       BIGINT        NOT NULL,
    title            VARCHAR(255)  NOT NULL,
    description      TEXT,
    due_date         DATE          NOT NULL,
    completed_date   DATE,
    status           ENUM('PENDING','IN_PROGRESS','COMPLETED','OVERDUE') NOT NULL DEFAULT 'PENDING',
    sequence_order   INT           NOT NULL DEFAULT 0,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_milestone_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE INDEX idx_milestones_project ON milestones(project_id);
CREATE INDEX idx_milestones_status ON milestones(status);
CREATE INDEX idx_milestones_due_date ON milestones(due_date);
