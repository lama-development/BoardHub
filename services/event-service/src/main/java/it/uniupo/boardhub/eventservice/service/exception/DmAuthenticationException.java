package it.uniupo.boardhub.eventservice.service.exception;

public class DmAuthenticationException extends RuntimeException {

    public DmAuthenticationException() {
        super("Credenziale del Dungeon Master assente o non valida.");
    }
}
