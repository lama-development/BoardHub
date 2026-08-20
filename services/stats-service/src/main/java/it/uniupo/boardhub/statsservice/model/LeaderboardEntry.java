package it.uniupo.boardhub.statsservice.model;

// Riga di classifica: un giocatore e il totale accumulato nel torneo.
public record LeaderboardEntry(
        int position,
        String playerReference,
        String displayName,
        int sessionsPlayed,
        int points,
        int savesSucceeded,
        int cellsTravelled,
        int timesDowned
) {
}
