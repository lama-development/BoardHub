package it.uniupo.boardhub.eventservice.service.exception;

public class GameSessionNotFoundException extends RuntimeException {

    public GameSessionNotFoundException(String sessionId) {
        super("Sessione non trovata: " + sessionId);
    }
}
