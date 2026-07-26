package it.uniupo.boardhub.eventservice.controller.dto;

// Informazioni pubbliche sufficienti a riconoscere la partita prima dell'ingresso.
public record PublicActiveSessionResponse(
        String tablePublicId,
        String tableDisplayName,
        String sessionId,
        String title,
        String gameType,
        String publicSummary
) {
}
