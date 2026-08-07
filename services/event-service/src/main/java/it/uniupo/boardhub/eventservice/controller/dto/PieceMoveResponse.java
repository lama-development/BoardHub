package it.uniupo.boardhub.eventservice.controller.dto;

import java.util.List;
import java.util.UUID;

public record PieceMoveResponse(
        String status,
        UUID commandId,
        String eventId,
        UUID sessionPieceId,
        UUID characterId,
        String from,
        String to,
        List<String> path,
        int cost,
        long version,
        List<String> visibleTrapsOnPath
) {
}
