package it.uniupo.boardhub.eventservice.repository;

import it.uniupo.boardhub.eventservice.model.join.GameTable;
import it.uniupo.boardhub.eventservice.model.join.GameTableStatus;
import it.uniupo.boardhub.eventservice.model.join.PublicSessionInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
public class GameTableRepository {

    private final JdbcTemplate jdbcTemplate;

    public GameTableRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Registra una sola volta il tavolo fisico e il riferimento contenuto nel QR.
    public void save(GameTable table) {
        jdbcTemplate.update("""
                        INSERT INTO game_schema.game_tables (
                            table_id, table_public_id, display_name, status,
                            active_session_id, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                table.tableId(),
                table.tablePublicId(),
                table.displayName(),
                table.status().name(),
                table.activeSessionId(),
                Timestamp.from(table.createdAt().toInstant()),
                Timestamp.from(table.updatedAt().toInstant())
        );
    }

    // Trova un tavolo tramite l'identificatore amministrativo interno.
    public Optional<GameTable> findById(String tableId) {
        List<GameTable> tables = jdbcTemplate.query("""
                SELECT table_id, table_public_id, display_name, status,
                       active_session_id, created_at, updated_at
                FROM game_schema.game_tables
                WHERE table_id = ?
                """, new GameTableRowMapper(), tableId);
        return tables.stream().findFirst();
    }

    // Trova il tavolo fisico tramite il riferimento stabile stampato nel QR.
    public Optional<GameTable> findByPublicId(String tablePublicId) {
        List<GameTable> tables = jdbcTemplate.query("""
                SELECT table_id, table_public_id, display_name, status,
                       active_session_id, created_at, updated_at
                FROM game_schema.game_tables
                WHERE table_public_id = ?
                """, new GameTableRowMapper(), tablePublicId);
        return tables.stream().findFirst();
    }

    // Conta i tavoli che ospitano davvero una sessione, non tutti quelli registrati.
    public int countActiveTables() {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM game_schema.game_tables
                WHERE status = 'ACTIVE' AND active_session_id IS NOT NULL
                """,
                Integer.class
        );
        return count == null ? 0 : count;
    }

    // Collega il tavolo alla sessione solo se non e gia occupato da un'altra partita.
    public boolean activateSession(String tableId, String sessionId, Timestamp updatedAt) {
        return jdbcTemplate.update("""
                UPDATE game_schema.game_tables
                SET active_session_id = ?, updated_at = ?
                WHERE table_id = ?
                  AND status = 'ACTIVE'
                  AND (active_session_id IS NULL OR active_session_id = ?)
                """, sessionId, updatedAt, tableId, sessionId) == 1;
    }

    // Risolve il QR in una proiezione pubblica priva di mappa, partecipanti e segreti.
    public Optional<PublicSessionInfo> findActivePublicSession(String tablePublicId) {
        List<PublicSessionInfo> sessions = jdbcTemplate.query("""
                SELECT t.table_public_id,
                       t.display_name AS table_display_name,
                       s.session_id,
                       s.title,
                       s.game_type,
                       s.public_summary
                FROM game_schema.game_tables t
                JOIN game_schema.game_sessions s
                  ON s.session_id = t.active_session_id
                WHERE t.table_public_id = ?
                  AND t.status = 'ACTIVE'
                  AND s.status = 'ACTIVE'
                  AND s.accepting_join_requests = TRUE
                """, (rs, rowNum) -> new PublicSessionInfo(
                rs.getString("table_public_id"),
                rs.getString("table_display_name"),
                rs.getString("session_id"),
                rs.getString("title"),
                rs.getString("game_type"),
                rs.getString("public_summary")
        ), tablePublicId);
        return sessions.stream().findFirst();
    }

    // Verifica che la sessione sia ancora collegata a un tavolo disponibile.
    public boolean isActiveSession(String sessionId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM game_schema.game_tables
                WHERE active_session_id = ? AND status = 'ACTIVE'
                """, Integer.class, sessionId);
        return count != null && count == 1;
    }

    // Libera il tavolo soltanto dalla sessione indicata, senza disattivare il QR.
    public boolean releaseSession(String sessionId, Timestamp updatedAt) {
        return jdbcTemplate.update("""
                UPDATE game_schema.game_tables
                SET active_session_id = NULL, updated_at = ?
                WHERE active_session_id = ?
                """, updatedAt, sessionId) == 1;
    }

    private static class GameTableRowMapper implements RowMapper<GameTable> {

        @Override
        public GameTable mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new GameTable(
                    rs.getString("table_id"),
                    rs.getString("table_public_id"),
                    rs.getString("display_name"),
                    GameTableStatus.valueOf(rs.getString("status")),
                    rs.getString("active_session_id"),
                    rs.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC),
                    rs.getTimestamp("updated_at").toInstant().atOffset(ZoneOffset.UTC)
            );
        }
    }
}
