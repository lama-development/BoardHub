package it.uniupo.boardhub.eventservice.service.exception;

public class SessionPieceNotFoundException extends RuntimeException {

    public SessionPieceNotFoundException() {
        super("Pedina non trovata nella sessione o non posseduta dal giocatore.");
    }
}
