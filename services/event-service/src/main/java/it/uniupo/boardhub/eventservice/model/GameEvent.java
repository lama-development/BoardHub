package it.uniupo.boardhub.eventservice.model;

import com.fasterxml.jackson.databind.JsonNode;

// Evento di gioco ricevuto dal topic MQTT.
public record GameEvent(
        String eventId,
        String eventType,
        String venueId,
        String tableId,
        String sessionId,
        String source,
        String occurredAt,
        long sequenceNumber,
        JsonNode payload
) {
}
