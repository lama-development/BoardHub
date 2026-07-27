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
                            active_session_id, claim_expires_at, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                table.tableId(),
                table.tablePublicId(),
                table.displayName(),
                table.status().name(),
                table.activeSessionId(),
                toTimestamp(table.claimExpiresAt()),
                Timestamp.from(table.createdAt().toInstant()),
                Timestamp.from(table.updatedAt().toInstant())
        );
    }

    // Trova un tavolo tramite l'identificatore amministrativo interno.
    public Optional<GameTable> findById(String tableId) {
        List<GameTable> tables = jdbcTemplate.query("""
                SELECT table_id, table_public_id, display_name, status,
                       active_session_id, claim_expires_at, created_at, updated_at
                FROM game_schema.game_tables
                WHERE table_id = ?
                """, new GameTableRowMapper(), tableId);
        return tables.stream().findFirst();
    }

    // Trova il tavolo fisico tramite il riferimento stabile stampato nel QR.
    public Optional<GameTable> findByPublicId(String tablePublicId) {
        List<GameTable> tables = jdbcTemplate.query("""
                SELECT table_id, table_public_id, display_name, status,
                       active_session_id, claim_expires_at, created_at, updated_at
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
                WHERE status = 'IN_SESSION' AND active_session_id IS NOT NULL
                """,
                Integer.class
        );
        return count == null ? 0 : count;
    }

    // Rende reclamabile un tavolo gia registrato e non occupato.
    public boolean enableClaim(String tablePublicId, Timestamp expiresAt, Timestamp updatedAt) {
        return jdbcTemplate.update("""
                UPDATE game_schema.game_tables
                SET status = 'CLAIMABLE', claim_expires_at = ?, updated_at = ?
                WHERE table_public_id = ?
                  AND status IN ('DISABLED', 'CLAIMABLE')
                  AND active_session_id IS NULL
                """, expiresAt, updatedAt, tablePublicId) == 1;
    }

    // Disabilita una finestra di claim senza interrompere una sessione attiva.
    public boolean disableClaim(String tablePublicId, Timestamp updatedAt) {
        return jdbcTemplate.update("""
                UPDATE game_schema.game_tables
                SET status = 'DISABLED', claim_expires_at = NULL, updated_at = ?
                WHERE table_public_id = ?
                  AND status IN ('DISABLED', 'CLAIMABLE')
                  AND active_session_id IS NULL
                """, updatedAt, tablePublicId) == 1;
    }

    // Scade il claim soltanto se la finestra temporale e realmente terminata.
    public boolean expireClaim(String tablePublicId, Timestamp now) {
        return jdbcTemplate.update("""
                UPDATE game_schema.game_tables
                SET status = 'DISABLED', claim_expires_at = NULL, updated_at = ?
                WHERE table_public_id = ?
                  AND status = 'CLAIMABLE'
                  AND claim_expires_at <= ?
                """, now, tablePublicId, now) == 1;
    }

    // Collega atomicamente la sessione soltanto a un claim ancora valido.
    public boolean claimSession(String tableId, String sessionId, Timestamp now) {
        return jdbcTemplate.update("""
                UPDATE game_schema.game_tables
                SET status = 'IN_SESSION',
                    active_session_id = ?,
                    claim_expires_at = NULL,
                    updated_at = ?
                WHERE table_id = ?
                  AND status = 'CLAIMABLE'
                  AND active_session_id IS NULL
                  AND claim_expires_at > ?
                """, sessionId, now, tableId, now) == 1;
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
                  AND t.status = 'IN_SESSION'
                  AND s.status = 'ACTIVE'
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
                WHERE active_session_id = ? AND status = 'IN_SESSION'
                """, Integer.class, sessionId);
        return count != null && count == 1;
    }

    // Conclusa la sessione, il QR resta valido ma il locale deve riabilitare il tavolo.
    public boolean releaseSession(String sessionId, Timestamp updatedAt) {
        return jdbcTemplate.update("""
                UPDATE game_schema.game_tables
                SET status = 'DISABLED',
                    active_session_id = NULL,
                    claim_expires_at = NULL,
                    updated_at = ?
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
                    optionalDateTime(rs.getTimestamp("claim_expires_at")),
                    rs.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC),
                    rs.getTimestamp("updated_at").toInstant().atOffset(ZoneOffset.UTC)
            );
        }
    }

    private static Timestamp toTimestamp(java.time.OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private static java.time.OffsetDateTime optionalDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}
