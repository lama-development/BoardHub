package it.uniupo.boardhub.eventservice.repository;

import it.uniupo.boardhub.eventservice.model.grid.GridDirection;
import it.uniupo.boardhub.eventservice.model.grid.TerrainType;
import it.uniupo.boardhub.eventservice.model.grid.TrapVisibility;
import it.uniupo.boardhub.eventservice.model.session.GameSession;
import it.uniupo.boardhub.eventservice.model.session.GameSessionStatus;
import it.uniupo.boardhub.eventservice.model.session.GridCellState;
import it.uniupo.boardhub.eventservice.model.session.GridTrapState;
import it.uniupo.boardhub.eventservice.model.session.GridWallState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class GameSessionRepositoryTest {

    private GameSessionRepository repository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:boardhub_session_repository;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new GameSessionRepository(jdbcTemplate);

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

    @Test
    void salvaELeggeSessioneConStatoPlancia() {
        GameSession session = new GameSession(
                "session-20260705-001",
                "venue-01",
                "table-04",
                "Cripta del Re Caduto",
                "DND",
                GameSessionStatus.ACTIVE,
                6,
                6,
                OffsetDateTime.parse("2026-07-05T14:00:00Z")
        );

        repository.saveSession(session);
        repository.saveCell(new GridCellState(session.sessionId(), "B3", TerrainType.DIFFICULT, null));
        repository.saveCell(new GridCellState(session.sessionId(), "C4", TerrainType.OBSTACLE, null));
        repository.saveCell(new GridCellState(session.sessionId(), "A3", TerrainType.NORMAL, "adv-01"));
        repository.saveWall(new GridWallState(session.sessionId(), "A3", GridDirection.EAST));
        repository.saveTrap(new GridTrapState(session.sessionId(), "trap-01", "B4", TrapVisibility.HIDDEN, true));

        var savedSession = repository.findSessionById(session.sessionId());
        var cells = repository.findCellsBySessionId(session.sessionId());
        var walls = repository.findWallsBySessionId(session.sessionId());
        var traps = repository.findTrapsBySessionId(session.sessionId());

        assertThat(savedSession).contains(session);
        assertThat(cells).containsExactly(
                new GridCellState(session.sessionId(), "A3", TerrainType.NORMAL, "adv-01"),
                new GridCellState(session.sessionId(), "B3", TerrainType.DIFFICULT, null),
                new GridCellState(session.sessionId(), "C4", TerrainType.OBSTACLE, null)
        );
        assertThat(walls).containsExactly(new GridWallState(session.sessionId(), "A3", GridDirection.EAST));
        assertThat(traps).containsExactly(
                new GridTrapState(session.sessionId(), "trap-01", "B4", TrapVisibility.HIDDEN, true)
        );
    }
}
