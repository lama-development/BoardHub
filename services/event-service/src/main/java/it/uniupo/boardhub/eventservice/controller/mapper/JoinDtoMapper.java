package it.uniupo.boardhub.eventservice.controller.mapper;

import it.uniupo.boardhub.eventservice.controller.dto.JoinRequestResponse;
import it.uniupo.boardhub.eventservice.controller.dto.ParticipantResponse;
import it.uniupo.boardhub.eventservice.model.join.SessionJoinRequest;
import it.uniupo.boardhub.eventservice.model.join.SessionParticipant;

public final class JoinDtoMapper {

    private JoinDtoMapper() {
    }

    public static JoinRequestResponse toResponse(SessionJoinRequest request) {
        return new JoinRequestResponse(
                request.requestId(), request.sessionId(), request.playerReference(),
                request.displayName(), request.status().name(), request.requestedAt().toString(),
                request.expiresAt().toString(),
                request.resolvedAt() == null ? null : request.resolvedAt().toString()
        );
    }

    public static ParticipantResponse toResponse(SessionParticipant participant) {
        return new ParticipantResponse(
                participant.participantId(), participant.sessionId(), participant.role().name(),
                participant.displayName(), participant.status().name(), participant.joinedAt().toString()
        );
    }
}
