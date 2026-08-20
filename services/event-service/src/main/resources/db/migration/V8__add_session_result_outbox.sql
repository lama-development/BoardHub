-- Consegna affidabile dei risultati conclusivi verso il servizio statistiche.
CREATE TABLE game_schema.session_result_outbox (
    fact_id VARCHAR(140) PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL UNIQUE,
    topic VARCHAR(255) NOT NULL,
    payload_json TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    last_error VARCHAR(500),
    CONSTRAINT fk_result_outbox_session
        FOREIGN KEY (session_id) REFERENCES game_schema.game_sessions(session_id),
    CONSTRAINT ck_result_outbox_status
        CHECK (status IN ('PENDING', 'PUBLISHED')),
    CONSTRAINT ck_result_outbox_attempt_count
        CHECK (attempt_count >= 0)
);

CREATE INDEX idx_session_result_outbox_due
    ON game_schema.session_result_outbox (status, next_attempt_at, created_at);
