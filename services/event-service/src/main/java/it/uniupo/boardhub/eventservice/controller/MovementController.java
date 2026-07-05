package it.uniupo.boardhub.eventservice.controller;

import it.uniupo.boardhub.eventservice.controller.dto.MovementGridRequest;
import it.uniupo.boardhub.eventservice.controller.dto.MovementTrapRequest;
import it.uniupo.boardhub.eventservice.controller.dto.MovementWallRequest;
import it.uniupo.boardhub.eventservice.controller.dto.ReachableCellResponse;
import it.uniupo.boardhub.eventservice.controller.dto.ReachableCellsRequest;
import it.uniupo.boardhub.eventservice.controller.dto.ReachableCellsResponse;
import it.uniupo.boardhub.eventservice.model.grid.GameGrid;
import it.uniupo.boardhub.eventservice.model.grid.GridCell;
import it.uniupo.boardhub.eventservice.model.grid.GridDirection;
import it.uniupo.boardhub.eventservice.model.grid.GridPosition;
import it.uniupo.boardhub.eventservice.model.grid.GridTrap;
import it.uniupo.boardhub.eventservice.model.grid.GridWall;
import it.uniupo.boardhub.eventservice.model.grid.MovementRequest;
import it.uniupo.boardhub.eventservice.model.grid.ReachableCell;
import it.uniupo.boardhub.eventservice.model.grid.TerrainType;
import it.uniupo.boardhub.eventservice.model.grid.TrapVisibility;
import it.uniupo.boardhub.eventservice.service.MovementService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/movement")
public class MovementController {

    private final MovementService movementService;

    public MovementController(MovementService movementService) {
        this.movementService = movementService;
    }

    // Espone il calcolo delle celle raggiungibili a dashboard, app o simulatore.
    @PostMapping("/reachable-cells")
    public ReachableCellsResponse calculateReachableCells(@RequestBody ReachableCellsRequest request) {
        GameGrid grid = toGrid(request.grid());
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

    // Converte il payload REST in modello interno della griglia.
    private GameGrid toGrid(MovementGridRequest request) {
        Objects.requireNonNull(request, "La griglia e obbligatoria.");

        GameGrid grid = GameGrid.empty(request.width(), request.height());
        grid = addTerrainCells(grid, request.difficultCells(), TerrainType.DIFFICULT);
        grid = addTerrainCells(grid, request.blockedCells(), TerrainType.BLOCKED);
        grid = addTerrainCells(grid, request.obstacleCells(), TerrainType.OBSTACLE);
        grid = addOccupiedCells(grid, request.occupiedCells());
        grid = addWalls(grid, request.walls());
        grid = addTraps(grid, request.traps());
        return grid;
    }

    // Applica un tipo di terreno a una lista di celle.
    private GameGrid addTerrainCells(GameGrid grid, List<String> cells, TerrainType terrainType) {
        GameGrid updatedGrid = grid;
        for (String cell : safeList(cells)) {
            updatedGrid = updatedGrid.withCell(new GridCell(GridPosition.fromCell(cell), terrainType, false));
        }
        return updatedGrid;
    }

    // Marca come occupate le celle indicate, mantenendo il terreno gia configurato.
    private GameGrid addOccupiedCells(GameGrid grid, List<String> cells) {
        GameGrid updatedGrid = grid;
        for (String cell : safeList(cells)) {
            GridPosition position = GridPosition.fromCell(cell);
            GridCell existingCell = updatedGrid.cellAt(position);
            updatedGrid = updatedGrid.withCell(new GridCell(position, existingCell.terrainType(), true));
        }
        return updatedGrid;
    }

    // Aggiunge muri sui bordi tra celle adiacenti.
    private GameGrid addWalls(GameGrid grid, List<MovementWallRequest> walls) {
        GameGrid updatedGrid = grid;
        for (MovementWallRequest wall : safeList(walls)) {
            GridDirection direction = GridDirection.valueOf(wall.direction().toUpperCase(Locale.ROOT));
            updatedGrid = updatedGrid.withWall(new GridWall(GridPosition.fromCell(wall.cell()), direction));
        }
        return updatedGrid;
    }

    // Aggiunge trappole visibili o nascoste alla griglia.
    private GameGrid addTraps(GameGrid grid, List<MovementTrapRequest> traps) {
        GameGrid updatedGrid = grid;
        for (MovementTrapRequest trap : safeList(traps)) {
            TrapVisibility visibility = TrapVisibility.valueOf(trap.visibility().toUpperCase(Locale.ROOT));
            updatedGrid = updatedGrid.withTrap(new GridTrap(
                    trap.trapId(),
                    GridPosition.fromCell(trap.cell()),
                    visibility,
                    trap.armed()
            ));
        }
        return updatedGrid;
    }

    // Converte una cella raggiungibile interna nel formato JSON di risposta.
    private ReachableCellResponse toResponse(ReachableCell reachableCell) {
        return new ReachableCellResponse(
                reachableCell.position().toCell(),
                reachableCell.cost(),
                reachableCell.path().stream().map(GridPosition::toCell).toList(),
                reachableCell.trapsOnPath().stream().map(GridTrap::trapId).toList()
        );
    }

    // Evita controlli null ripetuti sulle liste opzionali della richiesta.
    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
