package it.uniupo.boardhub.eventservice.controller.dto;

import java.util.UUID;

public record JoinRequestResponse(
        UUID requestId,
        String sessionId,
        String playerReference,
        String displayName,
        String status,
        String requestedAt,
        String expiresAt,
        String resolvedAt
) {
}
