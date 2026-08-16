package it.uniupo.boardhub.eventservice.controller.dto;

import java.util.List;
import java.util.UUID;

public record TrapRollResponse(
        UUID resolutionId,
        String status,
        int d20First,
        Integer d20Second,
        int selectedD20,
        int saveBonus,
        int saveTotal,
        boolean success,
        List<Integer> damageRolls,
        int damageTotal,
        int hpCurrent,
        String tacticalStatus,
        String movementDecision,
        int movementRemaining,
        long version
) {
}
