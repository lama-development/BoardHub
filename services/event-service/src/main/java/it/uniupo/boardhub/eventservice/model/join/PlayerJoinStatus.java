package it.uniupo.boardhub.eventservice.model.join;

// Stato consultabile dal dispositivo che ha creato la richiesta di ingresso.
public record PlayerJoinStatus(
        SessionJoinRequest request,
        SessionParticipant participant,
        String accessToken
) {
}
