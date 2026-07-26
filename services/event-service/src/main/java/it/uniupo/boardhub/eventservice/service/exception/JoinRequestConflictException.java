package it.uniupo.boardhub.eventservice.service.exception;

public class JoinRequestConflictException extends RuntimeException {

    public JoinRequestConflictException(String message) {
        super(message);
    }
}
