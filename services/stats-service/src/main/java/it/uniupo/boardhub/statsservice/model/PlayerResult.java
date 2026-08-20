package it.uniupo.boardhub.statsservice.model;

// Contributo di un giocatore a una sessione conclusa, con il punteggio assegnato.
public record PlayerResult(
        String sessionId,
        String playerReference,
        String displayName,
        String characterName,
        String className,
        String species,
        int level,
        boolean survived,
        int movesConfirmed,
        int cellsTravelled,
        int trapsTriggered,
        int savesSucceeded,
        int savesFailed,
        int damageTaken,
        int points
) {
}
