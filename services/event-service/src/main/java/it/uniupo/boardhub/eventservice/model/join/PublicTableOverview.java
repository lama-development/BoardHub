package it.uniupo.boardhub.eventservice.model.join;

// Vista pubblica del tavolo, valida anche quando non ospita una sessione.
public record PublicTableOverview(
        String tablePublicId,
        int tableNumber,
        String tableDisplayName,
        PublicTableAvailability availability,
        PublicSessionInfo activeSession
) {
}
