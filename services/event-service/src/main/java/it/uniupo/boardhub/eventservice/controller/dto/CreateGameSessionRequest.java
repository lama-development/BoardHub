package it.uniupo.boardhub.eventservice.controller.dto;

public record CreateGameSessionRequest(
        String sessionId,
        String venueId,
        String tableId,
        String title,
        String gameType,
        MovementGridRequest grid
) {
}
