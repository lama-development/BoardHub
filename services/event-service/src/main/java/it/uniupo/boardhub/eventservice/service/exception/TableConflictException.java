package it.uniupo.boardhub.eventservice.service.exception;

public class TableConflictException extends RuntimeException {

    public TableConflictException(String message) {
        super(message);
    }
}
