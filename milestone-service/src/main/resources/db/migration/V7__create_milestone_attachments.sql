CREATE TABLE milestone_attachments (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    milestone_id       BIGINT NOT NULL,
    original_filename  VARCHAR(255) NOT NULL,
    stored_filename    VARCHAR(255) NOT NULL UNIQUE,
    content_type       VARCHAR(100) NOT NULL,
    file_size          BIGINT NOT NULL,
    uploaded_by        BIGINT NOT NULL,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attachment_milestone FOREIGN KEY (milestone_id) REFERENCES milestones(id) ON DELETE CASCADE,
    CONSTRAINT fk_attachment_uploader FOREIGN KEY (uploaded_by) REFERENCES users(id)
);

CREATE INDEX idx_attachments_milestone ON milestone_attachments(milestone_id);
