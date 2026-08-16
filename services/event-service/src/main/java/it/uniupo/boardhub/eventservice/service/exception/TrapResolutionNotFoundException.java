package it.uniupo.boardhub.eventservice.service.exception;

public class TrapResolutionNotFoundException extends RuntimeException {
    public TrapResolutionNotFoundException() {
        super("La risoluzione della trappola non esiste nella sessione richiesta.");
    }
}
