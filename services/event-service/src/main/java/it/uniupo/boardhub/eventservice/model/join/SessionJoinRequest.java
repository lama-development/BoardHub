package it.uniupo.boardhub.eventservice.model.join;

import java.time.OffsetDateTime;
import java.util.UUID;

// Richiesta idempotente inviata dal giocatore e risolta dal Dungeon Master.
public record SessionJoinRequest(
        UUID requestId,
        UUID idempotencyKey,
        String requestFingerprint,
        String sessionId,
        String playerReference,
        String displayName,
        JoinRequestStatus status,
        OffsetDateTime requestedAt,
        OffsetDateTime expiresAt,
        OffsetDateTime resolvedAt
) {
}
