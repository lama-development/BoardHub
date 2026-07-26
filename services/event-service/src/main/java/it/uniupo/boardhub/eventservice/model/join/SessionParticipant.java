package it.uniupo.boardhub.eventservice.model.join;

import java.time.OffsetDateTime;
import java.util.UUID;

// Identita autorizzata di una persona nella singola sessione.
public record SessionParticipant(
        UUID participantId,
        UUID joinRequestId,
        String sessionId,
        String playerReference,
        ParticipantRole role,
        String displayName,
        ParticipantStatus status,
        OffsetDateTime joinedAt,
        OffsetDateTime leftAt
) {
}
