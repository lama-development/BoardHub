package it.uniupo.boardhub.eventservice.controller.dto;

public record CreateGameSessionRequest(
        String sessionId,
        String venueId,
        String tableId,
        String tablePublicId,
        String tableDisplayName,
        String title,
        String gameType,
        String publicSummary,
        Boolean acceptingJoinRequests,
        MovementGridRequest grid
) {
}
