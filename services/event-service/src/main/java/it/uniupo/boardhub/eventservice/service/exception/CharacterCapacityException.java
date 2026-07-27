package it.uniupo.boardhub.eventservice.service.exception;

public class CharacterCapacityException extends RuntimeException {

    public CharacterCapacityException(int maximum) {
        super("Il partecipante ha raggiunto il limite di " + maximum + " personaggi.");
    }
}
