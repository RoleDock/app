--liquibase formatted sql

--changeset roledock:0003-job-offers
CREATE TABLE job_offer (
    id UUID PRIMARY KEY,
    original_text TEXT NOT NULL,
    source_url VARCHAR(2000),
    analyzed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    review_status VARCHAR(20) DEFAULT 'UNREVIEWED' NOT NULL
        CHECK (review_status IN ('UNREVIEWED', 'CONFIRMED', 'CORRECTED')),
    initial_extraction TEXT NOT NULL,
    company TEXT,
    position TEXT,
    source_language TEXT,
    summary TEXT,
    city TEXT,
    region TEXT,
    country TEXT,
    contract_type VARCHAR(30) NOT NULL,
    work_arrangement_type VARCHAR(20) NOT NULL,
    remote_area TEXT,
    on_site_days_per_week INTEGER CHECK (on_site_days_per_week BETWEEN 0 AND 7)
);
CREATE TABLE job_offer_mission (
    job_offer_id UUID NOT NULL REFERENCES job_offer(id) ON DELETE CASCADE,
    sort_order INTEGER NOT NULL CHECK (sort_order >= 0),
    mission TEXT NOT NULL,
    PRIMARY KEY (job_offer_id, sort_order)
);
CREATE TABLE job_requirement (
    id UUID PRIMARY KEY,
    job_offer_id UUID NOT NULL REFERENCES job_offer(id) ON DELETE CASCADE,
    sort_order INTEGER NOT NULL CHECK (sort_order >= 0),
    raw_text TEXT NOT NULL,
    canonical_label TEXT NOT NULL,
    category VARCHAR(30) NOT NULL,
    requirement_kind VARCHAR(20) NOT NULL,
    centrality VARCHAR(20) NOT NULL,
    explicitness VARCHAR(20) NOT NULL,
    hard_blocker_candidate BOOLEAN NOT NULL,
    blocker_condition TEXT,
    constraint_operator VARCHAR(20),
    constraint_value TEXT,
    constraint_unit TEXT,
    extraction_confidence VARCHAR(20) NOT NULL,
    UNIQUE (job_offer_id, sort_order),
    CHECK ((constraint_operator IS NULL AND constraint_value IS NULL AND constraint_unit IS NULL)
        OR (constraint_operator IS NOT NULL AND constraint_value IS NOT NULL)),
    CHECK ((hard_blocker_candidate = FALSE AND blocker_condition IS NULL)
        OR (hard_blocker_candidate = TRUE AND blocker_condition IS NOT NULL
            AND requirement_kind = 'REQUIRED' AND explicitness = 'EXPLICIT'))
);
