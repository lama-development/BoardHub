package it.uniupo.boardhub.eventservice.controller.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record SessionEventNotification(
        String eventId,
        String eventType,
        String sessionId,
        String occurredAt,
        long sequenceNumber,
        JsonNode payload
) {
}
