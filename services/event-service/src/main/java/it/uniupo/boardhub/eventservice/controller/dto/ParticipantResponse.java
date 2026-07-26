package it.uniupo.boardhub.eventservice.controller.dto;

import java.util.UUID;

public record ParticipantResponse(
        UUID participantId,
        String sessionId,
        String role,
        String displayName,
        String status,
        String joinedAt
) {
}
