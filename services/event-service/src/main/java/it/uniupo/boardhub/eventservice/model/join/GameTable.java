package it.uniupo.boardhub.eventservice.model.join;

import java.time.OffsetDateTime;

// Tavolo fisico identificato dal QR pubblico riutilizzabile tra piu sessioni.
public record GameTable(
        String tableId,
        String tablePublicId,
        String displayName,
        GameTableStatus status,
        String activeSessionId,
        OffsetDateTime claimExpiresAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
