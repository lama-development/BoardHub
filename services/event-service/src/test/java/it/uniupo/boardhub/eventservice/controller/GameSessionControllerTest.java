package it.uniupo.boardhub.eventservice.controller;

import it.uniupo.boardhub.eventservice.repository.GameSessionRepository;
import it.uniupo.boardhub.eventservice.service.GameSessionCreationService;
import it.uniupo.boardhub.eventservice.service.MovementGridFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GameSessionControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:boardhub_game_session_controller;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        createSchema(jdbcTemplate);

        GameSessionRepository repository = new GameSessionRepository(jdbcTemplate);
        GameSessionCreationService creationService = new GameSessionCreationService(repository, new MovementGridFactory());
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_SESSION"));
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
