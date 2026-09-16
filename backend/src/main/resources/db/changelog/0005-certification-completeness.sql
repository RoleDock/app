--liquibase formatted sql
--changeset roledock:0005-certification-completeness
ALTER TABLE candidate_profile ADD COLUMN certifications_complete BOOLEAN NOT NULL DEFAULT FALSE;
