package it.uniupo.boardhub.eventservice.model.grid;

import java.util.Objects;

public record GridCell(GridPosition position, TerrainType terrainType, boolean occupied) {

    public GridCell {
        Objects.requireNonNull(position, "La posizione della cella e obbligatoria.");
        Objects.requireNonNull(terrainType, "Il tipo di terreno e obbligatorio.");
    }

    public boolean isWalkable() {
        return terrainType.isWalkable() && !occupied;
    }

    public int movementCost() {
        return terrainType.movementCost();
    }
}
