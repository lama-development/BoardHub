package it.uniupo.boardhub.eventservice.repository;

import it.uniupo.boardhub.eventservice.model.join.ParticipantRole;
import it.uniupo.boardhub.eventservice.model.join.ParticipantStatus;
import it.uniupo.boardhub.eventservice.model.join.SessionParticipant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SessionParticipantRepository {

    private final JdbcTemplate jdbcTemplate;

    public SessionParticipantRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Crea il partecipante soltanto dopo l'accettazione della richiesta.
    public void save(SessionParticipant participant) {
        jdbcTemplate.update("""
                        INSERT INTO game_schema.session_participants (
                            participant_id, join_request_id, session_id, player_reference,
                            role, display_name, status, joined_at, left_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                participant.participantId(),
                participant.joinRequestId(),
                participant.sessionId(),
                participant.playerReference(),
                participant.role().name(),
                participant.displayName(),
                participant.status().name(),
                Timestamp.from(participant.joinedAt().toInstant()),
                null
        );
    }

    public Optional<SessionParticipant> findByJoinRequestId(UUID requestId) {
        return first(jdbcTemplate.query("""
                SELECT participant_id, join_request_id, session_id, player_reference,
                       role, display_name, status, joined_at, left_at
                FROM game_schema.session_participants
                WHERE join_request_id = ?
                """, new ParticipantRowMapper(), requestId));
    }

    // Recupera il partecipante anche se ha lasciato la sessione, per validarne le credenziali.
    public Optional<SessionParticipant> findById(UUID participantId) {
        return first(jdbcTemplate.query("""
                SELECT participant_id, join_request_id, session_id, player_reference,
                       role, display_name, status, joined_at, left_at
                FROM game_schema.session_participants
                WHERE participant_id = ?
                """, new ParticipantRowMapper(), participantId));
    }

    // Blocca il partecipante mentre si verificano limiti o operazioni concorrenti di sua proprieta.
    public Optional<SessionParticipant> findByIdForUpdate(UUID participantId) {
        return first(jdbcTemplate.query("""
                SELECT participant_id, join_request_id, session_id, player_reference,
                       role, display_name, status, joined_at, left_at
                FROM game_schema.session_participants
                WHERE participant_id = ?
                FOR UPDATE
                """, new ParticipantRowMapper(), participantId));
    }

    public Optional<SessionParticipant> findBySessionAndPlayerReference(
            String sessionId,
            String playerReference
    ) {
        return first(jdbcTemplate.query("""
                SELECT participant_id, join_request_id, session_id, player_reference,
                       role, display_name, status, joined_at, left_at
                FROM game_schema.session_participants
                WHERE session_id = ? AND player_reference = ? AND status = 'ACTIVE'
                """, new ParticipantRowMapper(), sessionId, playerReference));
    }

    public List<SessionParticipant> findActiveBySessionId(String sessionId) {
        return jdbcTemplate.query("""
                SELECT participant_id, join_request_id, session_id, player_reference,
                       role, display_name, status, joined_at, left_at
                FROM game_schema.session_participants
                WHERE session_id = ? AND status = 'ACTIVE'
                ORDER BY joined_at ASC
                """, new ParticipantRowMapper(), sessionId);
    }

    public int countActivePlayers(String sessionId) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM game_schema.session_participants
                WHERE session_id = ? AND role = 'PLAYER' AND status = 'ACTIVE'
                """, Integer.class, sessionId);
        return count == null ? 0 : count;
    }

    public int closeActiveParticipants(String sessionId, java.time.OffsetDateTime leftAt) {
        return jdbcTemplate.update("""
                UPDATE game_schema.session_participants
                SET status = 'LEFT', left_at = ?
                WHERE session_id = ? AND status = 'ACTIVE'
                """, Timestamp.from(leftAt.toInstant()), sessionId);
    }

    private Optional<SessionParticipant> first(List<SessionParticipant> participants) {
        return participants.stream().findFirst();
    }

    private static class ParticipantRowMapper implements RowMapper<SessionParticipant> {

        @Override
        public SessionParticipant mapRow(ResultSet rs, int rowNum) throws SQLException {
            Timestamp leftAt = rs.getTimestamp("left_at");
            return new SessionParticipant(
                    rs.getObject("participant_id", UUID.class),
                    rs.getObject("join_request_id", UUID.class),
                    rs.getString("session_id"),
                    rs.getString("player_reference"),
                    ParticipantRole.valueOf(rs.getString("role")),
                    rs.getString("display_name"),
                    ParticipantStatus.valueOf(rs.getString("status")),
                    rs.getTimestamp("joined_at").toInstant().atOffset(ZoneOffset.UTC),
                    leftAt == null ? null : leftAt.toInstant().atOffset(ZoneOffset.UTC)
            );
        }
    }
}
