package it.uniupo.boardhub.eventservice.service.command;

import java.util.UUID;

public record CreateSessionPieceCommand(
        UUID characterId,
        String representationMode,
        String startCell
) {
}
