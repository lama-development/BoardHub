package it.uniupo.boardhub.eventservice.service.exception;

public class StalePieceStateException extends RuntimeException {

    public StalePieceStateException(long currentVersion) {
        super("La pedina e stata modificata. Ricaricare lo stato corrente (versione "
                + currentVersion + ").");
    }
}
