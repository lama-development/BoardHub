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

DROP INDEX IF EXISTS game_schema.idx_game_events_session_sequence;

CREATE UNIQUE INDEX IF NOT EXISTS uq_game_events_session_sequence
    ON game_schema.game_events (session_id, sequence_number);

CREATE TABLE IF NOT EXISTS game_schema.game_sessions (
    session_id VARCHAR(100) PRIMARY KEY,
    venue_id VARCHAR(80) NOT NULL,
    table_id VARCHAR(80) NOT NULL,
    title VARCHAR(150) NOT NULL,
    game_type VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    grid_width INTEGER NOT NULL,
    grid_height INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS game_schema.game_grid_cells (
    session_id VARCHAR(100) NOT NULL,
    cell VARCHAR(10) NOT NULL,
    terrain_type VARCHAR(40) NOT NULL,
    occupied_by VARCHAR(100),
    PRIMARY KEY (session_id, cell),
    CONSTRAINT fk_grid_cells_session
        FOREIGN KEY (session_id)
        REFERENCES game_schema.game_sessions (session_id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS game_schema.game_grid_walls (
    session_id VARCHAR(100) NOT NULL,
    cell VARCHAR(10) NOT NULL,
    direction VARCHAR(40) NOT NULL,
    PRIMARY KEY (session_id, cell, direction),
    CONSTRAINT fk_grid_walls_session
        FOREIGN KEY (session_id)
        REFERENCES game_schema.game_sessions (session_id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS game_schema.game_grid_traps (
    session_id VARCHAR(100) NOT NULL,
    trap_id VARCHAR(100) NOT NULL,
    cell VARCHAR(10) NOT NULL,
    visibility VARCHAR(40) NOT NULL,
    armed BOOLEAN NOT NULL,
    PRIMARY KEY (session_id, trap_id),
    CONSTRAINT fk_grid_traps_session
        FOREIGN KEY (session_id)
        REFERENCES game_schema.game_sessions (session_id)
        ON DELETE CASCADE
);
