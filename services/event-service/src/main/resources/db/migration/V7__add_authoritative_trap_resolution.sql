-- Risoluzione autorevole delle trappole, tiri salvezza e controllo temporaneo del DM.
ALTER TABLE game_schema.game_grid_traps
    ADD COLUMN lifecycle_policy VARCHAR(20) NOT NULL DEFAULT 'ONE_SHOT';

ALTER TABLE game_schema.game_grid_traps
    ADD COLUMN lifecycle_state VARCHAR(30) NOT NULL DEFAULT 'ARMED';

ALTER TABLE game_schema.game_grid_traps
    ADD COLUMN save_ability VARCHAR(20) NOT NULL DEFAULT 'DEXTERITY';

ALTER TABLE game_schema.game_grid_traps
    ADD COLUMN save_dc INTEGER NOT NULL DEFAULT 10;

ALTER TABLE game_schema.game_grid_traps
    ADD COLUMN roll_mode VARCHAR(20) NOT NULL DEFAULT 'NORMAL';

ALTER TABLE game_schema.game_grid_traps
    ADD COLUMN damage_expression VARCHAR(30) NOT NULL DEFAULT '1d6';

ALTER TABLE game_schema.game_grid_traps
    ADD COLUMN success_damage VARCHAR(10) NOT NULL DEFAULT 'NONE';

ALTER TABLE game_schema.game_grid_traps
    ADD COLUMN success_movement VARCHAR(10) NOT NULL DEFAULT 'CONTINUE';

ALTER TABLE game_schema.game_grid_traps
    ADD COLUMN failure_damage VARCHAR(10) NOT NULL DEFAULT 'FULL';

ALTER TABLE game_schema.game_grid_traps
    ADD COLUMN failure_movement VARCHAR(10) NOT NULL DEFAULT 'STOP';

ALTER TABLE game_schema.game_grid_traps
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE game_schema.game_grid_traps
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE game_schema.game_grid_traps
SET visibility = 'KEEP_DETAILS_HIDDEN'
WHERE visibility = 'ALWAYS_HIDDEN';

UPDATE game_schema.game_grid_traps
SET lifecycle_state = CASE WHEN armed THEN 'ARMED' ELSE 'DISARMED' END;

ALTER TABLE game_schema.game_grid_traps
    ADD CONSTRAINT uq_grid_traps_cell UNIQUE (session_id, cell);

ALTER TABLE game_schema.game_grid_traps
    ADD CONSTRAINT ck_grid_traps_lifecycle_policy
        CHECK (lifecycle_policy IN ('ONE_SHOT', 'PERSISTENT'));

ALTER TABLE game_schema.game_grid_traps
    ADD CONSTRAINT ck_grid_traps_lifecycle_state
        CHECK (lifecycle_state IN ('ARMED', 'TRIGGERED_ACTIVE', 'SPENT', 'DISARMED'));

ALTER TABLE game_schema.game_grid_traps
    ADD CONSTRAINT ck_grid_traps_visibility_v7
        CHECK (visibility IN ('HIDDEN', 'REVEALED', 'KEEP_DETAILS_HIDDEN'));

ALTER TABLE game_schema.game_grid_traps
    ADD CONSTRAINT ck_grid_traps_save_ability
        CHECK (save_ability IN ('STRENGTH', 'DEXTERITY', 'CONSTITUTION', 'INTELLIGENCE', 'WISDOM', 'CHARISMA'));

ALTER TABLE game_schema.game_grid_traps
    ADD CONSTRAINT ck_grid_traps_save_dc CHECK (save_dc BETWEEN 1 AND 30);

ALTER TABLE game_schema.game_grid_traps
    ADD CONSTRAINT ck_grid_traps_roll_mode
        CHECK (roll_mode IN ('NORMAL', 'ADVANTAGE', 'DISADVANTAGE'));

ALTER TABLE game_schema.game_grid_traps
    ADD CONSTRAINT ck_grid_traps_damage_policy
        CHECK (success_damage IN ('NONE', 'HALF', 'FULL') AND failure_damage IN ('NONE', 'HALF', 'FULL'));

ALTER TABLE game_schema.game_grid_traps
    ADD CONSTRAINT ck_grid_traps_movement_policy
        CHECK (success_movement IN ('CONTINUE', 'STOP') AND failure_movement IN ('CONTINUE', 'STOP'));

ALTER TABLE game_schema.game_grid_traps
    ADD CONSTRAINT ck_grid_traps_version CHECK (version >= 0);

ALTER TABLE game_schema.characters
    ADD COLUMN strength_save INTEGER NOT NULL DEFAULT 0;

ALTER TABLE game_schema.characters
    ADD COLUMN dexterity_save INTEGER NOT NULL DEFAULT 0;

ALTER TABLE game_schema.characters
    ADD COLUMN constitution_save INTEGER NOT NULL DEFAULT 0;

ALTER TABLE game_schema.characters
    ADD COLUMN intelligence_save INTEGER NOT NULL DEFAULT 0;

ALTER TABLE game_schema.characters
    ADD COLUMN wisdom_save INTEGER NOT NULL DEFAULT 0;

ALTER TABLE game_schema.characters
    ADD COLUMN charisma_save INTEGER NOT NULL DEFAULT 0;

ALTER TABLE game_schema.characters
    ADD COLUMN tactical_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE game_schema.characters
    ADD CONSTRAINT ck_character_save_bonuses CHECK (
        strength_save BETWEEN -20 AND 20
        AND dexterity_save BETWEEN -20 AND 20
        AND constitution_save BETWEEN -20 AND 20
        AND intelligence_save BETWEEN -20 AND 20
        AND wisdom_save BETWEEN -20 AND 20
        AND charisma_save BETWEEN -20 AND 20
    );

ALTER TABLE game_schema.characters
    ADD CONSTRAINT ck_character_tactical_status
        CHECK (tactical_status IN ('ACTIVE', 'DOWNED'));

CREATE TABLE game_schema.trap_resolutions (
    resolution_id UUID PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL,
    trap_id VARCHAR(100) NOT NULL,
    session_piece_id UUID NOT NULL,
    character_id UUID NOT NULL,
    participant_id UUID NOT NULL,
    move_command_id UUID NOT NULL UNIQUE,
    expected_piece_version BIGINT NOT NULL,
    status VARCHAR(40) NOT NULL,
    from_cell VARCHAR(10) NOT NULL,
    requested_destination VARCHAR(10) NOT NULL,
    trigger_cell VARCHAR(10) NOT NULL,
    path_json TEXT NOT NULL,
    remaining_path_json TEXT NOT NULL,
    movement_budget INTEGER NOT NULL,
    cost_to_trigger INTEGER NOT NULL,
    movement_remaining INTEGER NOT NULL,
    observed_physical_cell VARCHAR(10),
    save_d20_first INTEGER,
    save_d20_second INTEGER,
    save_selected INTEGER,
    save_bonus INTEGER,
    save_total INTEGER,
    save_success BOOLEAN,
    damage_rolls_json TEXT,
    damage_total INTEGER,
    movement_decision VARCHAR(10),
    roll_command_id UUID UNIQUE,
    roll_fingerprint CHAR(64),
    continue_command_id UUID UNIQUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_trap_resolution_trap
        FOREIGN KEY (session_id, trap_id)
        REFERENCES game_schema.game_grid_traps (session_id, trap_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_trap_resolution_piece
        FOREIGN KEY (session_piece_id)
        REFERENCES game_schema.session_pieces (session_piece_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_trap_resolution_character
        FOREIGN KEY (character_id)
        REFERENCES game_schema.characters (character_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_trap_resolution_participant
        FOREIGN KEY (participant_id)
        REFERENCES game_schema.session_participants (participant_id)
        ON DELETE CASCADE,
    CONSTRAINT ck_trap_resolution_status CHECK (status IN (
        'AWAITING_SAVE_ROLL', 'CONTINUATION_ALLOWED', 'CORRECTION_REQUIRED',
        'COMPLETED', 'CANCELLED_SESSION_ENDED'
    )),
    CONSTRAINT ck_trap_resolution_budget CHECK (
        movement_budget >= 0 AND cost_to_trigger >= 0
        AND movement_remaining >= 0 AND cost_to_trigger <= movement_budget
    ),
    CONSTRAINT ck_trap_resolution_expected_version CHECK (expected_piece_version >= 0),
    CONSTRAINT ck_trap_resolution_rolls CHECK (
        (save_d20_first IS NULL OR save_d20_first BETWEEN 1 AND 20)
        AND (save_d20_second IS NULL OR save_d20_second BETWEEN 1 AND 20)
        AND (save_selected IS NULL OR save_selected BETWEEN 1 AND 20)
    ),
    CONSTRAINT ck_trap_resolution_damage CHECK (damage_total IS NULL OR damage_total >= 0),
    CONSTRAINT ck_trap_resolution_movement CHECK (
        movement_decision IS NULL OR movement_decision IN ('CONTINUE', 'STOP')
    ),
    CONSTRAINT ck_trap_resolution_version CHECK (version >= 0)
);

CREATE INDEX idx_trap_resolutions_session_status
    ON game_schema.trap_resolutions (session_id, status, created_at);

CREATE INDEX idx_trap_resolutions_piece_status
    ON game_schema.trap_resolutions (session_piece_id, status, created_at);

CREATE TABLE game_schema.character_controls (
    session_id VARCHAR(100) NOT NULL,
    character_id UUID NOT NULL,
    dm_participant_id UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    assumed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (session_id, character_id),
    CONSTRAINT fk_character_control_character
        FOREIGN KEY (character_id)
        REFERENCES game_schema.characters (character_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_character_control_dm
        FOREIGN KEY (dm_participant_id)
        REFERENCES game_schema.session_participants (participant_id)
        ON DELETE CASCADE,
    CONSTRAINT ck_character_control_version CHECK (version >= 0)
);

CREATE INDEX idx_character_controls_dm
    ON game_schema.character_controls (session_id, dm_participant_id);
