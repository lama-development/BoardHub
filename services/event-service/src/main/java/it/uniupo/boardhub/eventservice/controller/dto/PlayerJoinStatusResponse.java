package it.uniupo.boardhub.eventservice.controller.dto;

// Risposta privata del giocatore; partecipante e token esistono solo dopo l'accettazione.
public record PlayerJoinStatusResponse(
        JoinRequestResponse request,
        ParticipantResponse participant,
        String accessToken
) {
}
