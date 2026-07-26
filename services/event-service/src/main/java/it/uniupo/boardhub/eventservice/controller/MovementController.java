package it.uniupo.boardhub.eventservice.controller;

import it.uniupo.boardhub.eventservice.controller.dto.ReachableCellResponse;
import it.uniupo.boardhub.eventservice.controller.dto.ReachableCellsRequest;
import it.uniupo.boardhub.eventservice.controller.dto.ReachableCellsResponse;
import it.uniupo.boardhub.eventservice.controller.mapper.ApiRequestMapper;
import it.uniupo.boardhub.eventservice.model.grid.GameGrid;
import it.uniupo.boardhub.eventservice.model.grid.GridPosition;
import it.uniupo.boardhub.eventservice.model.grid.GridTrap;
import it.uniupo.boardhub.eventservice.model.grid.MovementRequest;
import it.uniupo.boardhub.eventservice.model.grid.ReachableCell;
import it.uniupo.boardhub.eventservice.service.MovementGridFactory;
import it.uniupo.boardhub.eventservice.service.MovementService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/movement")
public class MovementController {

    private final MovementService movementService;
    private final MovementGridFactory gridFactory;

    public MovementController(MovementService movementService, MovementGridFactory gridFactory) {
        this.movementService = movementService;
        this.gridFactory = gridFactory;
    }

    // Espone il calcolo delle celle raggiungibili a dashboard, app o simulatore.
    @PostMapping("/reachable-cells")
    public ReachableCellsResponse calculateReachableCells(@RequestBody ReachableCellsRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La richiesta di movimento e obbligatoria.");
        }

        GameGrid grid = gridFactory.create(ApiRequestMapper.toGridConfiguration(request.grid()));
        MovementRequest movementRequest = new MovementRequest(
                request.characterId(),
                GridPosition.fromCell(request.start()),
                request.movementPoints()
        );

        List<ReachableCellResponse> reachableCells = movementService
                .calculateReachableCells(grid, movementRequest)
                .stream()
                .map(this::toResponse)
                .toList();

        return new ReachableCellsResponse(request.characterId(), reachableCells);
    }

    // Nasconde al client le trappole non ancora rivelate ai giocatori.
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
