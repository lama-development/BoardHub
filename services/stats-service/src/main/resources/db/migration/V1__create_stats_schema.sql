-- Schema del servizio statistiche. Appartiene esclusivamente a stats-service:
-- nessun altro servizio deve leggerlo o scriverlo.
CREATE TABLE IF NOT EXISTS stats_schema.session_results (
    session_id VARCHAR(100) PRIMARY KEY,
    venue_id VARCHAR(80) NOT NULL,
    table_id VARCHAR(80) NOT NULL,
    title VARCHAR(150) NOT NULL,
    game_type VARCHAR(40) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at TIMESTAMP WITH TIME ZONE NOT NULL,
    duration_minutes BIGINT NOT NULL,
    participant_count INTEGER NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_session_results_duration CHECK (duration_minutes >= 0),
    CONSTRAINT ck_session_results_participants CHECK (participant_count >= 0)
);

CREATE INDEX IF NOT EXISTS idx_session_results_game_type
    ON stats_schema.session_results (game_type, venue_id, ended_at);

CREATE TABLE IF NOT EXISTS stats_schema.player_results (
    session_id VARCHAR(100) NOT NULL,
    player_reference VARCHAR(100) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    character_name VARCHAR(80),
    class_name VARCHAR(80),
    species VARCHAR(80),
    level INTEGER NOT NULL DEFAULT 0,
    survived BOOLEAN NOT NULL,
    moves_confirmed INTEGER NOT NULL DEFAULT 0,
    cells_travelled INTEGER NOT NULL DEFAULT 0,
    traps_triggered INTEGER NOT NULL DEFAULT 0,
    saves_succeeded INTEGER NOT NULL DEFAULT 0,
    saves_failed INTEGER NOT NULL DEFAULT 0,
    damage_taken INTEGER NOT NULL DEFAULT 0,
    points INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (session_id, player_reference),
    CONSTRAINT fk_player_results_session
        FOREIGN KEY (session_id)
        REFERENCES stats_schema.session_results (session_id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_player_results_player
    ON stats_schema.player_results (player_reference);

CREATE TABLE IF NOT EXISTS stats_schema.tournaments (
    tournament_id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    game_type VARCHAR(40) NOT NULL,
    venue_id VARCHAR(80) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS stats_schema.tournament_sessions (
    tournament_id UUID NOT NULL,
    session_id VARCHAR(100) NOT NULL,
    added_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (tournament_id, session_id),
    CONSTRAINT fk_tournament_sessions_tournament
        FOREIGN KEY (tournament_id)
        REFERENCES stats_schema.tournaments (tournament_id)
        ON DELETE CASCADE
);
