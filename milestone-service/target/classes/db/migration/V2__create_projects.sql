CREATE TABLE IF NOT EXISTS projects (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255)  NOT NULL,
    description TEXT,
    agency_id   BIGINT        NOT NULL,
    status      ENUM('PLANNED','IN_PROGRESS','DELAYED','COMPLETED','CANCELLED') NOT NULL DEFAULT 'PLANNED',
    start_date  DATE,
    end_date    DATE,
    location    VARCHAR(255),
    budget      DECIMAL(15,2),
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_project_agency FOREIGN KEY (agency_id) REFERENCES users(id)
);

CREATE INDEX idx_projects_agency ON projects(agency_id);
CREATE INDEX idx_projects_status ON projects(status);
