package it.uniupo.boardhub.eventservice.service;

import it.uniupo.boardhub.eventservice.controller.dto.MovementGridRequest;
import it.uniupo.boardhub.eventservice.controller.dto.MovementTrapRequest;
import it.uniupo.boardhub.eventservice.controller.dto.MovementWallRequest;
import it.uniupo.boardhub.eventservice.model.grid.GameGrid;
import it.uniupo.boardhub.eventservice.model.grid.GridCell;
import it.uniupo.boardhub.eventservice.model.grid.GridDirection;
import it.uniupo.boardhub.eventservice.model.grid.GridPosition;
import it.uniupo.boardhub.eventservice.model.grid.GridTrap;
import it.uniupo.boardhub.eventservice.model.grid.GridWall;
import it.uniupo.boardhub.eventservice.model.grid.TerrainType;
import it.uniupo.boardhub.eventservice.model.grid.TrapVisibility;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class MovementGridFactory {

    public static final int MAX_GRID_CELLS = 2_500;

    // Costruisce una griglia validata con una sola copia immutabile finale.
    public GameGrid create(MovementGridRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La griglia e obbligatoria.");
        }

        if (request.width() < 1 || request.height() < 1) {
            throw new IllegalArgumentException("Le dimensioni della griglia devono essere maggiori di zero.");
        }

        long cellCount = (long) request.width() * request.height();
        if (cellCount > MAX_GRID_CELLS) {
            throw new IllegalArgumentException(
                    "La griglia non puo superare " + MAX_GRID_CELLS + " celle."
            );
        }

        validateConfigurationSize(request, cellCount);

        Map<GridPosition, GridCell> cells = new HashMap<>();
        Set<GridWall> walls = new HashSet<>();
        Map<GridPosition, GridTrap> traps = new HashMap<>();

        addTerrainCells(cells, request.difficultCells(), TerrainType.DIFFICULT, request.width(), request.height());
        addTerrainCells(cells, request.blockedCells(), TerrainType.BLOCKED, request.width(), request.height());
        addTerrainCells(cells, request.obstacleCells(), TerrainType.OBSTACLE, request.width(), request.height());
        addOccupiedCells(cells, request.occupiedCells(), request.width(), request.height());
        addWalls(walls, request.walls(), request.width(), request.height());
        addTraps(traps, request.traps(), request.width(), request.height());

        return new GameGrid(request.width(), request.height(), cells, walls, traps);
    }

    // Limita le collezioni ricevute per evitare richieste sproporzionate rispetto alla griglia.
    private void validateConfigurationSize(MovementGridRequest request, long cellCount) {
        ensureMaxSize("difficultCells", request.difficultCells(), cellCount);
        ensureMaxSize("blockedCells", request.blockedCells(), cellCount);
        ensureMaxSize("obstacleCells", request.obstacleCells(), cellCount);
        ensureMaxSize("occupiedCells", request.occupiedCells(), cellCount);
        ensureMaxSize("traps", request.traps(), cellCount);
        ensureMaxSize("walls", request.walls(), cellCount * 4);
    }

    private void ensureMaxSize(String field, List<?> values, long maximum) {
        if (values != null && values.size() > maximum) {
            throw new IllegalArgumentException(
                    field + " contiene piu elementi di quelli compatibili con la griglia."
            );
        }
    }

    private void addTerrainCells(
            Map<GridPosition, GridCell> cells,
            List<String> positions,
            TerrainType terrainType,
            int width,
            int height
    ) {
        for (String cell : safeList(positions)) {
            GridPosition position = parsePosition(cell);
            ensureInside(position, width, height);
            GridCell existing = cells.get(position);
            if (existing != null && existing.terrainType() != terrainType) {
                throw new IllegalArgumentException(
                        "La cella " + position.toCell() + " appartiene a piu categorie di terreno."
                );
            }
            boolean occupied = existing != null && existing.occupied();
            cells.put(position, new GridCell(position, terrainType, occupied));
        }
    }

    private void addOccupiedCells(Map<GridPosition, GridCell> cells, List<String> positions, int width, int height) {
        for (String cell : safeList(positions)) {
            GridPosition position = parsePosition(cell);
            ensureInside(position, width, height);
            GridCell existing = cells.get(position);
            TerrainType terrainType = existing == null ? TerrainType.NORMAL : existing.terrainType();
            if (!terrainType.isWalkable()) {
                throw new IllegalArgumentException(
                        "La cella occupata " + position.toCell() + " non e attraversabile."
                );
            }
            cells.put(position, new GridCell(position, terrainType, true));
        }
    }

    private void addWalls(Set<GridWall> walls, List<MovementWallRequest> requests, int width, int height) {
        for (MovementWallRequest wall : safeList(requests)) {
            if (wall == null || wall.direction() == null) {
                throw new IllegalArgumentException("Ogni muro deve avere una direzione.");
            }
            GridDirection direction = parseDirection(wall.direction());
            if (direction.isDiagonal()) {
                throw new IllegalArgumentException("Un muro puo trovarsi solo su un bordo ortogonale.");
            }

            GridPosition position = parsePosition(wall.cell());
            ensureInside(position, width, height);
            GridPosition adjacent = adjacentPosition(position, direction);
            ensureInside(adjacent, width, height);
            walls.add(new GridWall(position, direction));
            walls.add(new GridWall(adjacent, direction.opposite()));
        }
    }

    private void addTraps(
            Map<GridPosition, GridTrap> traps,
            List<MovementTrapRequest> requests,
            int width,
            int height
    ) {
        Set<String> trapIds = new HashSet<>();
        for (MovementTrapRequest trap : safeList(requests)) {
            if (trap == null || trap.trapId() == null || trap.trapId().isBlank()) {
                throw new IllegalArgumentException("Ogni trappola deve avere un trapId.");
            }

            GridPosition position = parsePosition(trap.cell());
            ensureInside(position, width, height);
            if (!trapIds.add(trap.trapId())) {
                throw new IllegalArgumentException("trapId duplicato: " + trap.trapId());
            }
            if (traps.containsKey(position)) {
                throw new IllegalArgumentException("Sono presenti piu trappole sulla cella " + position.toCell() + ".");
            }

            traps.put(position, new GridTrap(
                    trap.trapId(),
                    position,
                    parseVisibility(trap.visibility()),
                    trap.armed()
            ));
        }
    }

    private GridPosition adjacentPosition(GridPosition position, GridDirection direction) {
        return new GridPosition(
                position.row() + direction.rowDelta(),
                position.column() + direction.columnDelta()
        );
    }

    private void ensureInside(GridPosition position, int width, int height) {
        if (position.row() > height || position.column() > width) {
            throw new IllegalArgumentException("La posizione non appartiene alla griglia: " + position.toCell());
        }
    }

    private GridPosition parsePosition(String cell) {
        return GridPosition.fromCell(cell);
    }

    private GridDirection parseDirection(String direction) {
        try {
            return GridDirection.valueOf(direction.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Direzione del muro non valida: " + direction, ex);
        }
    }

    private TrapVisibility parseVisibility(String visibility) {
        if (visibility == null || visibility.isBlank()) {
            throw new IllegalArgumentException("La visibilita della trappola e obbligatoria.");
        }
        try {
            return TrapVisibility.valueOf(visibility.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Visibilita della trappola non valida: " + visibility, ex);
        }
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
