-- Platform-wide role model trimmed to ADMIN/AGENCY/PUBLIC (see milestone-service/TODO.md §7) — the
-- template's own V2 is already committed/applied elsewhere and can't be edited, so this follows up
-- as a new migration. No seed data to reassign here (the template ships none), but a real clone
-- that already let someone self-register as MANAGER would need the same reassignment first.
UPDATE users SET role = 'PUBLIC' WHERE role = 'MANAGER';

ALTER TABLE users MODIFY COLUMN role ENUM('ADMIN','AGENCY','PUBLIC') NOT NULL DEFAULT 'PUBLIC';
