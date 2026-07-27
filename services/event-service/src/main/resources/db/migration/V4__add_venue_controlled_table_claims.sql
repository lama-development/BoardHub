-- Abilitazione temporanea dei tavoli da parte del locale.
ALTER TABLE game_schema.game_tables
    ADD COLUMN claim_expires_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE game_schema.game_tables
    DROP CONSTRAINT ck_game_tables_status;

UPDATE game_schema.game_tables
SET status = CASE
    WHEN active_session_id IS NULL THEN 'DISABLED'
    ELSE 'IN_SESSION'
END;

ALTER TABLE game_schema.game_tables
    ADD CONSTRAINT ck_game_tables_status
        CHECK (status IN ('DISABLED', 'CLAIMABLE', 'IN_SESSION'));

ALTER TABLE game_schema.game_tables
    ADD CONSTRAINT ck_game_tables_runtime_state
        CHECK (
            (status = 'DISABLED' AND active_session_id IS NULL AND claim_expires_at IS NULL)
            OR
            (status = 'CLAIMABLE' AND active_session_id IS NULL AND claim_expires_at IS NOT NULL)
            OR
            (status = 'IN_SESSION' AND active_session_id IS NOT NULL AND claim_expires_at IS NULL)
        );

CREATE INDEX idx_game_tables_claim_expiry
    ON game_schema.game_tables (status, claim_expires_at);
