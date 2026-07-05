package it.uniupo.boardhub.eventservice.controller.dto;

import java.util.List;

public record ReachableCellsResponse(
        String characterId,
        List<ReachableCellResponse> reachableCells
) {
}
