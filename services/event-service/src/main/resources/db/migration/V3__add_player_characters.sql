-- Personaggi controllati dai partecipanti della sessione.
ALTER TABLE game_schema.session_participants
    ADD CONSTRAINT uq_participant_session_identity
        UNIQUE (participant_id, session_id);

CREATE TABLE game_schema.characters (
    character_id UUID PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL,
    participant_id UUID NOT NULL,
    name VARCHAR(80) NOT NULL,
    species VARCHAR(80) NOT NULL,
    age INTEGER,
    class_name VARCHAR(80) NOT NULL,
    level INTEGER NOT NULL,
    speed_cells INTEGER NOT NULL,
    hp_current INTEGER NOT NULL,
    hp_max INTEGER NOT NULL,
    armor_class INTEGER NOT NULL,
    party_visibility VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_characters_participant_session
        FOREIGN KEY (participant_id, session_id)
        REFERENCES game_schema.session_participants (participant_id, session_id)
        ON DELETE CASCADE,
    CONSTRAINT ck_character_age
        CHECK (age IS NULL OR age > 0),
    CONSTRAINT ck_character_level
        CHECK (level BETWEEN 1 AND 20),
    CONSTRAINT ck_character_speed
        CHECK (speed_cells BETWEEN 0 AND 100),
    CONSTRAINT ck_character_hp
        CHECK (
            hp_max BETWEEN 1 AND 1000000
            AND hp_current BETWEEN 0 AND hp_max
        ),
    CONSTRAINT ck_character_armor_class
        CHECK (armor_class BETWEEN 0 AND 100),
    CONSTRAINT ck_character_visibility
        CHECK (party_visibility IN ('OWNER_ONLY', 'PARTY', 'DM_ONLY')),
    CONSTRAINT ck_character_version
        CHECK (version >= 0)
);

CREATE INDEX idx_characters_participant
    ON game_schema.characters (session_id, participant_id, created_at);

CREATE INDEX idx_characters_session
    ON game_schema.characters (session_id, created_at);
