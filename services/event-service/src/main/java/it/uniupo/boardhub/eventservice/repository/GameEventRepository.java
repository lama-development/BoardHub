package it.uniupo.boardhub.eventservice.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.boardhub.eventservice.model.GameEvent;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;

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
}
