-- Sequenza indipendente per gli eventi prodotti autorevolmente dal backend.
ALTER TABLE game_schema.game_sessions
    ADD COLUMN server_event_sequence BIGINT NOT NULL DEFAULT 0;

ALTER TABLE game_schema.game_sessions
    ADD CONSTRAINT ck_game_sessions_server_event_sequence
        CHECK (server_event_sequence >= 0);

-- Produttori diversi possono usare lo stesso numero senza collidere.
DROP INDEX IF EXISTS game_schema.uq_game_events_session_sequence;

CREATE UNIQUE INDEX uq_game_events_session_source_sequence
    ON game_schema.game_events (session_id, source, sequence_number);
