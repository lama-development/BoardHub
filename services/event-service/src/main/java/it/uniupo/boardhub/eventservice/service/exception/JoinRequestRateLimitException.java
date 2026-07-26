package it.uniupo.boardhub.eventservice.service.exception;

public class JoinRequestRateLimitException extends RuntimeException {

    public JoinRequestRateLimitException() {
        super("Troppe richieste di ingresso: attendere prima di riprovare.");
    }
}
