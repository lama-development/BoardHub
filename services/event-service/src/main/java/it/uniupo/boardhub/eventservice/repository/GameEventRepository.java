package it.uniupo.boardhub.eventservice.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.boardhub.eventservice.model.GameEvent;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class GameEventRepository {

    private static final String INSERT_EVENT_SQL = """
            INSERT INTO game_schema.game_events (
                event_id,
                event_type,
                venue_id,
                table_id,
                session_id,
                source,
                occurred_at,
                sequence_number,
                payload_json
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String FIND_BY_SESSION_SQL = """
            SELECT
                event_id,
                event_type,
                venue_id,
                table_id,
                session_id,
                source,
                occurred_at,
                sequence_number,
                payload_json
            FROM game_schema.game_events
            WHERE session_id = ?
            ORDER BY sequence_number ASC, occurred_at ASC
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public GameEventRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    // Salva l'evento e ignora i duplicati con lo stesso eventId.
    public boolean save(GameEvent event) throws JsonProcessingException {
        try {
            jdbcTemplate.update(
                    INSERT_EVENT_SQL,
                    event.eventId(),
                    event.eventType(),
                    event.venueId(),
                    event.tableId(),
                    event.sessionId(),
                    event.source(),
                    Timestamp.from(OffsetDateTime.parse(event.occurredAt()).toInstant()),
                    event.sequenceNumber(),
                    objectMapper.writeValueAsString(event.payload())
            );
            return true;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }

    // Legge gli eventi di una sessione gia ordinati per ricostruire la partita.
    public List<GameEvent> findBySessionId(String sessionId) {
        return jdbcTemplate.query(FIND_BY_SESSION_SQL, new GameEventRowMapper(), sessionId);
    }

    private class GameEventRowMapper implements RowMapper<GameEvent> {

        @Override
        public GameEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
            try {
                return new GameEvent(
                        rs.getString("event_id"),
                        rs.getString("event_type"),
                        rs.getString("venue_id"),
                        rs.getString("table_id"),
                        rs.getString("session_id"),
                        rs.getString("source"),
                        rs.getTimestamp("occurred_at").toInstant().toString(),
                        rs.getLong("sequence_number"),
                        objectMapper.readTree(rs.getString("payload_json"))
                );
            } catch (JsonProcessingException ex) {
                throw new SQLException("Payload evento non leggibile", ex);
            }
        }
    }
}
