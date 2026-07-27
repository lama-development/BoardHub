package it.uniupo.boardhub.eventservice.service;

import it.uniupo.boardhub.eventservice.config.JoinProperties;
import it.uniupo.boardhub.eventservice.config.VenueProperties;
import it.uniupo.boardhub.eventservice.model.grid.GridConfiguration;
import it.uniupo.boardhub.eventservice.model.join.JoinRequestStatus;
import it.uniupo.boardhub.eventservice.repository.GameSessionRepository;
import it.uniupo.boardhub.eventservice.repository.GameTableRepository;
import it.uniupo.boardhub.eventservice.repository.JoinRequestRepository;
import it.uniupo.boardhub.eventservice.repository.SessionParticipantRepository;
import it.uniupo.boardhub.eventservice.service.command.CreateGameSessionCommand;
import it.uniupo.boardhub.eventservice.service.command.CreateJoinRequestCommand;
import it.uniupo.boardhub.eventservice.service.exception.JoinRequestConflictException;
import it.uniupo.boardhub.eventservice.service.exception.JoinRequestNotFoundException;
import it.uniupo.boardhub.eventservice.service.exception.JoinRequestRateLimitException;
import it.uniupo.boardhub.eventservice.service.exception.SessionCapacityException;
import it.uniupo.boardhub.eventservice.support.MigratedTestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JoinRequestServiceTest {

    private static final Instant INITIAL_TIME = Instant.parse("2026-07-23T10:00:00Z");

    private GameSessionRepository sessionRepository;
    private GameTableRepository tableRepository;
    private JoinRequestRepository requestRepository;
    private SessionParticipantRepository participantRepository;
    private JoinProperties properties;
    private JoinRequestService service;

    @BeforeEach
    void setUp() {
        JdbcTemplate jdbcTemplate = MigratedTestDatabase.create();
        sessionRepository = new GameSessionRepository(jdbcTemplate);
        tableRepository = new GameTableRepository(jdbcTemplate);
        requestRepository = new JoinRequestRepository(jdbcTemplate);
        participantRepository = new SessionParticipantRepository(jdbcTemplate);
        properties = properties(8, 5, Duration.ofMinutes(10));
        Clock clock = fixedClock(INITIAL_TIME);
        service = createJoinService(properties, clock);
        createSession(clock);
    }

    @Test
    void risolveQrCreaRichiestaEAggiungePartecipanteSoloDopoAccept() {
        TableSessionService tableService = tableService(properties, fixedClock(INITIAL_TIME));
        assertThat(tableService.resolveActiveSession("qr-table-04").sessionId()).isEqualTo("session-join-001");

        UUID key = UUID.randomUUID();
        var request = service.create(
                "session-join-001", key.toString(),
                new CreateJoinRequestCommand("player-device-01", "Andrea")
        );

        assertThat(request.status()).isEqualTo(JoinRequestStatus.PENDING);
        assertThat(participantRepository.countActivePlayers("session-join-001")).isZero();
        var pendingStatus = service.getPlayerStatus(
                "session-join-001", request.requestId(), key.toString()
        );
        assertThat(pendingStatus.request().status()).isEqualTo(JoinRequestStatus.PENDING);
        assertThat(pendingStatus.participant()).isNull();
        assertThat(pendingStatus.accessToken()).isNull();

        var accepted = service.accept("session-join-001", request.requestId());
        var retried = service.accept("session-join-001", request.requestId());
        var playerStatus = service.getPlayerStatus(
                "session-join-001", request.requestId(), key.toString()
        );

        assertThat(accepted.request().status()).isEqualTo(JoinRequestStatus.ACCEPTED);
        assertThat(accepted.accessToken()).startsWith("bhp1.").isEqualTo(retried.accessToken());
        assertThat(playerStatus.request().status()).isEqualTo(JoinRequestStatus.ACCEPTED);
        assertThat(playerStatus.participant().displayName()).isEqualTo("Andrea");
        assertThat(playerStatus.accessToken()).isEqualTo(accepted.accessToken());
        assertThat(participantRepository.countActivePlayers("session-join-001")).isEqualTo(1);
    }

    @Test
    void nonEsponeLoStatoAChiNonPossiedeLaChiaveDellaRichiesta() {
        UUID key = UUID.randomUUID();
        var request = service.create(
                "session-join-001", key.toString(),
                new CreateJoinRequestCommand("player-device-01", "Andrea")
        );

        assertThatThrownBy(() -> service.getPlayerStatus(
                "session-join-001", request.requestId(), UUID.randomUUID().toString()
        )).isInstanceOf(JoinRequestNotFoundException.class);
    }

    @Test
    void ripeteLaStessaRichiestaSenzaCreareDuplicati() {
        UUID key = UUID.randomUUID();
        CreateJoinRequestCommand body = new CreateJoinRequestCommand("player-device-01", "Andrea");

        var first = service.create("session-join-001", key.toString(), body);
        var retry = service.create("session-join-001", key.toString(), body);

        assertThat(retry).isEqualTo(first);
        assertThat(service.list("session-join-001", JoinRequestStatus.PENDING)).hasSize(1);
        assertThatThrownBy(() -> service.create(
                "session-join-001", key.toString(),
                new CreateJoinRequestCommand("player-device-02", "Davide")
        )).isInstanceOf(JoinRequestConflictException.class);
    }

    @Test
    void scadeLaRichiestaEImpedisceUnaDecisioneTardiva() {
        var request = service.create(
                "session-join-001", UUID.randomUUID().toString(),
                new CreateJoinRequestCommand("player-device-01", "Andrea")
        );
        JoinRequestService futureService = createJoinService(
                properties,
                fixedClock(INITIAL_TIME.plus(Duration.ofMinutes(11)))
        );

        assertThat(futureService.list("session-join-001", JoinRequestStatus.EXPIRED))
                .extracting(item -> item.requestId())
                .containsExactly(request.requestId());
        assertThatThrownBy(() -> futureService.accept("session-join-001", request.requestId()))
                .isInstanceOf(JoinRequestConflictException.class);
    }

    @Test
    void applicaLimiteGiocatoriELimiteRichieste() {
        JoinProperties restrictive = properties(1, 2, Duration.ofMinutes(10));
        JoinRequestService restrictiveService = createJoinService(restrictive, fixedClock(INITIAL_TIME));

        var first = restrictiveService.create(
                "session-join-001", UUID.randomUUID().toString(),
                new CreateJoinRequestCommand("player-device-01", "Andrea")
        );
        restrictiveService.accept("session-join-001", first.requestId());
        var second = restrictiveService.create(
                "session-join-001", UUID.randomUUID().toString(),
                new CreateJoinRequestCommand("player-device-02", "Davide")
        );

        assertThatThrownBy(() -> restrictiveService.accept("session-join-001", second.requestId()))
                .isInstanceOf(SessionCapacityException.class);

        var rateOne = restrictiveService.create(
                "session-join-001", UUID.randomUUID().toString(),
                new CreateJoinRequestCommand("player-device-rate", "Prova")
        );
        restrictiveService.reject("session-join-001", rateOne.requestId());
        var rateTwo = restrictiveService.create(
                "session-join-001", UUID.randomUUID().toString(),
                new CreateJoinRequestCommand("player-device-rate", "Prova")
        );
        restrictiveService.reject("session-join-001", rateTwo.requestId());

        assertThatThrownBy(() -> restrictiveService.create(
                "session-join-001", UUID.randomUUID().toString(),
                new CreateJoinRequestCommand("player-device-rate", "Prova")
        )).isInstanceOf(JoinRequestRateLimitException.class);
    }

    private void createSession(Clock clock) {
        TableSessionService tableService = tableService(properties, clock);
        tableService.enableTable("qr-table-04", null);
        DmAccessService dmAccessService = new DmAccessService(
                new SessionTokenService(properties),
                participantRepository,
                sessionRepository
        );
        GameSessionCreationService creationService = new GameSessionCreationService(
                sessionRepository, new MovementGridFactory(), tableService, dmAccessService, clock
        );
        creationService.createSession(new CreateGameSessionCommand(
                "session-join-001", "venue-01", "table-04", "qr-table-04", "Tavolo 4",
                "Cripta del Re Caduto", "DND", "Avventura per personaggi di livello 3.", true,
                new GridConfiguration(
                        3, 3, List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
                )
        ));
    }

    private JoinRequestService createJoinService(JoinProperties joinProperties, Clock clock) {
        return new JoinRequestService(
                sessionRepository, tableRepository, requestRepository, participantRepository,
                joinProperties, new SessionTokenService(joinProperties), clock
        );
    }

    private JoinProperties properties(int players, int rate, Duration ttl) {
        return new JoinProperties(
                ttl, players, 8, rate, "test-token-secret-at-least-32-chars"
        );
    }

    private TableSessionService tableService(JoinProperties joinProperties, Clock clock) {
        return new TableSessionService(
                tableRepository,
                joinProperties,
                new VenueProperties(Duration.ofMinutes(10), 60, "venue-test-key", true),
                clock
        );
    }

    private Clock fixedClock(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }
}
