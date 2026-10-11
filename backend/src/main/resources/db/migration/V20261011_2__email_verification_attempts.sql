ALTER TABLE email_verification ADD COLUMN IF NOT EXISTS failed_attempts INT NOT NULL DEFAULT 0;
