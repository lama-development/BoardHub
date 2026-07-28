package it.uniupo.boardhub.eventservice.repository;

import it.uniupo.boardhub.eventservice.model.piece.RepresentationMode;
import it.uniupo.boardhub.eventservice.model.piece.SessionPiece;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Repository
public class SessionPieceRepository {

    private static final String SELECT_FIELDS = """
            SELECT session_piece_id, session_id, character_id, participant_id,
                   representation_mode, current_cell, version, created_at, updated_at
            FROM game_schema.session_pieces
            """;

    private final JdbcTemplate jdbcTemplate;

    public SessionPieceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(SessionPiece piece) {
        jdbcTemplate.update("""
                        INSERT INTO game_schema.session_pieces (
                            session_piece_id, session_id, character_id, participant_id,
                            representation_mode, current_cell, version, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                piece.sessionPieceId(),
                piece.sessionId(),
                piece.characterId(),
                piece.participantId(),
                piece.representationMode().name(),
                piece.currentCell(),
                piece.version(),
                Timestamp.from(piece.createdAt().toInstant()),
                Timestamp.from(piece.updatedAt().toInstant())
        );
    }

    public boolean existsByCharacter(String sessionId, UUID characterId) {
        return count("""
                SELECT COUNT(*)
                FROM game_schema.session_pieces
                WHERE session_id = ? AND character_id = ?
                """, sessionId, characterId) > 0;
    }

    public List<SessionPiece> findByParticipant(String sessionId, UUID participantId) {
        return jdbcTemplate.query(
                SELECT_FIELDS + """
                        WHERE session_id = ? AND participant_id = ?
                        ORDER BY created_at ASC, session_piece_id ASC
                        """,
                new SessionPieceRowMapper(),
                sessionId,
                participantId
        );
    }

    public List<SessionPiece> findBySession(String sessionId) {
        return jdbcTemplate.query(
                SELECT_FIELDS + """
                        WHERE session_id = ?
                        ORDER BY created_at ASC, session_piece_id ASC
                        """,
                new SessionPieceRowMapper(),
                sessionId
        );
    }

    private int count(String sql, Object... parameters) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, parameters);
        return count == null ? 0 : count;
    }

    private static class SessionPieceRowMapper implements RowMapper<SessionPiece> {

        @Override
        public SessionPiece mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new SessionPiece(
                    rs.getObject("session_piece_id", UUID.class),
                    rs.getString("session_id"),
                    rs.getObject("character_id", UUID.class),
                    rs.getObject("participant_id", UUID.class),
                    RepresentationMode.valueOf(rs.getString("representation_mode")),
                    rs.getString("current_cell"),
                    rs.getLong("version"),
                    rs.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC),
                    rs.getTimestamp("updated_at").toInstant().atOffset(ZoneOffset.UTC)
            );
        }
    }
}
