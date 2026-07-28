package it.uniupo.boardhub.eventservice.model.piece;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SessionPiece(
        UUID sessionPieceId,
        String sessionId,
        UUID characterId,
        UUID participantId,
        RepresentationMode representationMode,
        String currentCell,
        long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
