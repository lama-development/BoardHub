package it.uniupo.boardhub.eventservice.model.trap;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CharacterControl(
        String sessionId,
        UUID characterId,
        UUID dmParticipantId,
        long version,
        OffsetDateTime assumedAt
) {
}
