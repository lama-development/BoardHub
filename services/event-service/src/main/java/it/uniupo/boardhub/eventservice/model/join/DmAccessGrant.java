package it.uniupo.boardhub.eventservice.model.join;

// Identita DM persistita e token firmato restituito una sola volta al dispositivo.
public record DmAccessGrant(
        SessionParticipant participant,
        String accessToken
) {
}
