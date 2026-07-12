package it.uniupo.boardhub.eventservice.service;

import it.uniupo.boardhub.eventservice.model.grid.GameGrid;
import it.uniupo.boardhub.eventservice.model.grid.GridCell;
import it.uniupo.boardhub.eventservice.model.grid.GridDirection;
import it.uniupo.boardhub.eventservice.model.grid.GridPosition;
import it.uniupo.boardhub.eventservice.model.grid.GridTrap;
import it.uniupo.boardhub.eventservice.model.grid.MovementRequest;
import it.uniupo.boardhub.eventservice.model.grid.ReachableCell;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;

@Service
public class MovementService {

    // Calcola le celle raggiungibili applicando Dijkstra sulla griglia.
    public List<ReachableCell> calculateReachableCells(GameGrid grid, MovementRequest request) {
        Objects.requireNonNull(grid, "La griglia e obbligatoria.");
        Objects.requireNonNull(request, "La richiesta di movimento e obbligatoria.");

        if (!grid.contains(request.start())) {
            throw new IllegalArgumentException("La posizione iniziale non appartiene alla griglia.");
        }
        if (!grid.cellAt(request.start()).terrainType().isWalkable()) {
            throw new IllegalArgumentException("La posizione iniziale non e attraversabile.");
        }

        Map<GridPosition, Integer> costs = new HashMap<>();
        Map<GridPosition, GridPosition> previous = new HashMap<>();
        PriorityQueue<MovementNode> queue = new PriorityQueue<>(Comparator.comparingInt(MovementNode::cost));

        costs.put(request.start(), 0);
        queue.add(new MovementNode(request.start(), 0));

        while (!queue.isEmpty()) {
            MovementNode current = queue.poll();
            if (current.cost() > costs.getOrDefault(current.position(), Integer.MAX_VALUE)) {
                continue;
            }

            for (GridDirection direction : GridDirection.values()) {
                GridPosition next = adjacentPosition(current.position(), direction);
                if (next == null || !grid.contains(next) || !canMove(grid, current.position(), next, direction)) {
                    continue;
                }

                GridCell nextCell = grid.cellAt(next);
                if (!nextCell.isWalkable()) {
                    continue;
                }

                int nextCost = current.cost() + nextCell.movementCost();
                if (nextCost > request.movementPoints()) {
                    continue;
                }

                if (nextCost < costs.getOrDefault(next, Integer.MAX_VALUE)) {
                    costs.put(next, nextCost);
                    previous.put(next, current.position());
                    queue.add(new MovementNode(next, nextCost));
                }
            }
        }

        return costs.entrySet().stream()
                .sorted(Comparator
                        .comparingInt(Map.Entry<GridPosition, Integer>::getValue)
                        .thenComparing(entry -> entry.getKey().row())
                        .thenComparing(entry -> entry.getKey().column()))
                .map(entry -> toReachableCell(grid, request.start(), entry.getKey(), entry.getValue(), previous))
                .toList();
    }

    // Converte il risultato interno dell'algoritmo in una cella raggiungibile esposta dal dominio.
    private ReachableCell toReachableCell(
            GameGrid grid,
            GridPosition start,
            GridPosition target,
            int cost,
            Map<GridPosition, GridPosition> previous
    ) {
        List<GridPosition> path = buildPath(start, target, previous);
        List<GridTrap> traps = path.stream()
                .map(grid::trapAt)
                .filter(Objects::nonNull)
                .filter(GridTrap::armed)
                .toList();
        return new ReachableCell(target, cost, path, traps);
    }

    // Ricostruisce il percorso minimo partendo dalla mappa dei predecessori.
    private List<GridPosition> buildPath(
            GridPosition start,
            GridPosition target,
            Map<GridPosition, GridPosition> previous
    ) {
        List<GridPosition> reversedPath = new ArrayList<>();
        GridPosition current = target;
        while (current != null) {
            reversedPath.add(current);
            if (current.equals(start)) {
                break;
            }
            current = previous.get(current);
        }

        java.util.Collections.reverse(reversedPath);
        return List.copyOf(reversedPath);
    }

    // Restituisce la posizione vicina nella direzione richiesta, se resta in coordinate positive.
    private GridPosition adjacentPosition(GridPosition position, GridDirection direction) {
        int row = position.row() + direction.rowDelta();
        int column = position.column() + direction.columnDelta();
        if (row < 1 || column < 1) {
            return null;
        }
        return new GridPosition(row, column);
    }

    // Verifica muri e regola anti-taglio angolo per movimenti ortogonali e diagonali.
    private boolean canMove(GameGrid grid, GridPosition from, GridPosition to, GridDirection direction) {
        if (!direction.isDiagonal()) {
            return !grid.hasWallBetween(from, to);
        }

        GridPosition verticalStep = new GridPosition(from.row() + direction.rowDelta(), from.column());
        GridPosition horizontalStep = new GridPosition(from.row(), from.column() + direction.columnDelta());

        if (!grid.contains(verticalStep) || !grid.contains(horizontalStep)) {
            return false;
        }
        if (!grid.cellAt(verticalStep).isWalkable() || !grid.cellAt(horizontalStep).isWalkable()) {
            return false;
        }
        if (grid.hasWallBetween(from, verticalStep) || grid.hasWallBetween(from, horizontalStep)) {
            return false;
        }
        return !grid.hasWallBetween(verticalStep, to) && !grid.hasWallBetween(horizontalStep, to);
    }

    private record MovementNode(GridPosition position, int cost) {
    }
}
