package it.uniupo.boardhub.eventservice.controller.dto;

// Durata opzionale della finestra in cui il tavolo puo essere reclamato.
public record EnableTableRequest(Integer durationMinutes) {
}
