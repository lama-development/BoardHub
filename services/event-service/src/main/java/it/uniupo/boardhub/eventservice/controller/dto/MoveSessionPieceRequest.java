package it.uniupo.boardhub.eventservice.controller.dto;

import java.util.UUID;

public record MoveSessionPieceRequest(
        String destination,
        Long expectedVersion,
        UUID commandId
) {
}
