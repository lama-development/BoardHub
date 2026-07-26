package it.uniupo.boardhub.eventservice.service.exception;

public class SessionCapacityException extends RuntimeException {

    public SessionCapacityException(String message) {
        super(message);
    }
}
