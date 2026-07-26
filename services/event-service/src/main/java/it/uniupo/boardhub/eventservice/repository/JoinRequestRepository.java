package it.uniupo.boardhub.eventservice.repository;

import it.uniupo.boardhub.eventservice.model.join.JoinRequestStatus;
import it.uniupo.boardhub.eventservice.model.join.SessionJoinRequest;
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
import java.util.UUID;

@Repository
public class JoinRequestRepository {

    private static final String SELECT_COLUMNS = """
            request_id, idempotency_key, request_fingerprint, session_id,
            player_reference, display_name, status, requested_at,
            expires_at, resolved_at
            """;

    private final JdbcTemplate jdbcTemplate;

    public JoinRequestRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Salva la richiesta mantenendo separata la chiave di idempotenza dal giocatore.
    public void save(SessionJoinRequest request) {
        jdbcTemplate.update("""
                        INSERT INTO game_schema.session_join_requests (
                            request_id, idempotency_key, request_fingerprint,
                            session_id, player_reference, pending_player_reference,
                            display_name, status, requested_at, expires_at, resolved_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                request.requestId(),
                request.idempotencyKey(),
                request.requestFingerprint(),
                request.sessionId(),
                request.playerReference(),
                request.playerReference(),
                request.displayName(),
                request.status().name(),
                Timestamp.from(request.requestedAt().toInstant()),
                Timestamp.from(request.expiresAt().toInstant()),
                null
        );
    }

    public Optional<SessionJoinRequest> findByIdempotencyKey(UUID idempotencyKey) {
        return first(jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM game_schema.session_join_requests WHERE idempotency_key = ?",
                new JoinRequestRowMapper(),
                idempotencyKey
        ));
    }

    // Blocca la singola richiesta durante la decisione del DM.
    public Optional<SessionJoinRequest> findByIdForUpdate(UUID requestId) {
        return first(jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM game_schema.session_join_requests WHERE request_id = ? FOR UPDATE",
                new JoinRequestRowMapper(),
                requestId
        ));
    }

    public List<SessionJoinRequest> findBySessionAndStatus(String sessionId, JoinRequestStatus status) {
        return jdbcTemplate.query(
                "SELECT " + SELECT_COLUMNS + " FROM game_schema.session_join_requests "
                        + "WHERE session_id = ? AND status = ? ORDER BY requested_at ASC",
                new JoinRequestRowMapper(),
                sessionId,
                status.name()
        );
    }

    // Rende definitive le richieste scadute e libera il vincolo sulla richiesta pendente.
    public int expireDueRequests(OffsetDateTime now) {
        Timestamp timestamp = Timestamp.from(now.toInstant());
        return jdbcTemplate.update("""
                UPDATE game_schema.session_join_requests
                SET status = 'EXPIRED', resolved_at = ?, pending_player_reference = NULL
                WHERE status = 'PENDING' AND expires_at <= ?
                """, timestamp, timestamp);
    }

    public int expirePendingForSession(String sessionId, OffsetDateTime now) {
        return jdbcTemplate.update("""
                UPDATE game_schema.session_join_requests
                SET status = 'EXPIRED', resolved_at = ?, pending_player_reference = NULL
                WHERE session_id = ? AND status = 'PENDING'
                """, Timestamp.from(now.toInstant()), sessionId);
    }

    public int countRecentRequests(String sessionId, String playerReference, OffsetDateTime since) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM game_schema.session_join_requests
                WHERE session_id = ? AND player_reference = ? AND requested_at >= ?
                """, Integer.class, sessionId, playerReference, Timestamp.from(since.toInstant()));
        return count == null ? 0 : count;
    }

    // Aggiorna lo stato solo se la richiesta e ancora pendente.
    public boolean resolve(UUID requestId, JoinRequestStatus status, OffsetDateTime resolvedAt) {
        return jdbcTemplate.update("""
                UPDATE game_schema.session_join_requests
                SET status = ?, resolved_at = ?, pending_player_reference = NULL
                WHERE request_id = ? AND status = 'PENDING'
                """, status.name(), Timestamp.from(resolvedAt.toInstant()), requestId) == 1;
    }

    private Optional<SessionJoinRequest> first(List<SessionJoinRequest> requests) {
        return requests.stream().findFirst();
    }

    private static class JoinRequestRowMapper implements RowMapper<SessionJoinRequest> {

        @Override
        public SessionJoinRequest mapRow(ResultSet rs, int rowNum) throws SQLException {
            Timestamp resolvedAt = rs.getTimestamp("resolved_at");
            return new SessionJoinRequest(
                    rs.getObject("request_id", UUID.class),
                    rs.getObject("idempotency_key", UUID.class),
                    rs.getString("request_fingerprint"),
                    rs.getString("session_id"),
                    rs.getString("player_reference"),
                    rs.getString("display_name"),
                    JoinRequestStatus.valueOf(rs.getString("status")),
                    rs.getTimestamp("requested_at").toInstant().atOffset(ZoneOffset.UTC),
                    rs.getTimestamp("expires_at").toInstant().atOffset(ZoneOffset.UTC),
                    resolvedAt == null ? null : resolvedAt.toInstant().atOffset(ZoneOffset.UTC)
            );
        }
    }
}
