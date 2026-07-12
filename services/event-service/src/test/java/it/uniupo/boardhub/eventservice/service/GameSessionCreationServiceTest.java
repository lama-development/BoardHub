package it.uniupo.boardhub.eventservice.service;

import it.uniupo.boardhub.eventservice.controller.dto.CreateGameSessionRequest;
import it.uniupo.boardhub.eventservice.controller.dto.MovementGridRequest;
import it.uniupo.boardhub.eventservice.controller.dto.MovementTrapRequest;
import it.uniupo.boardhub.eventservice.controller.dto.MovementWallRequest;
import it.uniupo.boardhub.eventservice.model.grid.GridPosition;
import it.uniupo.boardhub.eventservice.model.grid.MovementRequest;
import it.uniupo.boardhub.eventservice.model.grid.TerrainType;
import it.uniupo.boardhub.eventservice.model.grid.TrapVisibility;
import it.uniupo.boardhub.eventservice.repository.GameSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GameSessionCreationServiceTest {

    private GameSessionRepository repository;
    private GameSessionCreationService creationService;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:boardhub_game_session_creation_service;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        createSchema(jdbcTemplate);

        repository = new GameSessionRepository(jdbcTemplate);
        creationService = new GameSessionCreationService(repository, new MovementGridFactory());
    }

    @Test
    void salvaSessioneEPermetteCalcoloMovimentoDaStatoPersistito() {
        var session = creationService.createSession(new CreateGameSessionRequest(
                "session-20260705-001",
                "venue-01",
                "table-04",
                "Cripta del Re Caduto",
                "DND",
                new MovementGridRequest(
                        3,
                        3,
                        List.of("C1"),
                        List.of("A2"),
                        List.of(),
                        List.of("A1"),
                        List.of(new MovementWallRequest("B1", "SOUTH")),
                        List.of(new MovementTrapRequest("trap-01", "B1", "HIDDEN", true))
                )
        ));

        var cells = repository.findCellsBySessionId(session.sessionId());
        var walls = repository.findWallsBySessionId(session.sessionId());
        var traps = repository.findTrapsBySessionId(session.sessionId());

        assertThat(repository.findSessionById(session.sessionId())).contains(session);
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

    private void createSchema(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS game_schema CASCADE");
        jdbcTemplate.execute("CREATE SCHEMA game_schema");
        jdbcTemplate.execute("""
                CREATE TABLE game_schema.game_sessions (
                    session_id VARCHAR(100) PRIMARY KEY,
                    venue_id VARCHAR(80) NOT NULL,
                    table_id VARCHAR(80) NOT NULL,
                    title VARCHAR(150) NOT NULL,
                    game_type VARCHAR(40) NOT NULL,
                    status VARCHAR(40) NOT NULL,
                    grid_width INTEGER NOT NULL,
                    grid_height INTEGER NOT NULL,
                    created_at TIMESTAMP NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE game_schema.game_grid_cells (
                    session_id VARCHAR(100) NOT NULL,
                    cell VARCHAR(10) NOT NULL,
                    terrain_type VARCHAR(40) NOT NULL,
                    occupied_by VARCHAR(100),
                    PRIMARY KEY (session_id, cell)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE game_schema.game_grid_walls (
                    session_id VARCHAR(100) NOT NULL,
                    cell VARCHAR(10) NOT NULL,
                    direction VARCHAR(40) NOT NULL,
                    PRIMARY KEY (session_id, cell, direction)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE game_schema.game_grid_traps (
                    session_id VARCHAR(100) NOT NULL,
                    trap_id VARCHAR(100) NOT NULL,
                    cell VARCHAR(10) NOT NULL,
                    visibility VARCHAR(40) NOT NULL,
                    armed BOOLEAN NOT NULL,
                    PRIMARY KEY (session_id, trap_id)
                )
                """);
    }
}
