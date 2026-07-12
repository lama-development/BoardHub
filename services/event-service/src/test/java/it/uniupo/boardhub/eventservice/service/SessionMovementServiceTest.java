package it.uniupo.boardhub.eventservice.service;

import it.uniupo.boardhub.eventservice.model.grid.GridDirection;
import it.uniupo.boardhub.eventservice.model.grid.GridPosition;
import it.uniupo.boardhub.eventservice.model.grid.MovementRequest;
import it.uniupo.boardhub.eventservice.model.grid.TerrainType;
import it.uniupo.boardhub.eventservice.model.grid.TrapVisibility;
import it.uniupo.boardhub.eventservice.model.session.GameSession;
import it.uniupo.boardhub.eventservice.model.session.GameSessionStatus;
import it.uniupo.boardhub.eventservice.model.session.GridCellState;
import it.uniupo.boardhub.eventservice.model.session.GridTrapState;
import it.uniupo.boardhub.eventservice.model.session.GridWallState;
import it.uniupo.boardhub.eventservice.repository.GameSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SessionMovementServiceTest {

    private GameSessionRepository repository;
    private SessionMovementService service;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:boardhub_session_movement_service;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new GameSessionRepository(jdbcTemplate);
        service = new SessionMovementService(
                new SessionGridService(repository),
                new MovementService()
        );

        createSchema(jdbcTemplate);
    }

    @Test
    void usaGrigliaPersistitaPerCalcolareMovimento() {
        String sessionId = "session-20260705-001";
        repository.saveSession(new GameSession(
                sessionId,
                "venue-01",
                "table-04",
                "Cripta del Re Caduto",
                "DND",
                GameSessionStatus.ACTIVE,
                3,
                3,
                OffsetDateTime.parse("2026-07-05T14:00:00Z")
        ));
        repository.saveCell(new GridCellState(sessionId, "C1", TerrainType.DIFFICULT, null));
        repository.saveCell(new GridCellState(sessionId, "A2", TerrainType.BLOCKED, null));
        repository.saveWall(new GridWallState(sessionId, "B1", GridDirection.SOUTH));
        repository.saveTrap(new GridTrapState(sessionId, "trap-01", "B1", TrapVisibility.HIDDEN, true));

        var cells = service.calculateReachableCells(
                sessionId,
                new MovementRequest("adv-01", GridPosition.fromCell("A1"), 2)
        );

        assertThat(cells).extracting(cell -> cell.position().toCell())
                .contains("A1", "B1")
                .doesNotContain("A2", "B2", "C1");
        assertThat(cells.stream()
                .filter(cell -> cell.position().toCell().equals("B1"))
                .findFirst()
                .orElseThrow()
                .trapsOnPath())
                .extracting("trapId")
                .containsExactly("trap-01");
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
