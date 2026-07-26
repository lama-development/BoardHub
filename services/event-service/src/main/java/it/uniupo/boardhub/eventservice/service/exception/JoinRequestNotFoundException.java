package it.uniupo.boardhub.eventservice.service.exception;

import java.util.UUID;

public class JoinRequestNotFoundException extends RuntimeException {

    public JoinRequestNotFoundException(UUID requestId) {
        super("Richiesta di ingresso non trovata: " + requestId);
    }
}
