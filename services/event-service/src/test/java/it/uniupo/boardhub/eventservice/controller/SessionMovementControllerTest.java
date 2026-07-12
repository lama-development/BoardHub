package it.uniupo.boardhub.eventservice.controller;

import it.uniupo.boardhub.eventservice.model.grid.GridDirection;
import it.uniupo.boardhub.eventservice.model.grid.TerrainType;
import it.uniupo.boardhub.eventservice.model.grid.TrapVisibility;
import it.uniupo.boardhub.eventservice.model.session.GameSession;
import it.uniupo.boardhub.eventservice.model.session.GameSessionStatus;
import it.uniupo.boardhub.eventservice.model.session.GridCellState;
import it.uniupo.boardhub.eventservice.model.session.GridTrapState;
import it.uniupo.boardhub.eventservice.model.session.GridWallState;
import it.uniupo.boardhub.eventservice.repository.GameSessionRepository;
import it.uniupo.boardhub.eventservice.service.MovementService;
import it.uniupo.boardhub.eventservice.service.SessionGridService;
import it.uniupo.boardhub.eventservice.service.SessionMovementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SessionMovementControllerTest {

    private MockMvc mockMvc;
    private GameSessionRepository repository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:boardhub_session_movement_controller;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new GameSessionRepository(jdbcTemplate);
        createSchema(jdbcTemplate);

        SessionGridService sessionGridService = new SessionGridService(repository);
        SessionMovementService sessionMovementService = new SessionMovementService(
                sessionGridService,
                new MovementService()
        );
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SessionMovementController(sessionMovementService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void calcolaCelleRaggiungibiliDaSessioneSalvata() throws Exception {
        String sessionId = "session-20260705-001";
        saveDemoSession(sessionId);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/movement/reachable-cells", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "characterId": "adv-01",
                                  "start": "A1",
                                  "movementPoints": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.characterId").value("adv-01"))
                .andExpect(jsonPath("$.reachableCells[?(@.cell == 'A1')]").exists())
                .andExpect(jsonPath("$.reachableCells[?(@.cell == 'B1')]").exists())
                .andExpect(jsonPath("$.reachableCells[?(@.cell == 'C1')]").doesNotExist())
                .andExpect(jsonPath("$.reachableCells[?(@.cell == 'A2')]").doesNotExist())
                .andExpect(jsonPath("$.reachableCells[?(@.cell == 'B2')]").doesNotExist())
                .andExpect(jsonPath("$.reachableCells[1].cell").value("B1"))
                .andExpect(jsonPath("$.reachableCells[1].trapsOnPath").isEmpty());
    }

    @Test
    void restituisceNotFoundSeSessioneNonEsiste() throws Exception {
        mockMvc.perform(post("/api/v1/sessions/{sessionId}/movement/reachable-cells", "session-non-esistente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "characterId": "adv-01",
                                  "start": "A1",
                                  "movementPoints": 2
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));
    }

    private void saveDemoSession(String sessionId) {
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
