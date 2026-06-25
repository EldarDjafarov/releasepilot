-- ReleasePilot — full schema

CREATE TABLE promotions (
    id                  UUID         PRIMARY KEY,
    application_id      VARCHAR(255) NOT NULL,
    version             VARCHAR(100) NOT NULL,
    target_environment  VARCHAR(20)  NOT NULL CHECK (target_environment IN ('DEV', 'STAGING', 'PRODUCTION')),
    status              VARCHAR(20)  NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'IN_PROGRESS', 'COMPLETED', 'ROLLED_BACK', 'CANCELLED')),
    requested_by        VARCHAR(255) NOT NULL,
    requested_at        TIMESTAMPTZ  NOT NULL,
    approved_by         VARCHAR(255),
    approved_at         TIMESTAMPTZ,
    completion_notes    TEXT,
    rollback_reason     TEXT,
    cancellation_reason TEXT,
    release_notes       TEXT,
    updated_at          TIMESTAMPTZ
);

-- One active promotion per application + environment
CREATE UNIQUE INDEX uq_one_active_promotion_per_app_env
    ON promotions (application_id, target_environment)
    WHERE status IN ('PENDING', 'APPROVED', 'IN_PROGRESS');

CREATE INDEX idx_promotions_application_id
    ON promotions (application_id, requested_at DESC);

-- Structured event store
CREATE TABLE promotion_events (
    id                  UUID         PRIMARY KEY,
    promotion_id        UUID         NOT NULL REFERENCES promotions(id),
    application_id      VARCHAR(255) NOT NULL,
    target_environment  VARCHAR(20)  NOT NULL,
    event_type          VARCHAR(50)  NOT NULL,
    acting_user         VARCHAR(255) NOT NULL,
    occurred_at         TIMESTAMPTZ  NOT NULL,
    reason              TEXT
);

CREATE INDEX idx_promotion_events_promotion_id
    ON promotion_events (promotion_id, occurred_at ASC);

CREATE INDEX idx_promotion_events_application_id
    ON promotion_events (application_id, occurred_at DESC);

-- Transactional outbox
CREATE TABLE outbox_events (
    id              UUID        PRIMARY KEY,
    promotion_id    UUID        NOT NULL,
    event_type      VARCHAR(50) NOT NULL,
    acting_user     VARCHAR(255) NOT NULL,
    occurred_at     TIMESTAMPTZ NOT NULL,
    payload         TEXT        NOT NULL,
    status          VARCHAR(10) NOT NULL DEFAULT 'PENDING'
                                CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at    TIMESTAMPTZ
);

CREATE INDEX idx_outbox_events_status ON outbox_events (status, created_at ASC)
    WHERE status = 'PENDING';
