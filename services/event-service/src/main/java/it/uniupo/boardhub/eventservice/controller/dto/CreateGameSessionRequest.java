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
    // Costruttore sintetico per i client interni che usano i valori pubblici predefiniti.
    public CreateGameSessionRequest(
            String sessionId,
            String venueId,
            String tableId,
            String title,
            String gameType,
            MovementGridRequest grid
    ) {
        this(sessionId, venueId, tableId, null, null, title, gameType, null, null, grid);
    }
}
