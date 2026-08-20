package it.uniupo.boardhub.eventservice.model.result;

// Contributo di un giocatore a una sessione conclusa, senza punteggio di torneo.
public record PlayerResult(
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
        int damageTaken
) {
}
