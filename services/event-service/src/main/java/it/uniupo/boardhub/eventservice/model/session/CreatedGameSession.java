package it.uniupo.boardhub.eventservice.model.session;

import it.uniupo.boardhub.eventservice.model.join.GameTable;

// Risultato atomico della creazione con tavolo reclamato e accesso del DM.
public record CreatedGameSession(
        GameSession session,
        GameTable table,
        String dmAccessToken
) {
}
