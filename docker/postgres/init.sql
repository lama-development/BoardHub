-- Creazione schemi isolati
CREATE SCHEMA IF NOT EXISTS venue_schema;
CREATE SCHEMA IF NOT EXISTS game_schema;
CREATE SCHEMA IF NOT EXISTS stats_schema;

CREATE TABLE IF NOT EXISTS game_schema.game_events (
    event_id VARCHAR(80) PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    venue_id VARCHAR(80) NOT NULL,
    table_id VARCHAR(80) NOT NULL,
    session_id VARCHAR(100) NOT NULL,
    source VARCHAR(50) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    sequence_number BIGINT NOT NULL,
    payload_json TEXT NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_game_events_session_sequence
    ON game_schema.game_events (session_id, sequence_number);
