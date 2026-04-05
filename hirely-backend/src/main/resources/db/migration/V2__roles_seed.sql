-- Ensure core roles exist.
INSERT INTO role(name, description, status, default_dashboard)
SELECT 'admin', 'Full access', 'active', 'admin'
WHERE NOT EXISTS (SELECT 1 FROM role WHERE name='admin');

INSERT INTO role(name, description, status, default_dashboard)
SELECT 'recruiter', 'Recruiter access', 'active', 'recruiter'
WHERE NOT EXISTS (SELECT 1 FROM role WHERE name='recruiter');

INSERT INTO role(name, description, status, default_dashboard)
SELECT 'candidate', 'Candidate access', 'active', 'candidate'
WHERE NOT EXISTS (SELECT 1 FROM role WHERE name='candidate');
