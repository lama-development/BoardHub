package it.uniupo.boardhub.eventservice.controller;

import it.uniupo.boardhub.eventservice.config.JoinProperties;
import it.uniupo.boardhub.eventservice.config.VenueProperties;
import it.uniupo.boardhub.eventservice.repository.GameSessionRepository;
import it.uniupo.boardhub.eventservice.repository.GameTableRepository;
import it.uniupo.boardhub.eventservice.repository.SessionParticipantRepository;
import it.uniupo.boardhub.eventservice.service.GameSessionCreationService;
import it.uniupo.boardhub.eventservice.service.DmAccessService;
import it.uniupo.boardhub.eventservice.service.MovementGridFactory;
import it.uniupo.boardhub.eventservice.service.SessionTokenService;
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
                Duration.ofMinutes(10), 8, 8, 5, "test-token-secret-at-least-32-chars"
        );

        GameSessionRepository repository = new GameSessionRepository(jdbcTemplate);
        GameTableRepository tableRepository = new GameTableRepository(jdbcTemplate);
        TableSessionService tableService = new TableSessionService(
                tableRepository,
                properties,
                new VenueProperties(Duration.ofMinutes(10), 60, "venue-test-key", true),
                clock
        );
        tableService.enableTable("qr-table-04", null);
        DmAccessService dmAccessService = new DmAccessService(
                new SessionTokenService(properties),
                new SessionParticipantRepository(jdbcTemplate),
                repository
        );
        GameSessionCreationService creationService = new GameSessionCreationService(
                repository, new MovementGridFactory(), tableService, dmAccessService, clock
        );
        mockMvc = MockMvcBuilders
                .standaloneSetup(new GameSessionController(creationService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void creaSessioneConGrigliaIniziale() throws Exception {
        mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId": "session-20260705-001",
                                  "venueId": "venue-01",
                                  "tableId": "table-04",
                                  "tablePublicId": "qr-table-04",
                                  "tableDisplayName": "Tavolo 4",
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
                .andExpect(jsonPath("$.tablePublicId").value("qr-table-04"))
                .andExpect(jsonPath("$.tableDisplayName").value("Tavolo 4"))
                .andExpect(jsonPath("$.title").value("Cripta del Re Caduto"))
                .andExpect(jsonPath("$.gameType").value("DND"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.gridWidth").value(3))
                .andExpect(jsonPath("$.gridHeight").value(3))
                .andExpect(jsonPath("$.dmAccessToken").value(
                        org.hamcrest.Matchers.startsWith("bhd1.")
                ));
    }

    @Test
    void restituisceConflictSeSessioneEsisteGia() throws Exception {
        String request = """
                {
                  "sessionId": "session-20260705-001",
                  "venueId": "venue-01",
                  "tableId": "table-04",
                  "tablePublicId": "qr-table-04",
                  "tableDisplayName": "Tavolo 4",
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_SESSION"));
    }

    @Test
    void rifiutaLaCreazioneSuUnTavoloNonAbilitatoDalLocale() throws Exception {
        mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId": "session-protected-001",
                                  "venueId": "venue-01",
                                  "tableId": "table-05",
                                  "tablePublicId": "qr-table-05",
                                  "tableDisplayName": "Tavolo 5",
                                  "title": "Sessione non autorizzata",
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
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("JOIN_CONFLICT"))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("non ha abilitato")
                ));
    }

}
