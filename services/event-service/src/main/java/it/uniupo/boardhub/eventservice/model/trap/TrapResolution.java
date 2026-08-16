package it.uniupo.boardhub.eventservice.model.trap;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TrapResolution(
        UUID resolutionId,
        String sessionId,
        String trapId,
        UUID sessionPieceId,
        UUID characterId,
        UUID participantId,
        UUID moveCommandId,
        long expectedPieceVersion,
        Status status,
        String fromCell,
        String requestedDestination,
        String triggerCell,
        List<String> path,
        List<String> remainingPath,
        int movementBudget,
        int costToTrigger,
        int movementRemaining,
        String observedPhysicalCell,
        Integer saveD20First,
        Integer saveD20Second,
        Integer saveSelected,
        Integer saveBonus,
        Integer saveTotal,
        Boolean saveSuccess,
        List<Integer> damageRolls,
        Integer damageTotal,
        TrapDefinition.MovementDecision movementDecision,
        UUID rollCommandId,
        String rollFingerprint,
        UUID continueCommandId,
        long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime completedAt
) {
    public TrapResolution {
        path = List.copyOf(path);
        remainingPath = List.copyOf(remainingPath);
        damageRolls = damageRolls == null ? List.of() : List.copyOf(damageRolls);
    }

    public boolean pending() {
        return status != Status.COMPLETED && status != Status.CANCELLED_SESSION_ENDED;
    }

    public enum Status {
        AWAITING_SAVE_ROLL,
        CONTINUATION_ALLOWED,
        CORRECTION_REQUIRED,
        COMPLETED,
        CANCELLED_SESSION_ENDED
    }
}
