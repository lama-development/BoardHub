package it.uniupo.boardhub.eventservice.service.exception;

public class SessionPieceConflictException extends RuntimeException {

    public SessionPieceConflictException(String message) {
        super(message);
    }
}
