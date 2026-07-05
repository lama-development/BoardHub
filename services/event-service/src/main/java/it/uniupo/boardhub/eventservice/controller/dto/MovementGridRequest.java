package it.uniupo.boardhub.eventservice.controller.dto;

import java.util.List;

public record MovementGridRequest(
        int width,
        int height,
        List<String> difficultCells,
        List<String> blockedCells,
        List<String> obstacleCells,
        List<String> occupiedCells,
        List<MovementWallRequest> walls,
        List<MovementTrapRequest> traps
) {
}
