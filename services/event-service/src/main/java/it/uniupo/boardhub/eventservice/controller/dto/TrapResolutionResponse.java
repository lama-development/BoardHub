package it.uniupo.boardhub.eventservice.controller.dto;

import java.util.UUID;

public record TrapResolutionResponse(
        UUID resolutionId,
        String status,
        UUID sessionPieceId,
        UUID characterId,
        String requestedDestination,
        String triggerCell,
        int movementRemaining,
        Long version
) {
}
