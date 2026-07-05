package it.uniupo.boardhub.eventservice.model.grid;

import java.util.Objects;

public record GridTrap(String trapId, GridPosition position, TrapVisibility visibility, boolean armed) {

    public GridTrap {
        if (trapId == null || trapId.isBlank()) {
            throw new IllegalArgumentException("L'identificativo della trappola e obbligatorio.");
        }
        Objects.requireNonNull(position, "La posizione della trappola e obbligatoria.");
        Objects.requireNonNull(visibility, "La visibilita della trappola e obbligatoria.");
    }

    public boolean isVisibleToPlayers() {
        return visibility == TrapVisibility.REVEALED;
    }

    public boolean canBeRevealed() {
        return visibility != TrapVisibility.ALWAYS_HIDDEN;
    }
}
