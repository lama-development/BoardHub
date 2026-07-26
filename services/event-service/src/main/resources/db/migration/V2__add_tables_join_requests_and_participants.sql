-- Tavoli riutilizzabili, ingresso controllato dal DM e partecipanti della sessione.
ALTER TABLE game_schema.game_sessions
    ADD COLUMN IF NOT EXISTS public_summary VARCHAR(500) NOT NULL DEFAULT '';

ALTER TABLE game_schema.game_sessions
    ADD COLUMN IF NOT EXISTS accepting_join_requests BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE game_schema.game_tables (
    table_id VARCHAR(80) PRIMARY KEY,
    table_public_id VARCHAR(100) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    active_session_id VARCHAR(100) UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_game_tables_status
        CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT fk_game_tables_active_session
        FOREIGN KEY (active_session_id)
        REFERENCES game_schema.game_sessions (session_id)
        ON DELETE SET NULL
);

CREATE TABLE game_schema.session_join_requests (
    request_id UUID PRIMARY KEY,
    idempotency_key UUID NOT NULL UNIQUE,
    request_fingerprint CHAR(64) NOT NULL,
    session_id VARCHAR(100) NOT NULL,
    player_reference VARCHAR(100) NOT NULL,
    pending_player_reference VARCHAR(100),
    display_name VARCHAR(80) NOT NULL,
    status VARCHAR(20) NOT NULL,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_join_requests_session
        FOREIGN KEY (session_id)
        REFERENCES game_schema.game_sessions (session_id)
        ON DELETE CASCADE,
    CONSTRAINT uq_pending_join_request
        UNIQUE (session_id, pending_player_reference),
    CONSTRAINT ck_join_request_status
        CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'EXPIRED')),
    CONSTRAINT ck_join_request_expiry
        CHECK (expires_at > requested_at),
    CONSTRAINT ck_join_request_resolution
        CHECK (
            (status = 'PENDING' AND resolved_at IS NULL AND pending_player_reference = player_reference)
            OR
            (status <> 'PENDING' AND resolved_at IS NOT NULL AND pending_player_reference IS NULL)
        )
);

CREATE INDEX idx_join_requests_session_status
    ON game_schema.session_join_requests (session_id, status, requested_at);

CREATE INDEX idx_join_requests_player_rate
    ON game_schema.session_join_requests (session_id, player_reference, requested_at);

CREATE INDEX idx_join_requests_pending_expiry
    ON game_schema.session_join_requests (status, expires_at);

CREATE TABLE game_schema.session_participants (
    participant_id UUID PRIMARY KEY,
    join_request_id UUID UNIQUE,
    session_id VARCHAR(100) NOT NULL,
    player_reference VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    status VARCHAR(20) NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL,
    left_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_participants_join_request
        FOREIGN KEY (join_request_id)
        REFERENCES game_schema.session_join_requests (request_id),
    CONSTRAINT fk_participants_session
        FOREIGN KEY (session_id)
        REFERENCES game_schema.game_sessions (session_id)
        ON DELETE CASCADE,
    CONSTRAINT uq_participant_player
        UNIQUE (session_id, player_reference),
    CONSTRAINT ck_participant_role
        CHECK (role IN ('DM', 'PLAYER')),
    CONSTRAINT ck_participant_status
        CHECK (status IN ('ACTIVE', 'LEFT')),
    CONSTRAINT ck_participant_left_at
        CHECK (
            (status = 'ACTIVE' AND left_at IS NULL)
            OR
            (status = 'LEFT' AND left_at IS NOT NULL)
        )
);

CREATE INDEX idx_participants_session_status
    ON game_schema.session_participants (session_id, status, role);
