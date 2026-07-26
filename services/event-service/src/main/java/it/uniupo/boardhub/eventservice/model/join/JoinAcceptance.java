package it.uniupo.boardhub.eventservice.model.join;

// Esito dell'accettazione con credenziale locale riproducibile e firmata.
public record JoinAcceptance(
        SessionJoinRequest request,
        SessionParticipant participant,
        String accessToken
) {
}
