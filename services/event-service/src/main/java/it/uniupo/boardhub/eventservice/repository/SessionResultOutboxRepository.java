package it.uniupo.boardhub.eventservice.repository;

import it.uniupo.boardhub.eventservice.model.result.SessionResultOutboxEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class SessionResultOutboxRepository {

    private final JdbcTemplate jdbcTemplate;

    public SessionResultOutboxRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Registra il fatto nella transazione che conclude la sessione.
    public void enqueue(
            String factId,
            String sessionId,
            String topic,
            String payloadJson,
            OffsetDateTime createdAt
    ) {
        jdbcTemplate.update("""
                INSERT INTO game_schema.session_result_outbox (
                    fact_id, session_id, topic, payload_json, status,
                    attempt_count, next_attempt_at, created_at
                ) VALUES (?, ?, ?, ?, 'PENDING', 0, ?, ?)
                """,
                factId,
                sessionId,
                topic,
                payloadJson,
                Timestamp.from(createdAt.toInstant()),
                Timestamp.from(createdAt.toInstant())
        );
    }

    public List<SessionResultOutboxEntry> findDue(OffsetDateTime now, int limit) {
        return jdbcTemplate.query("""
                        SELECT fact_id, session_id, topic, payload_json,
                               attempt_count, next_attempt_at
                        FROM game_schema.session_result_outbox
                        WHERE status = 'PENDING' AND next_attempt_at <= ?
                        ORDER BY created_at, fact_id
                        LIMIT ?
                        """,
                (rs, rowNum) -> new SessionResultOutboxEntry(
                        rs.getString("fact_id"),
                        rs.getString("session_id"),
                        rs.getString("topic"),
                        rs.getString("payload_json"),
                        rs.getInt("attempt_count"),
                        rs.getTimestamp("next_attempt_at").toInstant().atOffset(java.time.ZoneOffset.UTC)
                ),
                Timestamp.from(now.toInstant()),
                limit
        );
    }

    public boolean markPublished(String factId, OffsetDateTime publishedAt) {
        return jdbcTemplate.update("""
                UPDATE game_schema.session_result_outbox
                SET status = 'PUBLISHED', published_at = ?, last_error = NULL
                WHERE fact_id = ? AND status = 'PENDING'
                """, Timestamp.from(publishedAt.toInstant()), factId) == 1;
    }

    public void scheduleRetry(
            String factId,
            int attemptCount,
            OffsetDateTime nextAttemptAt,
            String error
    ) {
        jdbcTemplate.update("""
                UPDATE game_schema.session_result_outbox
                SET attempt_count = ?, next_attempt_at = ?, last_error = ?
                WHERE fact_id = ? AND status = 'PENDING'
                """,
                attemptCount,
                Timestamp.from(nextAttemptAt.toInstant()),
                abbreviate(error),
                factId
        );
    }

    private String abbreviate(String error) {
        if (error == null || error.length() <= 500) {
            return error;
        }
        return error.substring(0, 500);
    }
}
