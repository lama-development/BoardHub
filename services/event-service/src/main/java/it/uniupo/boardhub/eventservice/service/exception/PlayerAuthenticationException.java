package it.uniupo.boardhub.eventservice.service.exception;

// Mantiene intenzionalmente generico l'errore per non rivelare dettagli della credenziale.
public class PlayerAuthenticationException extends RuntimeException {

    public PlayerAuthenticationException() {
        super("Credenziale giocatore assente, non valida o non piu attiva.");
    }
}
