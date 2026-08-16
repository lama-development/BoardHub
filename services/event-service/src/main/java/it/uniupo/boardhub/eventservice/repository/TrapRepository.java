package it.uniupo.boardhub.eventservice.repository;

import it.uniupo.boardhub.eventservice.model.grid.TrapVisibility;
import it.uniupo.boardhub.eventservice.model.trap.TrapDefinition;
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
public class TrapRepository {

    private static final String SELECT = """
            SELECT session_id, trap_id, cell, visibility, lifecycle_policy, lifecycle_state,
                   save_ability, save_dc, roll_mode, damage_expression, success_damage,
                   success_movement, failure_damage, failure_movement, version, updated_at
            FROM game_schema.game_grid_traps
            """;

    private final JdbcTemplate jdbcTemplate;

    public TrapRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TrapDefinition> findBySession(String sessionId) {
        return jdbcTemplate.query(
                SELECT + " WHERE session_id = ? ORDER BY cell ASC, trap_id ASC",
                new TrapRowMapper(), sessionId
        );
    }

    public Optional<TrapDefinition> findBySessionAndCell(String sessionId, String cell) {
        return jdbcTemplate.query(
                SELECT + " WHERE session_id = ? AND cell = ?",
                new TrapRowMapper(), sessionId, cell
        ).stream().findFirst();
    }

    public Optional<TrapDefinition> findByIdForUpdate(String sessionId, String trapId) {
        return jdbcTemplate.query(
                SELECT + " WHERE session_id = ? AND trap_id = ? FOR UPDATE",
                new TrapRowMapper(), sessionId, trapId
        ).stream().findFirst();
    }

    public boolean markTriggered(
            String sessionId,
            String trapId,
            long expectedVersion,
            TrapDefinition.LifecycleState state,
            TrapVisibility visibility,
            OffsetDateTime updatedAt
    ) {
        boolean armed = state == TrapDefinition.LifecycleState.ARMED
                || state == TrapDefinition.LifecycleState.TRIGGERED_ACTIVE;
        return jdbcTemplate.update("""
                UPDATE game_schema.game_grid_traps
                SET lifecycle_state = ?, visibility = ?, armed = ?,
                    version = version + 1, updated_at = ?
                WHERE session_id = ? AND trap_id = ? AND version = ?
                """,
                state.name(), visibility.normalized().name(), armed,
                Timestamp.from(updatedAt.toInstant()), sessionId, trapId, expectedVersion
        ) == 1;
    }

    private static class TrapRowMapper implements RowMapper<TrapDefinition> {
        @Override
        public TrapDefinition mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new TrapDefinition(
                    rs.getString("session_id"),
                    rs.getString("trap_id"),
                    rs.getString("cell"),
                    TrapVisibility.valueOf(rs.getString("visibility")),
                    TrapDefinition.LifecyclePolicy.valueOf(rs.getString("lifecycle_policy")),
                    TrapDefinition.LifecycleState.valueOf(rs.getString("lifecycle_state")),
                    TrapDefinition.SaveAbility.valueOf(rs.getString("save_ability")),
                    rs.getInt("save_dc"),
                    TrapDefinition.RollMode.valueOf(rs.getString("roll_mode")),
                    rs.getString("damage_expression"),
                    TrapDefinition.DamagePolicy.valueOf(rs.getString("success_damage")),
                    TrapDefinition.MovementDecision.valueOf(rs.getString("success_movement")),
                    TrapDefinition.DamagePolicy.valueOf(rs.getString("failure_damage")),
                    TrapDefinition.MovementDecision.valueOf(rs.getString("failure_movement")),
                    rs.getLong("version"),
                    rs.getTimestamp("updated_at").toInstant().atOffset(ZoneOffset.UTC)
            );
        }
    }
}
