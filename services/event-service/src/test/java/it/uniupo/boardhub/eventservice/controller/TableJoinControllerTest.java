package it.uniupo.boardhub.eventservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.boardhub.eventservice.config.CharacterProperties;
import it.uniupo.boardhub.eventservice.config.JoinProperties;
import it.uniupo.boardhub.eventservice.config.VenueProperties;
import it.uniupo.boardhub.eventservice.model.grid.GridConfiguration;
import it.uniupo.boardhub.eventservice.repository.CharacterRepository;
import it.uniupo.boardhub.eventservice.repository.GameSessionRepository;
import it.uniupo.boardhub.eventservice.repository.GameTableRepository;
import it.uniupo.boardhub.eventservice.repository.JoinRequestRepository;
import it.uniupo.boardhub.eventservice.repository.SessionParticipantRepository;
import it.uniupo.boardhub.eventservice.service.DmAccessService;
import it.uniupo.boardhub.eventservice.service.CharacterService;
import it.uniupo.boardhub.eventservice.service.GameSessionCreationService;
import it.uniupo.boardhub.eventservice.service.JoinRequestService;
import it.uniupo.boardhub.eventservice.service.MovementGridFactory;
import it.uniupo.boardhub.eventservice.service.ParticipantAccessService;
import it.uniupo.boardhub.eventservice.service.SessionTokenService;
import it.uniupo.boardhub.eventservice.service.SessionLifecycleService;
import it.uniupo.boardhub.eventservice.service.TableSessionService;
import it.uniupo.boardhub.eventservice.service.VenueAccessService;
import it.uniupo.boardhub.eventservice.service.command.CreateGameSessionCommand;
import it.uniupo.boardhub.eventservice.support.MigratedTestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TableJoinControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private String dmAuthorization;

    @BeforeEach
    void setUp() {
        JdbcTemplate jdbcTemplate = MigratedTestDatabase.create();
        Clock clock = Clock.fixed(Instant.parse("2026-07-23T10:00:00Z"), ZoneOffset.UTC);
        JoinProperties properties = new JoinProperties(
                Duration.ofMinutes(10), 8, 8, 5, "test-token-secret-at-least-32-chars"
        );
        GameSessionRepository sessionRepository = new GameSessionRepository(jdbcTemplate);
        GameTableRepository tableRepository = new GameTableRepository(jdbcTemplate);
        SessionParticipantRepository participantRepository =
                new SessionParticipantRepository(jdbcTemplate);
        SessionTokenService tokenService = new SessionTokenService(properties);
        ParticipantAccessService participantAccessService = new ParticipantAccessService(
                tokenService, participantRepository, sessionRepository
        );
        CharacterService characterService = new CharacterService(
                participantAccessService,
                new CharacterRepository(jdbcTemplate),
                sessionRepository,
                new CharacterProperties(8),
                clock
        );
        VenueProperties venueProperties =
                new VenueProperties(Duration.ofMinutes(10), 60, "venue-test-key", true);
        TableSessionService tableService = new TableSessionService(
                tableRepository,
                properties,
                venueProperties,
                clock
        );
        tableService.enableTable("qr-table-04", null);
        JoinRequestService joinService = new JoinRequestService(
                sessionRepository, tableRepository, new JoinRequestRepository(jdbcTemplate),
                participantRepository, properties, tokenService, clock
        );
        DmAccessService dmAccessService = new DmAccessService(
                tokenService, participantRepository, sessionRepository
        );
        GameSessionCreationService creationService = new GameSessionCreationService(
                sessionRepository, new MovementGridFactory(), tableService, dmAccessService, clock
        );
        var createdSession = creationService.createSession(new CreateGameSessionCommand(
                "session-api-001", "venue-01", "table-04", "qr-table-04", "Tavolo 4",
                "Miniere Perdute", "DND", "Sessione dimostrativa.", true,
                new GridConfiguration(
                        3, 3, List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
                )
        ));
        dmAuthorization = "Bearer " + createdSession.dmAccessToken();

        objectMapper = new ObjectMapper();
        SessionLifecycleService lifecycleService = new SessionLifecycleService(
                sessionRepository, tableRepository,
                new JoinRequestRepository(jdbcTemplate),
                participantRepository, clock
        );
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new PublicTableController(tableService, joinService),
                        new VenueTableAdminController(
                                tableService,
                                new VenueAccessService(venueProperties),
                                lifecycleService
                        ),
                        new PlayerSessionController(
                                participantAccessService,
                                characterService
                        ),
                        new DmSessionController(
                                joinService,
                                dmAccessService,
                                lifecycleService,
                                characterService
                        )
                )
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void esegueIlFlussoQrRichiestaApprovazioneEPartecipante() throws Exception {
        mockMvc.perform(get("/api/v1/public/tables/qr-table-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_SESSION"))
                .andExpect(jsonPath("$.tableNumber").value(4))
                .andExpect(jsonPath("$.activeSession.sessionId").value("session-api-001"));

        mockMvc.perform(get("/api/v1/public/tables/qr-table-04/active-session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("session-api-001"))
                .andExpect(jsonPath("$.tableDisplayName").value("Tavolo 4"));

        String joinClaim = UUID.randomUUID().toString();
        MvcResult created = mockMvc.perform(post("/api/v1/public/sessions/session-api-001/join-requests")
                        .header("Idempotency-Key", joinClaim)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"playerReference":"device-api-01","displayName":"Andrea"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());
        String requestId = body.get("requestId").asText();

        mockMvc.perform(get("/api/v1/public/sessions/session-api-001/join-requests/" + requestId)
                        .header("X-BoardHub-Join-Claim", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/public/sessions/session-api-001/join-requests/" + requestId)
                        .header("X-BoardHub-Join-Claim", joinClaim))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request.status").value("PENDING"))
                .andExpect(jsonPath("$.participant").doesNotExist())
                .andExpect(jsonPath("$.accessToken").doesNotExist());

        mockMvc.perform(get("/api/v1/dm/sessions/session-api-001/join-requests"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("DM_UNAUTHORIZED"));
        mockMvc.perform(get("/api/v1/dm/sessions/session-api-001/join-requests")
                        .header("Authorization", "Bearer bhd1.token-non-valido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("DM_UNAUTHORIZED"));
        mockMvc.perform(get("/api/v1/dm/sessions/session-api-001/join-requests")
                        .header("Authorization", dmAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestId").value(requestId));

        MvcResult accepted = mockMvc.perform(post("/api/v1/dm/sessions/session-api-001/join-requests/"
                        + requestId + "/accept")
                        .header("Authorization", dmAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.participant.displayName").value("Andrea"))
                .andExpect(jsonPath("$.accessToken").value(org.hamcrest.Matchers.startsWith("bhp1.")))
                .andReturn();
        String accessToken = objectMapper.readTree(
                accepted.getResponse().getContentAsString()
        ).get("accessToken").asText();

        mockMvc.perform(get("/api/v1/public/sessions/session-api-001/join-requests/" + requestId)
                        .header("X-BoardHub-Join-Claim", joinClaim))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.participant.displayName").value("Andrea"))
                .andExpect(jsonPath("$.accessToken").value(org.hamcrest.Matchers.startsWith("bhp1.")));

        mockMvc.perform(get("/api/v1/dm/sessions/session-api-001/participants")
                        .header("Authorization", dmAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].displayName").value(
                        org.hamcrest.Matchers.hasItem("Andrea")
                ))
                .andExpect(jsonPath("$[*].role").value(
                        org.hamcrest.Matchers.hasItems("DM", "PLAYER")
                ));

        mockMvc.perform(get("/api/v1/player/sessions/session-api-001/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Andrea"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        mockMvc.perform(get("/api/v1/player/sessions/session-api-001/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("PLAYER_UNAUTHORIZED"));
        mockMvc.perform(get("/api/v1/player/sessions/session-api-001/me")
                        .header("Authorization", "Bearer " + accessToken + "alterato"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("PLAYER_UNAUTHORIZED"));
        mockMvc.perform(get("/api/v1/player/sessions/altra-sessione/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("PLAYER_UNAUTHORIZED"));

        mockMvc.perform(post("/api/v1/player/sessions/session-api-001/characters")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Elaria",
                                  "species": "Elfa",
                                  "className": "Maga",
                                  "level": 3,
                                  "speedCells": 6,
                                  "hpMax": 18,
                                  "armorClass": 12
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Elaria"))
                .andExpect(jsonPath("$.age").doesNotExist())
                .andExpect(jsonPath("$.hpCurrent").value(18))
                .andExpect(jsonPath("$.partyVisibility").value("OWNER_ONLY"));
        mockMvc.perform(get("/api/v1/player/sessions/session-api-001/characters")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Elaria"));
        mockMvc.perform(get("/api/v1/player/sessions/session-api-001/characters"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("PLAYER_UNAUTHORIZED"));
        mockMvc.perform(get("/api/v1/dm/sessions/session-api-001/characters")
                        .header("Authorization", dmAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Elaria"))
                .andExpect(jsonPath("$[0].partyVisibility").value("OWNER_ONLY"));
        mockMvc.perform(get("/api/v1/dm/sessions/session-api-001/characters"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("DM_UNAUTHORIZED"));

        mockMvc.perform(post("/api/v1/dm/sessions/session-api-001/close")
                        .header("Authorization", dmAuthorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENDED"));
        mockMvc.perform(get("/api/v1/player/sessions/session-api-001/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("PLAYER_UNAUTHORIZED"));
        mockMvc.perform(get("/api/v1/public/tables/qr-table-04/active-session"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/public/tables/qr-table-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"))
                .andExpect(jsonPath("$.activeSession").doesNotExist());
        mockMvc.perform(get("/api/v1/dm/sessions/session-api-001/participants")
                        .header("Authorization", dmAuthorization))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("DM_UNAUTHORIZED"));
    }

    @Test
    void validaIlNumeroDelTavoloPubblico() throws Exception {
        mockMvc.perform(get("/api/v1/public/tables/qr-table-00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("da 1 a 8")));

        mockMvc.perform(get("/api/v1/public/tables/qr-table-09"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("da 1 a 8")));
    }

    @Test
    void ilLocaleAbilitaDisabilitaEChiudeIPropriTavoli() throws Exception {
        mockMvc.perform(get("/api/v1/admin/tables"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("VENUE_UNAUTHORIZED"));

        mockMvc.perform(post("/api/v1/admin/tables/qr-table-05/enable")
                        .header("X-BoardHub-Venue-Key", "venue-test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"durationMinutes\":15}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLAIMABLE"))
                .andExpect(jsonPath("$.claimExpiresAt").exists());

        mockMvc.perform(post("/api/v1/admin/tables/qr-table-05/disable")
                        .header("X-BoardHub-Venue-Key", "venue-test-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));

        mockMvc.perform(post("/api/v1/admin/tables/qr-table-04/close-session")
                        .header("X-BoardHub-Venue-Key", "venue-test-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"))
                .andExpect(jsonPath("$.activeSession").doesNotExist());
    }
}
