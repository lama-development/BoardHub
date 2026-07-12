package it.uniupo.boardhub.eventservice.controller;

import it.uniupo.boardhub.eventservice.controller.dto.ReachableCellResponse;
import it.uniupo.boardhub.eventservice.controller.dto.ReachableCellsResponse;
import it.uniupo.boardhub.eventservice.controller.dto.SessionReachableCellsRequest;
import it.uniupo.boardhub.eventservice.model.grid.GridPosition;
import it.uniupo.boardhub.eventservice.model.grid.GridTrap;
import it.uniupo.boardhub.eventservice.model.grid.MovementRequest;
import it.uniupo.boardhub.eventservice.model.grid.ReachableCell;
import it.uniupo.boardhub.eventservice.service.SessionMovementService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/movement")
public class SessionMovementController {

    private final SessionMovementService sessionMovementService;

    public SessionMovementController(SessionMovementService sessionMovementService) {
        this.sessionMovementService = sessionMovementService;
    }

    // Calcola le celle raggiungibili usando la griglia salvata della sessione.
    @PostMapping("/reachable-cells")
    public ReachableCellsResponse calculateReachableCells(
            @PathVariable String sessionId,
            @RequestBody SessionReachableCellsRequest request
    ) {
        MovementRequest movementRequest = new MovementRequest(
                request.characterId(),
                GridPosition.fromCell(request.start()),
                request.movementPoints()
        );

        List<ReachableCellResponse> reachableCells = sessionMovementService
                .calculateReachableCells(sessionId, movementRequest)
                .stream()
                .map(this::toResponse)
                .toList();

        return new ReachableCellsResponse(request.characterId(), reachableCells);
    }

    // Converte una cella raggiungibile interna nel formato JSON di risposta.
    private ReachableCellResponse toResponse(ReachableCell reachableCell) {
        return new ReachableCellResponse(
                reachableCell.position().toCell(),
                reachableCell.cost(),
                reachableCell.path().stream().map(GridPosition::toCell).toList(),
                reachableCell.trapsOnPath().stream()
                        .filter(GridTrap::isVisibleToPlayers)
                        .map(GridTrap::trapId)
                        .toList()
        );
    }
}
