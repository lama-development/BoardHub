package it.uniupo.boardhub.eventservice.controller.dto;

public record SessionReachableCellsRequest(
        String characterId,
        String start,
        int movementPoints
) {
}
