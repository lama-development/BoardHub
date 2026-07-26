package it.uniupo.boardhub.eventservice.service.exception;

public class TableSessionNotFoundException extends RuntimeException {

    public TableSessionNotFoundException(String tablePublicId) {
        super("Nessuna sessione pubblica attiva per il tavolo: " + tablePublicId);
    }
}
