package it.uniupo.boardhub.statsservice.repository;

import it.uniupo.boardhub.statsservice.model.PlayerResult;
import it.uniupo.boardhub.statsservice.model.PlayerStatistics;
import it.uniupo.boardhub.statsservice.model.SessionResult;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
public class SessionResultRepository {

    private final JdbcTemplate jdbcTemplate;

    public SessionResultRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Registra il risultato e ignora le consegne ripetute: con QoS 1 lo stesso
    // fatto puo arrivare piu volte e sessionId e la chiave primaria.
    public boolean save(SessionResult result) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO stats_schema.session_results (
                        session_id, venue_id, table_id, title, game_type,
                        started_at, ended_at, duration_minutes, participant_count
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    result.sessionId(), result.venueId(), result.tableId(),
                    result.title(), result.gameType(),
                    Timestamp.from(result.startedAt().toInstant()),
                    Timestamp.from(result.endedAt().toInstant()),
                    result.durationMinutes(), result.participants().size()
            );
        } catch (DuplicateKeyException alreadyKnown) {
            return false;
        }

        for (PlayerResult player : result.participants()) {
            jdbcTemplate.update("""
                    INSERT INTO stats_schema.player_results (
                        session_id, player_reference, display_name, character_name,
                        class_name, species, level, survived, moves_confirmed,
                        cells_travelled, traps_triggered, saves_succeeded,
                        saves_failed, damage_taken, points
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    result.sessionId(), player.playerReference(), player.displayName(),
                    player.characterName(), player.className(), player.species(),
                    player.level(), player.survived(), player.movesConfirmed(),
                    player.cellsTravelled(), player.trapsTriggered(),
                    player.savesSucceeded(), player.savesFailed(),
                    player.damageTaken(), player.points()
            );
        }
        return true;
    }

    public Optional<SessionResult> findBySessionId(String sessionId) {
        List<SessionResult> sessions = jdbcTemplate.query(
                "SELECT * FROM stats_schema.session_results WHERE session_id = ?",
                (rs, rowNum) -> mapSession(rs, findPlayers(sessionId)),
                sessionId
        );
        return sessions.stream().findFirst();
    }

    public List<SessionResult> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM stats_schema.session_results ORDER BY ended_at DESC",
                (rs, rowNum) -> mapSession(rs, List.of())
        );
    }

    public List<PlayerResult> findPlayers(String sessionId) {
        return jdbcTemplate.query(
                "SELECT * FROM stats_schema.player_results WHERE session_id = ? ORDER BY points DESC, display_name",
                PLAYER_MAPPER, sessionId
        );
    }

    public Optional<PlayerStatistics> findPlayerStatistics(String playerReference) {
        List<PlayerStatistics> found = jdbcTemplate.query("""
                SELECT
                    player_reference,
                    MAX(display_name) AS display_name,
                    COUNT(*) AS sessions_played,
                    SUM(CASE WHEN survived THEN 1 ELSE 0 END) AS sessions_survived,
                    SUM(points) AS total_points,
                    SUM(moves_confirmed) AS total_moves,
                    SUM(cells_travelled) AS total_cells,
                    SUM(traps_triggered) AS total_traps,
                    SUM(saves_succeeded) AS total_saves_ok,
                    SUM(saves_failed) AS total_saves_ko,
                    SUM(damage_taken) AS total_damage
                FROM stats_schema.player_results
                WHERE player_reference = ?
                GROUP BY player_reference
                """,
                (rs, rowNum) -> {
                    int played = rs.getInt("sessions_played");
                    int survived = rs.getInt("sessions_survived");
                    return new PlayerStatistics(
                            rs.getString("player_reference"),
                            rs.getString("display_name"),
                            played,
                            survived,
                            played == 0 ? 0.0 : Math.round((survived * 1000.0) / played) / 10.0,
                            rs.getInt("total_points"),
                            rs.getInt("total_moves"),
                            rs.getInt("total_cells"),
                            rs.getInt("total_traps"),
                            rs.getInt("total_saves_ok"),
                            rs.getInt("total_saves_ko"),
                            rs.getInt("total_damage")
                    );
                },
                playerReference
        );
        return found.stream().findFirst();
    }

    public List<String> findSessionIdsFor(String gameType, String venueId) {
        return jdbcTemplate.queryForList(
                "SELECT session_id FROM stats_schema.session_results WHERE game_type = ? AND venue_id = ?",
                String.class, gameType, venueId
        );
    }

    private SessionResult mapSession(ResultSet rs, List<PlayerResult> players) throws SQLException {
        return new SessionResult(
                rs.getString("session_id"),
                rs.getString("venue_id"),
                rs.getString("table_id"),
                rs.getString("title"),
                rs.getString("game_type"),
                rs.getTimestamp("started_at").toInstant().atOffset(ZoneOffset.UTC),
                rs.getTimestamp("ended_at").toInstant().atOffset(ZoneOffset.UTC),
                rs.getLong("duration_minutes"),
                players
        );
    }

    private static final RowMapper<PlayerResult> PLAYER_MAPPER = (rs, rowNum) -> new PlayerResult(
            rs.getString("session_id"),
            rs.getString("player_reference"),
            rs.getString("display_name"),
            rs.getString("character_name"),
            rs.getString("class_name"),
            rs.getString("species"),
            rs.getInt("level"),
            rs.getBoolean("survived"),
            rs.getInt("moves_confirmed"),
            rs.getInt("cells_travelled"),
            rs.getInt("traps_triggered"),
            rs.getInt("saves_succeeded"),
            rs.getInt("saves_failed"),
            rs.getInt("damage_taken"),
            rs.getInt("points")
    );
}
