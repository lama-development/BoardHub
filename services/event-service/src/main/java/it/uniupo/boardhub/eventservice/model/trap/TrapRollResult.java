package it.uniupo.boardhub.eventservice.model.trap;

import java.util.List;
import java.util.UUID;

public record TrapRollResult(
        UUID resolutionId,
        TrapResolution.Status status,
        int d20First,
        Integer d20Second,
        int selectedD20,
        int saveBonus,
        int saveTotal,
        boolean success,
        List<Integer> damageRolls,
        int damageTotal,
        int hpCurrent,
        CharacterTacticalOutcome tacticalStatus,
        TrapDefinition.MovementDecision movementDecision,
        int movementRemaining,
        long version
) {
    public TrapRollResult {
        damageRolls = List.copyOf(damageRolls);
    }

    public enum CharacterTacticalOutcome { ACTIVE, DOWNED }
}
