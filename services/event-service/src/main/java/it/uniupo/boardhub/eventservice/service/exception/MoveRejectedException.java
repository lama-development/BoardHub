package it.uniupo.boardhub.eventservice.service.exception;

public class MoveRejectedException extends RuntimeException {

    public MoveRejectedException(String message) {
        super(message);
    }
}
