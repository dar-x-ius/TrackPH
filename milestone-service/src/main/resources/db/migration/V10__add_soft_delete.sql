-- Switch Project/Milestone/Attachment deletion from hard DELETE to soft delete, so the record (and
-- any attached evidence) survives even when someone tries to remove it — a hard DELETE with FK
-- ON DELETE CASCADE meant deleting a project silently wiped its milestones and their proof photos
-- with nothing recoverable. AuditLog is intentionally left alone: it must never be soft-deletable.
ALTER TABLE projects ADD COLUMN deleted_at TIMESTAMP NULL DEFAULT NULL;
ALTER TABLE milestones ADD COLUMN deleted_at TIMESTAMP NULL DEFAULT NULL;
ALTER TABLE milestone_attachments ADD COLUMN deleted_at TIMESTAMP NULL DEFAULT NULL;

CREATE INDEX idx_projects_deleted_at ON projects(deleted_at);
CREATE INDEX idx_milestones_deleted_at ON milestones(deleted_at);
CREATE INDEX idx_attachments_deleted_at ON milestone_attachments(deleted_at);
