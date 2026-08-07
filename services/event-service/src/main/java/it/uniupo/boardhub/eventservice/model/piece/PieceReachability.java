package it.uniupo.boardhub.eventservice.model.piece;

import it.uniupo.boardhub.eventservice.model.grid.ReachableCell;

import java.util.List;
import java.util.UUID;

public record PieceReachability(
        UUID sessionPieceId,
        String currentCell,
        int movementPoints,
        long version,
        List<ReachableCell> reachableCells
) {

    public PieceReachability {
        reachableCells = List.copyOf(reachableCells);
    }
}
