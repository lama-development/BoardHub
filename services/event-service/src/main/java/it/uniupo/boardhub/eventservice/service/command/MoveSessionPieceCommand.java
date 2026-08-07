package it.uniupo.boardhub.eventservice.service.command;

import java.util.UUID;

public record MoveSessionPieceCommand(
        String destination,
        Long expectedVersion,
        UUID commandId
) {
}
