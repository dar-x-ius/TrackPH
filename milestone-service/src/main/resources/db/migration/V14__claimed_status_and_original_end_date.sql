-- Phase 1b: agencies claim IN_PROGRESS or COMPLETED when they attach a proof, and a milestone finished
-- after its due date reads COMPLETED_LATE. The project remembers its first end date so an extension shows.
ALTER TABLE milestones MODIFY status ENUM('PENDING','IN_PROGRESS','COMPLETED','COMPLETED_LATE','OVERDUE') NOT NULL DEFAULT 'PENDING';
ALTER TABLE projects MODIFY status ENUM('PLANNED','IN_PROGRESS','DELAYED','COMPLETED','COMPLETED_LATE','CANCELLED') NOT NULL DEFAULT 'PLANNED';

ALTER TABLE milestones ADD COLUMN claim ENUM('IN_PROGRESS','COMPLETED') NULL;
ALTER TABLE projects ADD COLUMN original_end_date DATE NULL;

UPDATE projects SET original_end_date = end_date;

-- Phase 1 completed every milestone that had a live proof, so each of those proofs becomes a COMPLETED claim.
UPDATE milestones m SET m.claim = 'COMPLETED'
WHERE EXISTS (SELECT 1 FROM milestone_attachments a WHERE a.milestone_id = m.id AND a.deleted_at IS NULL);
