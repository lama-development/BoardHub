package it.uniupo.boardhub.eventservice.model.session;

import it.uniupo.boardhub.eventservice.model.join.GameTable;

// Risultato della creazione che include il riferimento pubblico del tavolo.
public record CreatedGameSession(GameSession session, GameTable table) {
}
