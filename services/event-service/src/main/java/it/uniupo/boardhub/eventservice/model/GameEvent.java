package it.uniupo.boardhub.eventservice.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

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

    public GameEvent {
        requireText(eventId, "eventId");
        requireText(eventType, "eventType");
        requireText(venueId, "venueId");
        requireText(tableId, "tableId");
        requireText(sessionId, "sessionId");
        requireText(source, "source");
        if (occurredAt == null || occurredAt.isBlank()) {
            throw new IllegalArgumentException("occurredAt e obbligatorio.");
        }
        try {
            OffsetDateTime.parse(occurredAt);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("occurredAt deve essere una data ISO 8601 valida.", ex);
        }
        if (sequenceNumber < 1) {
            throw new IllegalArgumentException("sequenceNumber deve essere maggiore di zero.");
        }
        if (payload == null || payload.isNull()) {
            throw new IllegalArgumentException("payload e obbligatorio.");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " e obbligatorio.");
        }
    }
}
