package it.uniupo.boardhub.eventservice.controller.dto;

public record ReachableCellsRequest(
        String characterId,
        String start,
        int movementPoints,
        MovementGridRequest grid
) {
}
