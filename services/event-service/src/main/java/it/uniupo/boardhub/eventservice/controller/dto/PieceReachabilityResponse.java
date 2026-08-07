package it.uniupo.boardhub.eventservice.controller.dto;

import java.util.List;
import java.util.UUID;

public record PieceReachabilityResponse(
        UUID sessionPieceId,
        String currentCell,
        int movementPoints,
        long version,
        List<ReachableCellResponse> reachableCells
) {
}
