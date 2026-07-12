package it.uniupo.boardhub.eventservice.service;

public class DuplicateGameSessionException extends RuntimeException {

    public DuplicateGameSessionException(String sessionId) {
        super("Sessione gia esistente: " + sessionId);
    }
}
