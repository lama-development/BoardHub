package it.uniupo.boardhub.eventservice.service.command;

// Input applicativo per richiedere l'ingresso in una sessione.
public record CreateJoinRequestCommand(
        String playerReference,
        String displayName
) {
}
