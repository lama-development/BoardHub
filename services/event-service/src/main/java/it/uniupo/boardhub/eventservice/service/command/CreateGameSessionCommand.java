package it.uniupo.boardhub.eventservice.service.command;

import it.uniupo.boardhub.eventservice.model.grid.GridConfiguration;

// Input applicativo per creare una sessione senza dipendere dai DTO HTTP.
public record CreateGameSessionCommand(
        String sessionId,
        String venueId,
        String tableId,
        String tablePublicId,
        String tableDisplayName,
        String title,
        String gameType,
        String publicSummary,
        Boolean acceptingJoinRequests,
        GridConfiguration grid
) {
}
