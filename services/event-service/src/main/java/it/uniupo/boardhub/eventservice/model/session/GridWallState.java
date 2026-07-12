package it.uniupo.boardhub.eventservice.model.session;

import it.uniupo.boardhub.eventservice.model.grid.GridDirection;

// Stato persistito di un muro posto sul bordo di una cella.
public record GridWallState(
        String sessionId,
        String cell,
        GridDirection direction
) {
}
