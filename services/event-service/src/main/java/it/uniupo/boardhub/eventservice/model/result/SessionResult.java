package it.uniupo.boardhub.eventservice.model.result;

import java.time.OffsetDateTime;
import java.util.List;

// Riepilogo pubblicato alla chiusura di una sessione.
public record SessionResult(
        String sessionId,
        String venueId,
        String tableId,
        String title,
        String gameType,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        long durationMinutes,
        List<PlayerResult> participants
) {
    public SessionResult {
        participants = List.copyOf(participants);
    }

    public String factId() {
        return "result-" + sessionId;
    }
}
