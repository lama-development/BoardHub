package it.uniupo.boardhub.eventservice.model.session;

import it.uniupo.boardhub.eventservice.model.grid.TrapVisibility;

// Stato persistito di una trappola associata alla griglia della sessione.
public record GridTrapState(
        String sessionId,
        String trapId,
        String cell,
        TrapVisibility visibility,
        boolean armed
) {
}
