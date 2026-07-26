package it.uniupo.boardhub.eventservice.controller.dto;

// Risposta riservata che consegna al giocatore la credenziale della sessione.
public record AcceptJoinRequestResponse(
        JoinRequestResponse request,
        ParticipantResponse participant,
        String accessToken
) {
}
