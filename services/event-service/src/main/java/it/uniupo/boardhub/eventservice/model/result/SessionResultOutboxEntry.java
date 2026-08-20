package it.uniupo.boardhub.eventservice.model.result;

import java.time.OffsetDateTime;

// Risultato di sessione in attesa di consegna al servizio statistiche.
public record SessionResultOutboxEntry(
        String factId,
        String sessionId,
        String topic,
        String payloadJson,
        int attemptCount,
        OffsetDateTime nextAttemptAt
) {
}
