package it.uniupo.boardhub.eventservice.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.boardhub.eventservice.model.GameEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;

class GameEventRepositoryTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private JdbcTemplate jdbcTemplate;
    private GameEventRepository repository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:boardhub_event_repository;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new GameEventRepository(jdbcTemplate, objectMapper);

        jdbcTemplate.execute("DROP SCHEMA IF EXISTS game_schema CASCADE");
        jdbcTemplate.execute("CREATE SCHEMA game_schema");
        jdbcTemplate.execute("""
                CREATE TABLE game_schema.game_events (
                    event_id VARCHAR(80) PRIMARY KEY,
                    event_type VARCHAR(50) NOT NULL,
                    venue_id VARCHAR(80) NOT NULL,
                    table_id VARCHAR(80) NOT NULL,
                    session_id VARCHAR(100) NOT NULL,
                    source VARCHAR(50) NOT NULL,
                    occurred_at TIMESTAMP NOT NULL,
                    sequence_number BIGINT NOT NULL,
                    payload_json TEXT NOT NULL,
                    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("CREATE UNIQUE INDEX uq_game_events_session_sequence ON game_schema.game_events (session_id, sequence_number)");
    }

    @Test
    void salvaEventoEIgnoraEventIdDuplicato() throws Exception {
        GameEvent event = new GameEvent(
                "evt-000001",
                "MOVE",
                "venue-01",
                "table-04",
                "session-20260702-001",
                "SIMULATOR",
                "2026-07-02T10:00:00Z",
                2,
                objectMapper.readTree("""
                        {
                          "characterId": "adv-01",
                          "from": "A3",
                          "to": "A4"
                        }
                        """)
        );

        boolean firstInsert = repository.save(event);
        boolean duplicateInsert = repository.save(event);

        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM game_schema.game_events",
                Integer.class
        );
        String payload = jdbcTemplate.queryForObject(
                "SELECT payload_json FROM game_schema.game_events WHERE event_id = ?",
                String.class,
                "evt-000001"
        );

        assertThat(firstInsert).isTrue();
        assertThat(duplicateInsert).isFalse();
        assertThat(rows).isEqualTo(1);
        assertThat(payload).contains("\"characterId\":\"adv-01\"");
    }

    @Test
    void leggeEventiDellaSessioneInOrdine() throws Exception {
        GameEvent secondEvent = new GameEvent(
                "evt-000002",
                "DAMAGE",
                "venue-01",
                "table-04",
                "session-20260702-001",
                "SIMULATOR",
                "2026-07-02T10:00:02Z",
                2,
                objectMapper.readTree("""
                        {
                          "targetId": "mon-01",
                          "amount": 8
                        }
                        """)
        );
        GameEvent firstEvent = new GameEvent(
                "evt-000001",
                "MOVE",
                "venue-01",
                "table-04",
                "session-20260702-001",
                "SIMULATOR",
                "2026-07-02T10:00:01Z",
                1,
                objectMapper.readTree("""
                        {
                          "characterId": "adv-01",
                          "from": "A3",
                          "to": "A4"
                        }
                        """)
        );

        repository.save(secondEvent);
        repository.save(firstEvent);

        var events = repository.findBySessionId("session-20260702-001");

        assertThat(events).extracting(GameEvent::eventType).containsExactly("MOVE", "DAMAGE");
        assertThat(events.get(0).payload().get("characterId").asText()).isEqualTo("adv-01");
    }

    @Test
    void ignoraUnaSequenzaDuplicataNellaStessaSessione() throws Exception {
        GameEvent firstEvent = new GameEvent(
                "evt-000001", "MOVE", "venue-01", "table-04", "session-20260702-001",
                "SIMULATOR", "2026-07-02T10:00:00Z", 1, objectMapper.readTree("{}")
        );
        GameEvent duplicateSequence = new GameEvent(
                "evt-000002", "DAMAGE", "venue-01", "table-04", "session-20260702-001",
                "SIMULATOR", "2026-07-02T10:00:01Z", 1, objectMapper.readTree("{}")
        );

        assertThat(repository.save(firstEvent)).isTrue();
        assertThat(repository.save(duplicateSequence)).isFalse();
    }
}
