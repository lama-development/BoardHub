package it.uniupo.boardhub.eventservice.model.session;

import java.time.OffsetDateTime;

// Sessione D&D persistita con dimensioni base della plancia.
public record GameSession(
        String sessionId,
        String venueId,
        String tableId,
        String title,
        String gameType,
        String publicSummary,
        boolean acceptingJoinRequests,
        GameSessionStatus status,
        int gridWidth,
        int gridHeight,
        OffsetDateTime createdAt
) {
    // Mantiene compatibili i punti interni che non devono configurare l'ingresso pubblico.
    public GameSession(
            String sessionId,
            String venueId,
            String tableId,
            String title,
            String gameType,
            GameSessionStatus status,
            int gridWidth,
            int gridHeight,
            OffsetDateTime createdAt
    ) {
        this(
                sessionId, venueId, tableId, title, gameType, "", true,
                status, gridWidth, gridHeight, createdAt
        );
    }
}
