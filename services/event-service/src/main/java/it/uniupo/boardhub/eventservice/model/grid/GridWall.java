package it.uniupo.boardhub.eventservice.model.grid;

import java.util.Objects;

public record GridWall(GridPosition position, GridDirection direction) {

    public GridWall {
        Objects.requireNonNull(position, "La posizione del muro e obbligatoria.");
        Objects.requireNonNull(direction, "La direzione del muro e obbligatoria.");
    }
}
