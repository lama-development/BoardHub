package it.uniupo.boardhub.eventservice.model.grid;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public record GameGrid(
        int width,
        int height,
        Map<GridPosition, GridCell> cells,
        Set<GridWall> walls,
        Map<GridPosition, GridTrap> traps
) {

    public GameGrid {
        if (width < 1) {
            throw new IllegalArgumentException("La larghezza della griglia deve essere maggiore di zero.");
        }
        if (height < 1) {
            throw new IllegalArgumentException("L'altezza della griglia deve essere maggiore di zero.");
        }
        Objects.requireNonNull(cells, "Le celle della griglia sono obbligatorie.");
        Objects.requireNonNull(walls, "I muri della griglia sono obbligatori.");
        Objects.requireNonNull(traps, "Le trappole della griglia sono obbligatorie.");
        cells = Map.copyOf(cells);
        walls = Set.copyOf(walls);
        traps = Map.copyOf(traps);
    }

    public static GameGrid empty(int width, int height) {
        return new GameGrid(width, height, Map.of(), Set.of(), Map.of());
    }

    public boolean contains(GridPosition position) {
        return position.row() <= height && position.column() <= width;
    }

    public GridCell cellAt(GridPosition position) {
        if (!contains(position)) {
            throw new IllegalArgumentException("La posizione non appartiene alla griglia: " + position.toCell());
        }
        return cells.getOrDefault(position, new GridCell(position, TerrainType.NORMAL, false));
    }

    public GameGrid withCell(GridCell cell) {
        if (!contains(cell.position())) {
            throw new IllegalArgumentException("La cella non appartiene alla griglia: " + cell.position().toCell());
        }

        Map<GridPosition, GridCell> updatedCells = new HashMap<>(cells);
        updatedCells.put(cell.position(), cell);
        return new GameGrid(width, height, updatedCells, walls, traps);
    }

    public GameGrid withWall(GridWall wall) {
        if (!contains(wall.position())) {
            throw new IllegalArgumentException("Il muro parte da una cella fuori griglia: " + wall.position().toCell());
        }

        GridPosition adjacent = wall.position().move(wall.direction());
        if (!contains(adjacent)) {
            throw new IllegalArgumentException("Il muro punta fuori dalla griglia: " + adjacent.toCell());
        }

        Set<GridWall> updatedWalls = new HashSet<>(walls);
        updatedWalls.add(wall);
        updatedWalls.add(new GridWall(adjacent, wall.direction().opposite()));
        return new GameGrid(width, height, cells, updatedWalls, traps);
    }

    public GameGrid withTrap(GridTrap trap) {
        if (!contains(trap.position())) {
            throw new IllegalArgumentException("La trappola non appartiene alla griglia: " + trap.position().toCell());
        }

        Map<GridPosition, GridTrap> updatedTraps = new HashMap<>(traps);
        updatedTraps.put(trap.position(), trap);
        return new GameGrid(width, height, cells, walls, updatedTraps);
    }

    public GridTrap trapAt(GridPosition position) {
        if (!contains(position)) {
            throw new IllegalArgumentException("La posizione non appartiene alla griglia: " + position.toCell());
        }
        return traps.get(position);
    }

    public boolean hasWall(GridPosition position, GridDirection direction) {
        return walls.contains(new GridWall(position, direction));
    }

    public boolean hasWallBetween(GridPosition from, GridPosition to) {
        if (!contains(from) || !contains(to)) {
            throw new IllegalArgumentException("Le posizioni devono appartenere alla griglia.");
        }

        int rowDelta = to.row() - from.row();
        int columnDelta = to.column() - from.column();
        for (GridDirection direction : GridDirection.values()) {
            if (direction.rowDelta() == rowDelta && direction.columnDelta() == columnDelta) {
                return hasWall(from, direction);
            }
        }
        throw new IllegalArgumentException("Le celle non sono adiacenti: " + from.toCell() + " -> " + to.toCell());
    }
}
