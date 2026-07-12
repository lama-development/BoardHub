package it.uniupo.boardhub.eventservice.service;

import it.uniupo.boardhub.eventservice.model.grid.GameGrid;
import it.uniupo.boardhub.eventservice.model.grid.MovementRequest;
import it.uniupo.boardhub.eventservice.model.grid.ReachableCell;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SessionMovementService {

    private final SessionGridService sessionGridService;
    private final MovementService movementService;

    public SessionMovementService(SessionGridService sessionGridService, MovementService movementService) {
        this.sessionGridService = sessionGridService;
        this.movementService = movementService;
    }

    // Carica la griglia salvata e applica l'algoritmo di movimento.
    public List<ReachableCell> calculateReachableCells(String sessionId, MovementRequest request) {
        GameGrid grid = sessionGridService.loadGrid(sessionId);
        return movementService.calculateReachableCells(grid, request);
    }
}
