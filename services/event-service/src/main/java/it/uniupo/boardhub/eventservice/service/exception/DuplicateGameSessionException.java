package it.uniupo.boardhub.eventservice.service.exception;

public class DuplicateGameSessionException extends RuntimeException {

    public DuplicateGameSessionException(String sessionId) {
        super("Sessione gia esistente: " + sessionId);
    }
}
