package it.uniupo.boardhub.eventservice.service;

import it.uniupo.boardhub.eventservice.config.JoinProperties;
import it.uniupo.boardhub.eventservice.config.VenueProperties;
import it.uniupo.boardhub.eventservice.model.grid.GridConfiguration;
import it.uniupo.boardhub.eventservice.model.grid.GridPosition;
import it.uniupo.boardhub.eventservice.model.grid.MovementRequest;
import it.uniupo.boardhub.eventservice.model.grid.TerrainType;
import it.uniupo.boardhub.eventservice.model.grid.TrapVisibility;
import it.uniupo.boardhub.eventservice.repository.GameSessionRepository;
import it.uniupo.boardhub.eventservice.repository.GameTableRepository;
import it.uniupo.boardhub.eventservice.repository.SessionParticipantRepository;
import it.uniupo.boardhub.eventservice.service.command.CreateGameSessionCommand;
import it.uniupo.boardhub.eventservice.support.MigratedTestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GameSessionCreationServiceTest {

    private GameSessionRepository repository;
    private GameSessionCreationService creationService;

    @BeforeEach
    void setUp() {
        JdbcTemplate jdbcTemplate = MigratedTestDatabase.create();
        Clock clock = Clock.fixed(Instant.parse("2026-07-23T10:00:00Z"), ZoneOffset.UTC);
        JoinProperties properties = new JoinProperties(
                Duration.ofMinutes(10), 8, 8, 5, "test-token-secret-at-least-32-chars"
        );

        repository = new GameSessionRepository(jdbcTemplate);
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
        creationService = new GameSessionCreationService(
                repository, new MovementGridFactory(), tableService, dmAccessService, clock
        );
    }

    @Test
    void salvaSessioneEPermetteCalcoloMovimentoDaStatoPersistito() {
        var created = creationService.createSession(new CreateGameSessionCommand(
                "session-20260705-001",
                "venue-01",
                "table-04",
                "qr-table-04",
                "Tavolo 4",
                "Cripta del Re Caduto",
                "DND",
                "Sessione dimostrativa.",
                true,
                new GridConfiguration(
                        3,
                        3,
                        List.of("C1"),
                        List.of("A2"),
                        List.of(),
                        List.of("A1"),
                        List.of(new GridConfiguration.WallConfiguration("B1", "SOUTH")),
                        List.of(new GridConfiguration.TrapConfiguration(
                                "trap-01", "B1", "HIDDEN", true
                        ))
                )
        ));
        var session = created.session();

        var cells = repository.findCellsBySessionId(session.sessionId());
        var walls = repository.findWallsBySessionId(session.sessionId());
        var traps = repository.findTrapsBySessionId(session.sessionId());

        assertThat(repository.findSessionById(session.sessionId())).contains(session);
        assertThat(created.table().tablePublicId()).isEqualTo("qr-table-04");
        assertThat(created.dmAccessToken()).startsWith("bhd1.");
        assertThat(cells).extracting("cell").containsExactly("A1", "A2", "C1");
        assertThat(cells).filteredOn(cell -> cell.cell().equals("A1"))
                .first()
                .satisfies(cell -> {
                    assertThat(cell.terrainType()).isEqualTo(TerrainType.NORMAL);
                    assertThat(cell.occupiedBy()).isEqualTo("OCCUPIED");
                });
        assertThat(walls).extracting("cell").containsExactly("B1");
        assertThat(traps).first().satisfies(trap -> {
            assertThat(trap.trapId()).isEqualTo("trap-01");
            assertThat(trap.visibility()).isEqualTo(TrapVisibility.HIDDEN);
        });

        SessionMovementService movementService = new SessionMovementService(
                new SessionGridService(repository),
                new MovementService()
        );
        var reachableCells = movementService.calculateReachableCells(
                session.sessionId(),
                new MovementRequest("adv-01", GridPosition.fromCell("A1"), 2)
        );

        assertThat(reachableCells).extracting(cell -> cell.position().toCell())
                .contains("A1", "B1")
                .doesNotContain("A2", "B2", "C1");
    }

}
