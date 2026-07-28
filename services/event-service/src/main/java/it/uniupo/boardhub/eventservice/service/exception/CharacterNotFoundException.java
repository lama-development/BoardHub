package it.uniupo.boardhub.eventservice.service.exception;

public class CharacterNotFoundException extends RuntimeException {

    public CharacterNotFoundException() {
        super("Personaggio non trovato nella sessione per il giocatore autenticato.");
    }
}
