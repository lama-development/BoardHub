package it.uniupo.boardhub.eventservice.controller.dto;

public record CloseSessionResponse(String sessionId, String status, String endedAt) {
}
