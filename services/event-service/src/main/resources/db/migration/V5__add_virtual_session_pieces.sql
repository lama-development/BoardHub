-- Associazione persistente tra personaggi e pedine virtuali presenti sulla plancia.
ALTER TABLE game_schema.characters
    ADD CONSTRAINT uq_character_session_owner
        UNIQUE (character_id, session_id, participant_id);

CREATE TABLE game_schema.session_pieces (
    session_piece_id UUID PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL,
    character_id UUID NOT NULL,
    participant_id UUID NOT NULL,
    representation_mode VARCHAR(20) NOT NULL,
    current_cell VARCHAR(10) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_session_pieces_character_owner
        FOREIGN KEY (character_id, session_id, participant_id)
        REFERENCES game_schema.characters (character_id, session_id, participant_id)
        ON DELETE CASCADE,
    CONSTRAINT uq_session_piece_character
        UNIQUE (session_id, character_id),
    CONSTRAINT uq_session_piece_cell
        UNIQUE (session_id, current_cell),
    CONSTRAINT ck_session_piece_representation
        CHECK (representation_mode = 'VIRTUAL'),
    CONSTRAINT ck_session_piece_version
        CHECK (version >= 0)
);

CREATE INDEX idx_session_pieces_owner
    ON game_schema.session_pieces (session_id, participant_id, created_at);

CREATE INDEX idx_session_pieces_session
    ON game_schema.session_pieces (session_id, created_at);
