-- MANAGER turned out to have almost no distinct permissions once ownership checks were added to
-- AGENCY (see SecurityConfig/MilestoneService) — ADMIN and AGENCY already cover the paper's two
-- formal DFD actors (Agency and User), so MANAGER is dropped rather than kept as a role with no
-- clear identity. The seeded pm_manager account becomes a PUBLIC demo login instead, since there
-- wasn't one before.
UPDATE users SET username = 'citizen_demo', email = 'citizen@trackph.demo', role = 'PUBLIC', full_name = 'Demo Citizen', agency_name = NULL
WHERE username = 'pm_manager';

-- Catch-all for any other MANAGER row (e.g. a real deployment where someone self-registered as one)
-- so the narrower enum below never has an existing value it can no longer represent.
UPDATE users SET role = 'PUBLIC' WHERE role = 'MANAGER';

ALTER TABLE users MODIFY COLUMN role ENUM('ADMIN','AGENCY','PUBLIC') NOT NULL DEFAULT 'PUBLIC';
