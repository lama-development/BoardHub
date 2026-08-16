package it.uniupo.boardhub.eventservice.controller.dto;

import java.util.UUID;

public record CharacterControlResponse(
        String sessionId,
        UUID characterId,
        UUID dmParticipantId,
        long version,
        String assumedAt
) {
}
