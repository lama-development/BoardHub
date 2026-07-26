package it.uniupo.boardhub.eventservice.model.join;

// Proiezione pubblica minima restituita prima dell'approvazione del DM.
public record PublicSessionInfo(
        String tablePublicId,
        String tableDisplayName,
        String sessionId,
        String title,
        String gameType,
        String publicSummary
) {
}
