package it.uniupo.boardhub.eventservice.repository;

import it.uniupo.boardhub.eventservice.model.character.PartyVisibility;
import it.uniupo.boardhub.eventservice.model.character.PlayerCharacter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Repository
public class CharacterRepository {

    private static final String SELECT_FIELDS = """
            SELECT character_id, session_id, participant_id, name, species, age,
                   class_name, level, speed_cells, hp_current, hp_max, armor_class,
                   party_visibility, version, created_at, updated_at
            FROM game_schema.characters
            """;

    private final JdbcTemplate jdbcTemplate;

    public CharacterRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(PlayerCharacter character) {
        jdbcTemplate.update("""
                        INSERT INTO game_schema.characters (
                            character_id, session_id, participant_id, name, species, age,
                            class_name, level, speed_cells, hp_current, hp_max, armor_class,
                            party_visibility, version, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                character.characterId(),
                character.sessionId(),
                character.participantId(),
                character.name(),
                character.species(),
                character.age(),
                character.className(),
                character.level(),
                character.speedCells(),
                character.hpCurrent(),
                character.hpMax(),
                character.armorClass(),
                character.partyVisibility().name(),
                character.version(),
                Timestamp.from(character.createdAt().toInstant()),
                Timestamp.from(character.updatedAt().toInstant())
        );
    }

    public int countByParticipant(String sessionId, UUID participantId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM game_schema.characters
                WHERE session_id = ? AND participant_id = ?
                """, Integer.class, sessionId, participantId);
        return count == null ? 0 : count;
    }

    public List<PlayerCharacter> findByParticipant(String sessionId, UUID participantId) {
        return jdbcTemplate.query(
                SELECT_FIELDS + """
                        WHERE session_id = ? AND participant_id = ?
                        ORDER BY created_at ASC, character_id ASC
                        """,
                new CharacterRowMapper(),
                sessionId,
                participantId
        );
    }

    public List<PlayerCharacter> findBySession(String sessionId) {
        return jdbcTemplate.query(
                SELECT_FIELDS + """
                        WHERE session_id = ?
                        ORDER BY created_at ASC, character_id ASC
                        """,
                new CharacterRowMapper(),
                sessionId
        );
    }

    private static class CharacterRowMapper implements RowMapper<PlayerCharacter> {

        @Override
        public PlayerCharacter mapRow(ResultSet rs, int rowNum) throws SQLException {
            int ageValue = rs.getInt("age");
            Integer age = rs.wasNull() ? null : ageValue;
            return new PlayerCharacter(
                    rs.getObject("character_id", UUID.class),
                    rs.getString("session_id"),
                    rs.getObject("participant_id", UUID.class),
                    rs.getString("name"),
                    rs.getString("species"),
                    age,
                    rs.getString("class_name"),
                    rs.getInt("level"),
                    rs.getInt("speed_cells"),
                    rs.getInt("hp_current"),
                    rs.getInt("hp_max"),
                    rs.getInt("armor_class"),
                    PartyVisibility.valueOf(rs.getString("party_visibility")),
                    rs.getLong("version"),
                    rs.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC),
                    rs.getTimestamp("updated_at").toInstant().atOffset(ZoneOffset.UTC)
            );
        }
    }
}
