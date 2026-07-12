package it.uniupo.boardhub.eventservice.repository;

import it.uniupo.boardhub.eventservice.model.grid.GridDirection;
import it.uniupo.boardhub.eventservice.model.grid.TerrainType;
import it.uniupo.boardhub.eventservice.model.grid.TrapVisibility;
import it.uniupo.boardhub.eventservice.model.session.GameSession;
import it.uniupo.boardhub.eventservice.model.session.GameSessionStatus;
import it.uniupo.boardhub.eventservice.model.session.GridCellState;
import it.uniupo.boardhub.eventservice.model.session.GridTrapState;
import it.uniupo.boardhub.eventservice.model.session.GridWallState;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class GameSessionRepository {

    private static final String INSERT_SESSION_SQL = """
            INSERT INTO game_schema.game_sessions (
                session_id,
                venue_id,
                table_id,
                title,
                game_type,
                status,
                grid_width,
                grid_height,
                created_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_CELL_SQL = """
            INSERT INTO game_schema.game_grid_cells (
                session_id,
                cell,
                terrain_type,
                occupied_by
            )
            VALUES (?, ?, ?, ?)
            """;

    private static final String INSERT_WALL_SQL = """
            INSERT INTO game_schema.game_grid_walls (
                session_id,
                cell,
                direction
            )
            VALUES (?, ?, ?)
            """;

    private static final String INSERT_TRAP_SQL = """
            INSERT INTO game_schema.game_grid_traps (
                session_id,
                trap_id,
                cell,
                visibility,
                armed
            )
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String FIND_SESSION_SQL = """
            SELECT
                session_id,
                venue_id,
                table_id,
                title,
                game_type,
                status,
                grid_width,
                grid_height,
                created_at
            FROM game_schema.game_sessions
            WHERE session_id = ?
            """;

    private static final String FIND_CELLS_SQL = """
            SELECT session_id, cell, terrain_type, occupied_by
            FROM game_schema.game_grid_cells
            WHERE session_id = ?
            ORDER BY cell ASC
            """;

    private static final String FIND_WALLS_SQL = """
            SELECT session_id, cell, direction
            FROM game_schema.game_grid_walls
            WHERE session_id = ?
            ORDER BY cell ASC, direction ASC
            """;

    private static final String FIND_TRAPS_SQL = """
            SELECT session_id, trap_id, cell, visibility, armed
            FROM game_schema.game_grid_traps
            WHERE session_id = ?
            ORDER BY trap_id ASC
            """;

    private final JdbcTemplate jdbcTemplate;

    public GameSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Salva i metadati principali di una sessione D&D.
    public void saveSession(GameSession session) {
        jdbcTemplate.update(
                INSERT_SESSION_SQL,
                session.sessionId(),
                session.venueId(),
                session.tableId(),
                session.title(),
                session.gameType(),
                session.status().name(),
                session.gridWidth(),
                session.gridHeight(),
                Timestamp.from(session.createdAt().toInstant())
        );
    }

    // Salva una cella configurata, ad esempio terreno difficile o cella occupata.
    public void saveCell(GridCellState cell) {
        jdbcTemplate.update(
                INSERT_CELL_SQL,
                cell.sessionId(),
                cell.cell(),
                cell.terrainType().name(),
                cell.occupiedBy()
        );
    }

    // Salva un muro sul bordo di una cella della sessione.
    public void saveWall(GridWallState wall) {
        jdbcTemplate.update(
                INSERT_WALL_SQL,
                wall.sessionId(),
                wall.cell(),
                wall.direction().name()
        );
    }

    // Salva una trappola configurata dal Dungeon Master.
    public void saveTrap(GridTrapState trap) {
        jdbcTemplate.update(
                INSERT_TRAP_SQL,
                trap.sessionId(),
                trap.trapId(),
                trap.cell(),
                trap.visibility().name(),
                trap.armed()
        );
    }

    // Recupera i metadati della sessione.
    public Optional<GameSession> findSessionById(String sessionId) {
        List<GameSession> sessions = jdbcTemplate.query(FIND_SESSION_SQL, new GameSessionRowMapper(), sessionId);
        return sessions.stream().findFirst();
    }

    // Recupera le celle configurate della griglia.
    public List<GridCellState> findCellsBySessionId(String sessionId) {
        return jdbcTemplate.query(FIND_CELLS_SQL, new GridCellStateRowMapper(), sessionId);
    }

    // Recupera i muri configurati della griglia.
    public List<GridWallState> findWallsBySessionId(String sessionId) {
        return jdbcTemplate.query(FIND_WALLS_SQL, new GridWallStateRowMapper(), sessionId);
    }

    // Recupera le trappole configurate della griglia.
    public List<GridTrapState> findTrapsBySessionId(String sessionId) {
        return jdbcTemplate.query(FIND_TRAPS_SQL, new GridTrapStateRowMapper(), sessionId);
    }

    // Converte una riga SQL nella sessione usata dal livello applicativo.
    private static class GameSessionRowMapper implements RowMapper<GameSession> {

        @Override
        public GameSession mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new GameSession(
                    rs.getString("session_id"),
                    rs.getString("venue_id"),
                    rs.getString("table_id"),
                    rs.getString("title"),
                    rs.getString("game_type"),
                    GameSessionStatus.valueOf(rs.getString("status")),
                    rs.getInt("grid_width"),
                    rs.getInt("grid_height"),
                    rs.getTimestamp("created_at").toInstant().atOffset(java.time.ZoneOffset.UTC)
            );
        }
    }

    // Converte le celle persistite nello stato della plancia.
    private static class GridCellStateRowMapper implements RowMapper<GridCellState> {

        @Override
        public GridCellState mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new GridCellState(
                    rs.getString("session_id"),
                    rs.getString("cell"),
                    TerrainType.valueOf(rs.getString("terrain_type")),
                    rs.getString("occupied_by")
            );
        }
    }

    // Converte i bordi bloccati persistiti in muri della sessione.
    private static class GridWallStateRowMapper implements RowMapper<GridWallState> {

        @Override
        public GridWallState mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new GridWallState(
                    rs.getString("session_id"),
                    rs.getString("cell"),
                    GridDirection.valueOf(rs.getString("direction"))
            );
        }
    }

    // Converte le trappole persistite nello stato letto dal movimento.
    private static class GridTrapStateRowMapper implements RowMapper<GridTrapState> {

        @Override
        public GridTrapState mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new GridTrapState(
                    rs.getString("session_id"),
                    rs.getString("trap_id"),
                    rs.getString("cell"),
                    TrapVisibility.valueOf(rs.getString("visibility")),
                    rs.getBoolean("armed")
            );
        }
    }
}
