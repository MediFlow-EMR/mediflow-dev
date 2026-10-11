-- Versioned migration applied by Flyway before Hibernate validates the schema.
-- Existing MediFlow schema is required; no tables or clinical data are dropped.
ALTER TABLE handover ADD COLUMN IF NOT EXISTS source_type VARCHAR(20) NOT NULL DEFAULT 'AI';
ALTER TABLE handover ADD COLUMN IF NOT EXISTS final_summary TEXT NULL;
ALTER TABLE handover ADD COLUMN IF NOT EXISTS ai_model VARCHAR(100) NULL;
ALTER TABLE handover ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'APPROVED';
ALTER TABLE handover ADD COLUMN IF NOT EXISTS approved_at DATETIME(6) NULL;
ALTER TABLE handover ADD COLUMN IF NOT EXISTS version BIGINT NULL;

-- Legacy approved records keep their exact original content.
UPDATE handover SET final_summary = ai_summary WHERE final_summary IS NULL;
UPDATE handover SET version = 0 WHERE version IS NULL;

CREATE TABLE IF NOT EXISTS handover_revision (
    revision_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    handover_id BIGINT NOT NULL,
    edited_by_id BIGINT NOT NULL,
    edited_by_name VARCHAR(100) NOT NULL,
    recorded_at DATETIME(6) NOT NULL,
    status VARCHAR(20) NOT NULL,
    source_type VARCHAR(20) NOT NULL,
    ai_summary TEXT,
    final_summary TEXT,
    ai_model VARCHAR(100),
    record_version BIGINT,
    INDEX idx_handover_revision (handover_id, revision_id)
);
