package it.uniupo.boardhub.eventservice.service.exception;

public class VenueAuthenticationException extends RuntimeException {

    public VenueAuthenticationException() {
        super("Credenziale del locale assente o non valida.");
    }
}
