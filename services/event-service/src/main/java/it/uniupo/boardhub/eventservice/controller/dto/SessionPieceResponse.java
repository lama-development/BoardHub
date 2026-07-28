package it.uniupo.boardhub.eventservice.controller.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SessionPieceResponse(
        UUID sessionPieceId,
        String sessionId,
        UUID characterId,
        UUID participantId,
        String representationMode,
        String currentCell,
        long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
