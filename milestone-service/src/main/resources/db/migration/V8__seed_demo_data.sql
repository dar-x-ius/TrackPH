-- Demo/seed data for a fresh machine. Password for all three accounts is "Demo@12345"
-- (see milestone-service/TODO.md) — seed-only credentials, never for a real deployment.
INSERT INTO users (username, email, password_hash, role, full_name, agency_name, is_active) VALUES
('admin_demo', 'admin@trackph.demo',  '$2a$10$lQPzyUJ3942PZq5eQW2apOuRx07fmQomitLdb7g5.cdysD4Q6VG7W', 'ADMIN',   'Demo Admin',        NULL,                     TRUE),
('doh_agency', 'agency@trackph.demo', '$2a$10$lQPzyUJ3942PZq5eQW2apOuRx07fmQomitLdb7g5.cdysD4Q6VG7W', 'AGENCY',  'DOH Field Officer', 'Department of Health',   TRUE),
('pm_manager', 'manager@trackph.demo','$2a$10$lQPzyUJ3942PZq5eQW2apOuRx07fmQomitLdb7g5.cdysD4Q6VG7W', 'MANAGER', 'Project Manager',   NULL,                     TRUE);

-- Seven projects, deliberately covering every Project.status value (one PLANNED, two IN_PROGRESS,
-- two DELAYED, one COMPLETED, one CANCELLED) so the dashboard and status filters have real variety
-- to show on a fresh machine instead of one happy-path example.

INSERT INTO projects (name, description, agency_id, status, start_date, end_date, location, budget)
SELECT 'Barangay Health Center Renovation', 'Renovation and equipment upgrade of the barangay health center, including a new consultation wing.', id, 'IN_PROGRESS', '2026-01-15', '2026-12-31', 'Quezon City', 4500000.00
FROM users WHERE username = 'doh_agency';

INSERT INTO projects (name, description, agency_id, status, start_date, end_date, location, budget)
SELECT 'Rural Farm-to-Market Road', 'Construction of a 12km farm-to-market road connecting upland barangays to the municipal market.', id, 'DELAYED', '2025-08-01', '2026-06-30', 'Nueva Ecija', 12000000.00
FROM users WHERE username = 'doh_agency';

INSERT INTO projects (name, description, agency_id, status, start_date, end_date, location, budget)
SELECT 'Coastal Seawall Construction', 'Seawall and breakwater construction to protect low-lying coastal barangays from storm surge.', id, 'PLANNED', '2027-01-01', '2027-12-31', 'Infanta, Quezon', 8500000.00
FROM users WHERE username = 'doh_agency';

INSERT INTO projects (name, description, agency_id, status, start_date, end_date, location, budget)
SELECT 'Public Market Modernization', 'Demolition and rebuild of the old public market into a modern, ventilated facility.', id, 'COMPLETED', '2025-03-01', '2026-03-01', 'Tarlac City', 6200000.00
FROM users WHERE username = 'doh_agency';

INSERT INTO projects (name, description, agency_id, status, start_date, end_date, location, budget)
SELECT 'Elevated Busway Extension', 'Extension of the elevated busway corridor; shelved after the feasibility study due to budget reallocation.', id, 'CANCELLED', '2025-06-01', '2026-12-31', 'Metro Manila', 45000000.00
FROM users WHERE username = 'doh_agency';

INSERT INTO projects (name, description, agency_id, status, start_date, end_date, location, budget)
SELECT 'Solar-Powered Streetlight Installation', 'Replacement of aging streetlights with solar-powered fixtures along major barangay roads.', id, 'IN_PROGRESS', '2026-06-01', '2027-02-28', 'Davao City', 3200000.00
FROM users WHERE username = 'doh_agency';

INSERT INTO projects (name, description, agency_id, status, start_date, end_date, location, budget)
SELECT 'Flood Control Drainage Upgrade', 'Upgrade of undersized drainage culverts and a new pump station in a flood-prone district.', id, 'DELAYED', '2025-11-01', '2026-09-30', 'Marikina City', 15000000.00
FROM users WHERE username = 'doh_agency';

-- Milestones per project, via INSERT ... SELECT on project name (not hardcoded IDs), deliberately
-- varied so PENDING/IN_PROGRESS/COMPLETED/OVERDUE all show up somewhere.

INSERT INTO milestones (project_id, title, description, due_date, completed_date, status, sequence_order)
SELECT id, 'Site Assessment and Permitting', 'Survey the site and secure building permits.', '2026-02-15', '2026-02-10', 'COMPLETED', 1
FROM projects WHERE name = 'Barangay Health Center Renovation';

INSERT INTO milestones (project_id, title, description, due_date, completed_date, status, sequence_order)
SELECT id, 'Foundation and Structural Work', 'Lay the foundation and erect the structural frame of the new wing.', '2026-05-30', '2026-06-02', 'COMPLETED', 2
FROM projects WHERE name = 'Barangay Health Center Renovation';

INSERT INTO milestones (project_id, title, description, due_date, completed_date, status, sequence_order)
SELECT id, 'Interior Fit-out and Equipment Installation', 'Install medical equipment, plumbing, and electrical fixtures.', '2026-11-15', NULL, 'IN_PROGRESS', 3
FROM projects WHERE name = 'Barangay Health Center Renovation';

INSERT INTO milestones (project_id, title, description, due_date, completed_date, status, sequence_order)
SELECT id, 'Final Inspection and Turnover', 'Final government inspection and handover to the barangay health office.', '2026-12-20', NULL, 'PENDING', 4
FROM projects WHERE name = 'Barangay Health Center Renovation';

INSERT INTO milestones (project_id, title, description, due_date, completed_date, status, sequence_order)
SELECT id, 'Road Survey and Design', 'Topographic survey and final road design approval.', '2025-10-01', '2025-09-28', 'COMPLETED', 1
FROM projects WHERE name = 'Rural Farm-to-Market Road';

INSERT INTO milestones (project_id, title, description, due_date, completed_date, status, sequence_order)
SELECT id, 'Earthworks and Grading', 'Clearing, grading, and compaction of the road alignment.', '2026-08-15', NULL, 'OVERDUE', 2
FROM projects WHERE name = 'Rural Farm-to-Market Road';

INSERT INTO milestones (project_id, title, description, due_date, completed_date, status, sequence_order)
SELECT id, 'Paving and Drainage Installation', 'Asphalt paving and construction of roadside drainage.', '2027-01-31', NULL, 'PENDING', 3
FROM projects WHERE name = 'Rural Farm-to-Market Road';

INSERT INTO milestones (project_id, title, description, due_date, completed_date, status, sequence_order)
SELECT id, 'Environmental Impact Assessment', 'Environmental clearance for coastal construction work.', '2027-02-28', NULL, 'PENDING', 1
FROM projects WHERE name = 'Coastal Seawall Construction';

INSERT INTO milestones (project_id, title, description, due_date, completed_date, status, sequence_order)
SELECT id, 'Material Procurement and Bidding', 'Public bidding for seawall construction materials and contractor.', '2027-04-30', NULL, 'PENDING', 2
FROM projects WHERE name = 'Coastal Seawall Construction';

INSERT INTO milestones (project_id, title, description, due_date, completed_date, status, sequence_order)
SELECT id, 'Seawall Construction Phase 1', 'First 2km segment of seawall and breakwater construction.', '2027-09-30', NULL, 'PENDING', 3
FROM projects WHERE name = 'Coastal Seawall Construction';

INSERT INTO milestones (project_id, title, description, due_date, completed_date, status, sequence_order)
SELECT id, 'Demolition of Old Market Structure', 'Teardown and clearing of the original market building.', '2025-04-15', '2025-04-12', 'COMPLETED', 1
FROM projects WHERE name = 'Public Market Modernization';

INSERT INTO milestones (project_id, title, description, due_date, completed_date, status, sequence_order)
SELECT id, 'New Market Building Construction', 'Construction of the new modernized market building.', '2025-11-30', '2025-12-05', 'COMPLETED', 2
FROM projects WHERE name = 'Public Market Modernization';

INSERT INTO milestones (project_id, title, description, due_date, completed_date, status, sequence_order)
SELECT id, 'Vendor Relocation and Turnover', 'Relocate vendors back in and formally turn over the facility.', '2026-02-28', '2026-02-20', 'COMPLETED', 3
FROM projects WHERE name = 'Public Market Modernization';

INSERT INTO milestones (project_id, title, description, due_date, completed_date, status, sequence_order)
SELECT id, 'Feasibility Study', 'Ridership and cost-benefit study for the busway extension.', '2025-08-31', '2025-08-25', 'COMPLETED', 1
FROM projects WHERE name = 'Elevated Busway Extension';

INSERT INTO milestones (project_id, title, description, due_date, completed_date, status, sequence_order)
SELECT id, 'Right-of-Way Acquisition', 'Acquire right-of-way along the proposed extension route.', '2026-03-31', NULL, 'PENDING', 2
FROM projects WHERE name = 'Elevated Busway Extension';

INSERT INTO milestones (project_id, title, description, due_date, completed_date, status, sequence_order)
SELECT id, 'Site Survey and Pole Mapping', 'Survey existing streetlight poles and map replacement sites.', '2026-07-15', '2026-07-10', 'COMPLETED', 1
FROM projects WHERE name = 'Solar-Powered Streetlight Installation';

INSERT INTO milestones (project_id, title, description, due_date, completed_date, status, sequence_order)
SELECT id, 'Solar Fixture Installation', 'Install solar-powered fixtures along major barangay roads.', '2026-11-30', NULL, 'IN_PROGRESS', 2
FROM projects WHERE name = 'Solar-Powered Streetlight Installation';

INSERT INTO milestones (project_id, title, description, due_date, completed_date, status, sequence_order)
SELECT id, 'System Testing and Commissioning', 'Test and commission the solar lighting control system.', '2027-01-31', NULL, 'PENDING', 3
FROM projects WHERE name = 'Solar-Powered Streetlight Installation';

INSERT INTO milestones (project_id, title, description, due_date, completed_date, status, sequence_order)
SELECT id, 'Drainage Design and Permitting', 'Engineering design and permits for the drainage upgrade.', '2025-12-15', '2025-12-10', 'COMPLETED', 1
FROM projects WHERE name = 'Flood Control Drainage Upgrade';

INSERT INTO milestones (project_id, title, description, due_date, completed_date, status, sequence_order)
SELECT id, 'Culvert Excavation and Installation', 'Excavate and install upsized drainage culverts.', '2026-07-31', NULL, 'OVERDUE', 2
FROM projects WHERE name = 'Flood Control Drainage Upgrade';

INSERT INTO milestones (project_id, title, description, due_date, completed_date, status, sequence_order)
SELECT id, 'Pump Station Installation', 'Install the new drainage pump station.', '2026-12-31', NULL, 'PENDING', 3
FROM projects WHERE name = 'Flood Control Drainage Upgrade';
