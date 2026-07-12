package it.uniupo.boardhub.eventservice.service;

public class GameSessionNotFoundException extends RuntimeException {

    public GameSessionNotFoundException(String sessionId) {
        super("Sessione non trovata: " + sessionId);
    }
}
