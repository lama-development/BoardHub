package it.uniupo.boardhub.statsservice.model;

import java.time.OffsetDateTime;
import java.util.List;

// Risultato di una sessione conclusa, come ricevuto dal servizio di gioco.
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
}
