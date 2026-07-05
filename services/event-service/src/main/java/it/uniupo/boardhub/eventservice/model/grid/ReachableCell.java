package it.uniupo.boardhub.eventservice.model.grid;

import java.util.List;
import java.util.Objects;

public record ReachableCell(GridPosition position, int cost, List<GridPosition> path, List<GridTrap> trapsOnPath) {

    public ReachableCell {
        Objects.requireNonNull(position, "La posizione raggiungibile e obbligatoria.");
        Objects.requireNonNull(path, "Il percorso verso la cella e obbligatorio.");
        Objects.requireNonNull(trapsOnPath, "Le trappole sul percorso sono obbligatorie.");
        if (cost < 0) {
            throw new IllegalArgumentException("Il costo di movimento non puo essere negativo.");
        }
        path = List.copyOf(path);
        trapsOnPath = List.copyOf(trapsOnPath);
    }

    public ReachableCell(GridPosition position, int cost) {
        this(position, cost, List.of(position), List.of());
    }
}
