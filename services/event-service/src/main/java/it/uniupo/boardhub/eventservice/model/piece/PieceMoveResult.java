package it.uniupo.boardhub.eventservice.model.piece;

import java.util.List;
import java.util.UUID;

public record PieceMoveResult(
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

    public PieceMoveResult {
        path = List.copyOf(path);
        visibleTrapsOnPath = List.copyOf(visibleTrapsOnPath);
    }
}
