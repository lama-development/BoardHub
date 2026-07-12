package it.uniupo.boardhub.eventservice.controller.dto;

public record CreateGameSessionResponse(
        String sessionId,
        String venueId,
        String tableId,
        String title,
        String gameType,
        String status,
        int gridWidth,
        int gridHeight,
        String createdAt
) {
}
