package it.uniupo.boardhub.eventservice.model.trap;

import it.uniupo.boardhub.eventservice.model.grid.TrapVisibility;

import java.time.OffsetDateTime;

public record TrapDefinition(
        String sessionId,
        String trapId,
        String cell,
        TrapVisibility visibility,
        LifecyclePolicy lifecyclePolicy,
        LifecycleState lifecycleState,
        SaveAbility saveAbility,
        int saveDc,
        RollMode rollMode,
        String damageExpression,
        DamagePolicy successDamage,
        MovementDecision successMovement,
        DamagePolicy failureDamage,
        MovementDecision failureMovement,
        long version,
        OffsetDateTime updatedAt
) {
    public boolean triggersOnEntry() {
        return lifecycleState == LifecycleState.ARMED
                || lifecycleState == LifecycleState.TRIGGERED_ACTIVE;
    }

    public enum LifecyclePolicy { ONE_SHOT, PERSISTENT }
    public enum LifecycleState { ARMED, TRIGGERED_ACTIVE, SPENT, DISARMED }
    public enum SaveAbility { STRENGTH, DEXTERITY, CONSTITUTION, INTELLIGENCE, WISDOM, CHARISMA }
    public enum RollMode { NORMAL, ADVANTAGE, DISADVANTAGE }
    public enum DamagePolicy { NONE, HALF, FULL }
    public enum MovementDecision { CONTINUE, STOP }
}
