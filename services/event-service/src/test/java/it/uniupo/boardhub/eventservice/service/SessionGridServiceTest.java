package it.uniupo.boardhub.eventservice.service;

import it.uniupo.boardhub.eventservice.model.grid.GridDirection;
import it.uniupo.boardhub.eventservice.model.grid.GridPosition;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionGridServiceTest {

    private GameSessionRepository repository;
    private SessionGridService service;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:boardhub_session_grid_service;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new GameSessionRepository(jdbcTemplate);
        service = new SessionGridService(repository);

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
    void ricostruisceGrigliaDaSessionePersistita() {
        String sessionId = "session-20260705-001";
        repository.saveSession(new GameSession(
                sessionId,
                "venue-01",
                "table-04",
                "Cripta del Re Caduto",
                "DND",
                GameSessionStatus.ACTIVE,
                6,
                6,
                OffsetDateTime.parse("2026-07-05T14:00:00Z")
        ));
        repository.saveCell(new GridCellState(sessionId, "A3", TerrainType.NORMAL, "adv-01"));
        repository.saveCell(new GridCellState(sessionId, "B3", TerrainType.DIFFICULT, null));
        repository.saveCell(new GridCellState(sessionId, "C4", TerrainType.OBSTACLE, null));
        repository.saveWall(new GridWallState(sessionId, "A3", GridDirection.EAST));
        repository.saveTrap(new GridTrapState(sessionId, "trap-01", "B4", TrapVisibility.HIDDEN, true));

        var grid = service.loadGrid(sessionId);

        assertThat(grid.width()).isEqualTo(6);
        assertThat(grid.height()).isEqualTo(6);
        assertThat(grid.cellAt(GridPosition.fromCell("A3")).occupied()).isTrue();
        assertThat(grid.cellAt(GridPosition.fromCell("B3")).terrainType()).isEqualTo(TerrainType.DIFFICULT);
        assertThat(grid.cellAt(GridPosition.fromCell("C4")).isWalkable()).isFalse();
        assertThat(grid.hasWallBetween(GridPosition.fromCell("A3"), GridPosition.fromCell("B3"))).isTrue();
        assertThat(grid.hasWallBetween(GridPosition.fromCell("B3"), GridPosition.fromCell("A3"))).isTrue();
        assertThat(grid.trapAt(GridPosition.fromCell("B4")).trapId()).isEqualTo("trap-01");
    }

    @Test
    void rifiutaSessioneInesistente() {
        assertThatThrownBy(() -> service.loadGrid("session-missing"))
                .isInstanceOf(GameSessionNotFoundException.class)
                .hasMessageContaining("Sessione non trovata");
    }
}
