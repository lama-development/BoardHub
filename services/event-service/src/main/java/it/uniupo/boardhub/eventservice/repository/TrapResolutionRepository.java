package it.uniupo.boardhub.eventservice.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.boardhub.eventservice.model.trap.TrapDefinition;
import it.uniupo.boardhub.eventservice.model.trap.TrapResolution;
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
public class TrapResolutionRepository {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };
    private static final TypeReference<List<Integer>> INTEGER_LIST = new TypeReference<>() { };
    private static final String SELECT = """
            SELECT resolution_id, session_id, trap_id, session_piece_id, character_id,
                   participant_id, move_command_id, expected_piece_version, status, from_cell,
                   requested_destination, trigger_cell, path_json, remaining_path_json,
                   movement_budget, cost_to_trigger, movement_remaining, observed_physical_cell,
                   save_d20_first, save_d20_second, save_selected, save_bonus, save_total,
                   save_success, damage_rolls_json, damage_total, movement_decision,
                   roll_command_id, roll_fingerprint, continue_command_id, version,
                   created_at, updated_at, completed_at
            FROM game_schema.trap_resolutions
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public TrapResolutionRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void save(TrapResolution resolution) {
        jdbcTemplate.update("""
                INSERT INTO game_schema.trap_resolutions (
                    resolution_id, session_id, trap_id, session_piece_id, character_id,
                    participant_id, move_command_id, expected_piece_version, status, from_cell,
                    requested_destination, trigger_cell, path_json, remaining_path_json,
                    movement_budget, cost_to_trigger, movement_remaining, observed_physical_cell,
                    save_d20_first, save_d20_second, save_selected, save_bonus, save_total,
                    save_success, damage_rolls_json, damage_total, movement_decision,
                    roll_command_id, roll_fingerprint, continue_command_id, version,
                    created_at, updated_at, completed_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                resolution.resolutionId(), resolution.sessionId(), resolution.trapId(),
                resolution.sessionPieceId(), resolution.characterId(), resolution.participantId(),
                resolution.moveCommandId(), resolution.expectedPieceVersion(), resolution.status().name(),
                resolution.fromCell(), resolution.requestedDestination(), resolution.triggerCell(),
                json(resolution.path()), json(resolution.remainingPath()), resolution.movementBudget(),
                resolution.costToTrigger(), resolution.movementRemaining(), resolution.observedPhysicalCell(),
                resolution.saveD20First(), resolution.saveD20Second(), resolution.saveSelected(),
                resolution.saveBonus(), resolution.saveTotal(), resolution.saveSuccess(),
                resolution.damageRolls().isEmpty() ? null : json(resolution.damageRolls()),
                resolution.damageTotal(), name(resolution.movementDecision()), resolution.rollCommandId(),
                resolution.rollFingerprint(), resolution.continueCommandId(), resolution.version(),
                timestamp(resolution.createdAt()), timestamp(resolution.updatedAt()),
                timestamp(resolution.completedAt())
        );
    }

    public Optional<TrapResolution> findByMoveCommand(UUID commandId) {
        return queryOne(SELECT + " WHERE move_command_id = ?", commandId);
    }

    public Optional<TrapResolution> findById(String sessionId, UUID resolutionId) {
        return queryOne(SELECT + " WHERE session_id = ? AND resolution_id = ?", sessionId, resolutionId);
    }

    public Optional<TrapResolution> findByIdForUpdate(String sessionId, UUID resolutionId) {
        return queryOne(
                SELECT + " WHERE session_id = ? AND resolution_id = ? FOR UPDATE",
                sessionId, resolutionId
        );
    }

    public Optional<TrapResolution> findPendingByPiece(String sessionId, UUID pieceId) {
        return queryOne(SELECT + """
                WHERE session_id = ? AND session_piece_id = ?
                  AND status NOT IN ('COMPLETED', 'CANCELLED_SESSION_ENDED')
                ORDER BY created_at DESC LIMIT 1
                """, sessionId, pieceId);
    }

    // Serve al riepilogo di fine sessione, che considera tutte le risoluzioni.
    public List<TrapResolution> findAllBySession(String sessionId) {
        return jdbcTemplate.query(SELECT + """
                WHERE session_id = ?
                ORDER BY created_at ASC, resolution_id ASC
                """, new ResolutionRowMapper(), sessionId);
    }

    public List<TrapResolution> findPendingBySession(String sessionId) {
        return jdbcTemplate.query(SELECT + """
                WHERE session_id = ?
                  AND status NOT IN ('COMPLETED', 'CANCELLED_SESSION_ENDED')
                ORDER BY created_at ASC, resolution_id ASC
                """, new ResolutionRowMapper(), sessionId);
    }

    public boolean recordRoll(
            UUID resolutionId,
            long expectedVersion,
            UUID commandId,
            String fingerprint,
            int d20First,
            Integer d20Second,
            int selected,
            int bonus,
            int total,
            boolean success,
            List<Integer> damageRolls,
            int damageTotal,
            TrapDefinition.MovementDecision movementDecision,
            TrapResolution.Status status,
            OffsetDateTime now
    ) {
        return jdbcTemplate.update("""
                UPDATE game_schema.trap_resolutions
                SET save_d20_first = ?, save_d20_second = ?, save_selected = ?, save_bonus = ?,
                    save_total = ?, save_success = ?, damage_rolls_json = ?, damage_total = ?,
                    movement_decision = ?, roll_command_id = ?, roll_fingerprint = ?, status = ?,
                    version = version + 1, updated_at = ?,
                    completed_at = CASE WHEN ? = 'COMPLETED' THEN ? ELSE completed_at END
                WHERE resolution_id = ? AND version = ? AND status = 'AWAITING_SAVE_ROLL'
                """,
                d20First, d20Second, selected, bonus, total, success, json(damageRolls), damageTotal,
                movementDecision.name(), commandId, fingerprint, status.name(), timestamp(now),
                status.name(), timestamp(now), resolutionId, expectedVersion
        ) == 1;
    }

    public boolean completeContinuation(
            UUID resolutionId,
            long expectedVersion,
            UUID commandId,
            OffsetDateTime now
    ) {
        return jdbcTemplate.update("""
                UPDATE game_schema.trap_resolutions
                SET status = 'COMPLETED', continue_command_id = ?, version = version + 1,
                    updated_at = ?, completed_at = ?
                WHERE resolution_id = ? AND version = ? AND status = 'CONTINUATION_ALLOWED'
                """,
                commandId, timestamp(now), timestamp(now), resolutionId, expectedVersion
        ) == 1;
    }

    public int cancelPendingForSession(String sessionId, OffsetDateTime now) {
        return jdbcTemplate.update("""
                UPDATE game_schema.trap_resolutions
                SET status = 'CANCELLED_SESSION_ENDED', version = version + 1,
                    updated_at = ?, completed_at = ?
                WHERE session_id = ? AND status NOT IN ('COMPLETED', 'CANCELLED_SESSION_ENDED')
                """, timestamp(now), timestamp(now), sessionId);
    }

    private Optional<TrapResolution> queryOne(String sql, Object... args) {
        return jdbcTemplate.query(sql, new ResolutionRowMapper(), args).stream().findFirst();
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Impossibile serializzare la risoluzione della trappola.", ex);
        }
    }

    private <T> List<T> readList(String value, TypeReference<List<T>> type) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Risoluzione della trappola non leggibile.", ex);
        }
    }

    private String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private Timestamp timestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private class ResolutionRowMapper implements RowMapper<TrapResolution> {
        @Override
        public TrapResolution mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new TrapResolution(
                    rs.getObject("resolution_id", UUID.class), rs.getString("session_id"),
                    rs.getString("trap_id"), rs.getObject("session_piece_id", UUID.class),
                    rs.getObject("character_id", UUID.class), rs.getObject("participant_id", UUID.class),
                    rs.getObject("move_command_id", UUID.class), rs.getLong("expected_piece_version"),
                    TrapResolution.Status.valueOf(rs.getString("status")), rs.getString("from_cell"),
                    rs.getString("requested_destination"), rs.getString("trigger_cell"),
                    readList(rs.getString("path_json"), STRING_LIST),
                    readList(rs.getString("remaining_path_json"), STRING_LIST),
                    rs.getInt("movement_budget"), rs.getInt("cost_to_trigger"),
                    rs.getInt("movement_remaining"), rs.getString("observed_physical_cell"),
                    integer(rs, "save_d20_first"), integer(rs, "save_d20_second"),
                    integer(rs, "save_selected"), integer(rs, "save_bonus"), integer(rs, "save_total"),
                    bool(rs, "save_success"), readList(rs.getString("damage_rolls_json"), INTEGER_LIST),
                    integer(rs, "damage_total"), enumValue(rs.getString("movement_decision")),
                    rs.getObject("roll_command_id", UUID.class), rs.getString("roll_fingerprint"),
                    rs.getObject("continue_command_id", UUID.class), rs.getLong("version"),
                    time(rs, "created_at"), time(rs, "updated_at"), time(rs, "completed_at")
            );
        }

        private Integer integer(ResultSet rs, String field) throws SQLException {
            int value = rs.getInt(field);
            return rs.wasNull() ? null : value;
        }

        private Boolean bool(ResultSet rs, String field) throws SQLException {
            boolean value = rs.getBoolean(field);
            return rs.wasNull() ? null : value;
        }

        private OffsetDateTime time(ResultSet rs, String field) throws SQLException {
            Timestamp value = rs.getTimestamp(field);
            return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
        }

        private TrapDefinition.MovementDecision enumValue(String value) {
            return value == null ? null : TrapDefinition.MovementDecision.valueOf(value);
        }
    }
}
