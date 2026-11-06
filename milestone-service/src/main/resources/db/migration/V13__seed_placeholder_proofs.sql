-- Seed-only. Status is now derived from proofs, so the demo's hand-written COMPLETED milestones
-- would all fall back to PENDING at startup. This gives each one a placeholder proof so the demo
-- keeps its spread of statuses. Scoped to the demo agency so a real deployment's data is never touched.
-- Seeded IN_PROGRESS milestones will still drop to PENDING in phase 1 (known limit: one proof completes a milestone).
INSERT INTO milestone_attachments (milestone_id, original_filename, stored_filename, content_type, file_size, uploaded_by)
SELECT m.id, 'placeholder-proof.pdf', CONCAT('seed-proof-', m.id, '.pdf'), 'application/pdf', 1024, u.id
FROM milestones m
JOIN projects p ON p.id = m.project_id
JOIN users u ON u.id = p.agency_id
WHERE m.status = 'COMPLETED' AND m.deleted_at IS NULL AND u.username = 'doh_agency'
  AND NOT EXISTS (SELECT 1 FROM milestone_attachments a WHERE a.milestone_id = m.id);
