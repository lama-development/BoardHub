package it.uniupo.boardhub.eventservice.controller;

import it.uniupo.boardhub.eventservice.config.JoinProperties;
import it.uniupo.boardhub.eventservice.repository.GameSessionRepository;
import it.uniupo.boardhub.eventservice.repository.GameTableRepository;
import it.uniupo.boardhub.eventservice.service.GameSessionCreationService;
import it.uniupo.boardhub.eventservice.service.DmAccessService;
import it.uniupo.boardhub.eventservice.service.MovementGridFactory;
import it.uniupo.boardhub.eventservice.service.TableSessionService;
import it.uniupo.boardhub.eventservice.support.MigratedTestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GameSessionControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        JdbcTemplate jdbcTemplate = MigratedTestDatabase.create();
        Clock clock = Clock.fixed(Instant.parse("2026-07-23T10:00:00Z"), ZoneOffset.UTC);
        JoinProperties properties = new JoinProperties(
                Duration.ofMinutes(10), 8, 8, 5, "dm-test-key", "test-token-secret-32-characters"
        );

        GameSessionRepository repository = new GameSessionRepository(jdbcTemplate);
        TableSessionService tableService = new TableSessionService(
                new GameTableRepository(jdbcTemplate), properties, clock
        );
        GameSessionCreationService creationService = new GameSessionCreationService(
                repository, new MovementGridFactory(), tableService, clock
        );
        mockMvc = MockMvcBuilders
                .standaloneSetup(new GameSessionController(
                        creationService,
                        new DmAccessService(properties)
                ))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void creaSessioneConGrigliaIniziale() throws Exception {
        mockMvc.perform(post("/api/v1/sessions")
                        .header("X-BoardHub-DM-Key", "dm-test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId": "session-20260705-001",
                                  "venueId": "venue-01",
                                  "tableId": "table-04",
                                  "title": "Cripta del Re Caduto",
                                  "gameType": "DND",
                                  "grid": {
                                    "width": 3,
                                    "height": 3,
                                    "difficultCells": ["C1"],
                                    "blockedCells": ["A2"],
                                    "obstacleCells": [],
                                    "occupiedCells": ["A1"],
                                    "walls": [
                                      { "cell": "B1", "direction": "SOUTH" }
                                    ],
                                    "traps": [
                                      {
                                        "trapId": "trap-01",
                                        "cell": "B1",
                                        "visibility": "HIDDEN",
                                        "armed": true
                                      }
                                    ]
                                  }
                }
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value("session-20260705-001"))
                .andExpect(jsonPath("$.venueId").value("venue-01"))
                .andExpect(jsonPath("$.tableId").value("table-04"))
                .andExpect(jsonPath("$.tablePublicId").value("table-04"))
                .andExpect(jsonPath("$.tableDisplayName").value("table-04"))
                .andExpect(jsonPath("$.title").value("Cripta del Re Caduto"))
                .andExpect(jsonPath("$.gameType").value("DND"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.gridWidth").value(3))
                .andExpect(jsonPath("$.gridHeight").value(3));
    }

    @Test
    void restituisceConflictSeSessioneEsisteGia() throws Exception {
        String request = """
                {
                  "sessionId": "session-20260705-001",
                  "venueId": "venue-01",
                  "tableId": "table-04",
                  "title": "Cripta del Re Caduto",
                  "gameType": "DND",
                  "grid": {
                    "width": 3,
                    "height": 3,
                    "difficultCells": [],
                    "blockedCells": [],
                    "obstacleCells": [],
                    "occupiedCells": [],
                    "walls": [],
                    "traps": []
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/sessions")
                        .header("X-BoardHub-DM-Key", "dm-test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/sessions")
                        .header("X-BoardHub-DM-Key", "dm-test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_SESSION"));
    }

    @Test
    void richiedeLaChiaveDmPerCreareUnaSessione() throws Exception {
        mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId": "session-protected-001",
                                  "venueId": "venue-01",
                                  "tableId": "table-04",
                                  "title": "Sessione protetta",
                                  "gameType": "DND",
                                  "grid": {
                                    "width": 3,
                                    "height": 3,
                                    "difficultCells": [],
                                    "blockedCells": [],
                                    "obstacleCells": [],
                                    "occupiedCells": [],
                                    "walls": [],
                                    "traps": []
                                  }
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("DM_UNAUTHORIZED"));
    }

}
