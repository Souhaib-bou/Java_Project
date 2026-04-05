-- Adds recruiter/admin ownership to onboarding plans so we can enforce role access.
ALTER TABLE onboardingplan
  ADD COLUMN IF NOT EXISTS created_by INT NULL;

ALTER TABLE onboardingplan
  ADD KEY IF NOT EXISTS idx_onboardingplan_created_by (created_by);

-- Add FK only if it doesn't exist (MariaDB doesn't support IF NOT EXISTS on CONSTRAINT easily).
-- Safe approach: try add, ignore if already present.
SET @fk_exists := (
  SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'onboardingplan'
    AND CONSTRAINT_NAME = 'fk_onboardingplan_created_by'
);
SET @sql := IF(@fk_exists = 0,
  'ALTER TABLE onboardingplan ADD CONSTRAINT fk_onboardingplan_created_by FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE SET NULL ON UPDATE CASCADE',
  'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
