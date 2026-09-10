--liquibase formatted sql

--changeset roledock:0004-job-offer-review
ALTER TABLE job_offer ADD COLUMN review_bypassed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE job_requirement ADD COLUMN source VARCHAR(20) DEFAULT 'LLM_EXTRACTED' NOT NULL;
ALTER TABLE job_requirement ALTER COLUMN raw_text DROP NOT NULL;
ALTER TABLE job_requirement ALTER COLUMN extraction_confidence DROP NOT NULL;
ALTER TABLE job_requirement ADD CONSTRAINT requirement_source_check CHECK (
    (source = 'LLM_EXTRACTED' AND raw_text IS NOT NULL AND extraction_confidence IS NOT NULL)
    OR (source = 'USER_ADDED' AND raw_text IS NULL AND extraction_confidence IS NULL)
);
