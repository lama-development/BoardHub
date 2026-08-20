package it.uniupo.boardhub.statsservice.service;

import it.uniupo.boardhub.statsservice.model.PlayerResult;

// Formula di punteggio della piattaforma: D&D e cooperativo e non prevede un
// vincitore fra i giocatori. Il punteggio si calcola una sola volta, in ingresso,
// cosi una partita gia giocata non cambia se la formula viene modificata.
public final class ScoringRule {

    public static final int POINTS_SESSION_COMPLETED = 3;
    public static final int POINTS_SURVIVED = 2;
    public static final int POINTS_PER_SAVE_SUCCEEDED = 1;
    public static final int POINTS_DOWNED = -1;

    private ScoringRule() {
    }

    public static int pointsFor(PlayerResult result) {
        int points = POINTS_SESSION_COMPLETED;
        points += result.survived() ? POINTS_SURVIVED : POINTS_DOWNED;
        points += result.savesSucceeded() * POINTS_PER_SAVE_SUCCEEDED;
        return points;
    }

    public static String describe() {
        return "sessione completata +%d, sopravvissuto +%d, tiro salvezza riuscito +%d, abbattuto %d"
                .formatted(
                        POINTS_SESSION_COMPLETED,
                        POINTS_SURVIVED,
                        POINTS_PER_SAVE_SUCCEEDED,
                        POINTS_DOWNED
                );
    }
}
