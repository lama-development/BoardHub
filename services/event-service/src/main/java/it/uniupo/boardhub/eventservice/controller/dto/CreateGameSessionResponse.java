package it.uniupo.boardhub.eventservice.controller.dto;

public record CreateGameSessionResponse(
        String sessionId,
        String venueId,
        String tableId,
        String tablePublicId,
        String tableDisplayName,
        String title,
        String gameType,
        String publicSummary,
        boolean acceptingJoinRequests,
        String status,
        int gridWidth,
        int gridHeight,
        String createdAt,
        String dmAccessToken
) {
}
