package it.uniupo.boardhub.eventservice.model.session;

import java.time.OffsetDateTime;

// Sessione D&D persistita con dimensioni base della plancia.
public record GameSession(
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
}
