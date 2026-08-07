package it.uniupo.boardhub.eventservice.service.exception;

public class MoveCommandConflictException extends RuntimeException {

    public MoveCommandConflictException() {
        super("commandId e gia stato usato per un'altra operazione.");
    }
}
