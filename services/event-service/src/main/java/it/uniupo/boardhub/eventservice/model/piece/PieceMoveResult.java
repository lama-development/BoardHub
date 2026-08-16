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
        List<String> visibleTrapsOnPath,
        Status status,
        UUID resolutionId,
        String requestedDestination,
        Integer movementRemaining
) {

    public PieceMoveResult {
        path = List.copyOf(path);
        visibleTrapsOnPath = List.copyOf(visibleTrapsOnPath);
    }

    public PieceMoveResult(
            UUID commandId, String eventId, UUID sessionPieceId, UUID characterId,
            String from, String to, List<String> path, int cost, long version,
            List<String> visibleTrapsOnPath
    ) {
        this(
                commandId, eventId, sessionPieceId, characterId, from, to, path, cost,
                version, visibleTrapsOnPath, Status.CONFIRMED, null, to, null
        );
    }

    public enum Status {
        CONFIRMED,
        TRAP_PENDING
    }
}
