package it.uniupo.boardhub.eventservice.controller.dto;

import java.util.UUID;

public record CreateSessionPieceRequest(
        UUID characterId,
        String representationMode,
        String startCell
) {
}
