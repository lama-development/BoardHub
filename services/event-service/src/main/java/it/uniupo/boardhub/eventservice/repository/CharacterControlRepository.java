package it.uniupo.boardhub.eventservice.repository;

import it.uniupo.boardhub.eventservice.model.trap.CharacterControl;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CharacterControlRepository {

    private final JdbcTemplate jdbcTemplate;

    public CharacterControlRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<CharacterControl> find(String sessionId, UUID characterId) {
        return query(sessionId, characterId, false);
    }

    public Optional<CharacterControl> findForUpdate(String sessionId, UUID characterId) {
        return query(sessionId, characterId, true);
    }

    public List<CharacterControl> findBySession(String sessionId) {
        return jdbcTemplate.query("""
                        SELECT session_id, character_id, dm_participant_id, version, assumed_at
                        FROM game_schema.character_controls
                        WHERE session_id = ?
                        ORDER BY character_id ASC
                        """,
                (rs, rowNum) -> new CharacterControl(
                        rs.getString("session_id"),
                        rs.getObject("character_id", UUID.class),
                        rs.getObject("dm_participant_id", UUID.class),
                        rs.getLong("version"),
                        rs.getTimestamp("assumed_at").toInstant().atOffset(ZoneOffset.UTC)
                ),
                sessionId
        );
    }

    public void saveOrReplace(CharacterControl control) {
        int updated = jdbcTemplate.update("""
                UPDATE game_schema.character_controls
                SET dm_participant_id = ?, version = version + 1, assumed_at = ?
                WHERE session_id = ? AND character_id = ?
                """,
                control.dmParticipantId(), Timestamp.from(control.assumedAt().toInstant()),
                control.sessionId(), control.characterId()
        );
        if (updated == 0) {
            jdbcTemplate.update("""
                    INSERT INTO game_schema.character_controls (
                        session_id, character_id, dm_participant_id, version, assumed_at
                    ) VALUES (?, ?, ?, ?, ?)
                    """,
                    control.sessionId(), control.characterId(), control.dmParticipantId(),
                    control.version(), Timestamp.from(control.assumedAt().toInstant())
            );
        }
    }

    public boolean release(String sessionId, UUID characterId, UUID dmParticipantId) {
        return jdbcTemplate.update("""
                DELETE FROM game_schema.character_controls
                WHERE session_id = ? AND character_id = ? AND dm_participant_id = ?
                """, sessionId, characterId, dmParticipantId) == 1;
    }

    private Optional<CharacterControl> query(String sessionId, UUID characterId, boolean forUpdate) {
        String suffix = forUpdate ? " FOR UPDATE" : "";
        return jdbcTemplate.query("""
                        SELECT session_id, character_id, dm_participant_id, version, assumed_at
                        FROM game_schema.character_controls
                        WHERE session_id = ? AND character_id = ?
                        """ + suffix,
                (rs, rowNum) -> new CharacterControl(
                        rs.getString("session_id"),
                        rs.getObject("character_id", UUID.class),
                        rs.getObject("dm_participant_id", UUID.class),
                        rs.getLong("version"),
                        rs.getTimestamp("assumed_at").toInstant().atOffset(ZoneOffset.UTC)
                ),
                sessionId,
                characterId
        ).stream().findFirst();
    }
}
