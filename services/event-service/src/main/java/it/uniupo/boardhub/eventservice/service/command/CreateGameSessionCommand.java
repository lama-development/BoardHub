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

    public CreateGameSessionCommand(
            String sessionId,
            String venueId,
            String tableId,
            String title,
            String gameType,
            GridConfiguration grid
    ) {
        this(sessionId, venueId, tableId, null, null, title, gameType, null, null, grid);
    }
}
