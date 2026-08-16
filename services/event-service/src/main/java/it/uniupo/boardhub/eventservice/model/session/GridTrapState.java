package it.uniupo.boardhub.eventservice.model.session;

import it.uniupo.boardhub.eventservice.model.grid.TrapVisibility;
import it.uniupo.boardhub.eventservice.model.trap.TrapDefinition;

import java.time.OffsetDateTime;

// Stato persistito di una trappola associata alla griglia della sessione.
public record GridTrapState(
        String sessionId,
        String trapId,
        String cell,
        TrapVisibility visibility,
        boolean armed,
        TrapDefinition.LifecyclePolicy lifecyclePolicy,
        TrapDefinition.LifecycleState lifecycleState,
        TrapDefinition.SaveAbility saveAbility,
        int saveDc,
        TrapDefinition.RollMode rollMode,
        String damageExpression,
        TrapDefinition.DamagePolicy successDamage,
        TrapDefinition.MovementDecision successMovement,
        TrapDefinition.DamagePolicy failureDamage,
        TrapDefinition.MovementDecision failureMovement,
        long version,
        OffsetDateTime updatedAt
) {
    public GridTrapState(String sessionId, String trapId, String cell, TrapVisibility visibility, boolean armed) {
        this(
                sessionId, trapId, cell, visibility, armed,
                TrapDefinition.LifecyclePolicy.ONE_SHOT,
                armed ? TrapDefinition.LifecycleState.ARMED : TrapDefinition.LifecycleState.DISARMED,
                TrapDefinition.SaveAbility.DEXTERITY,
                10,
                TrapDefinition.RollMode.NORMAL,
                "1d6",
                TrapDefinition.DamagePolicy.NONE,
                TrapDefinition.MovementDecision.CONTINUE,
                TrapDefinition.DamagePolicy.FULL,
                TrapDefinition.MovementDecision.STOP,
                0,
                null
        );
    }

    public TrapDefinition toDefinition() {
        return new TrapDefinition(
                sessionId, trapId, cell, visibility.normalized(), lifecyclePolicy, lifecycleState,
                saveAbility, saveDc, rollMode, damageExpression, successDamage, successMovement,
                failureDamage, failureMovement, version, updatedAt
        );
    }
}
