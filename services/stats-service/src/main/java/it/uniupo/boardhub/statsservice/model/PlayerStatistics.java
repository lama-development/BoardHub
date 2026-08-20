package it.uniupo.boardhub.statsservice.model;

// Statistiche complessive di un giocatore su tutte le sessioni concluse.
public record PlayerStatistics(
        String playerReference,
        String displayName,
        int sessionsPlayed,
        int sessionsSurvived,
        double survivalRate,
        int totalPoints,
        int totalMoves,
        int totalCellsTravelled,
        int totalTrapsTriggered,
        int totalSavesSucceeded,
        int totalSavesFailed,
        int totalDamageTaken
) {
}
