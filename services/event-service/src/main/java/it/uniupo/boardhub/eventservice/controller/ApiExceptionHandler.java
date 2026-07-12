package it.uniupo.boardhub.eventservice.controller;

import it.uniupo.boardhub.eventservice.controller.dto.ErrorResponse;
import it.uniupo.boardhub.eventservice.service.DuplicateGameSessionException;
import it.uniupo.boardhub.eventservice.service.GameSessionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class ApiExceptionHandler {

    // Traduce JSON malformati o incompatibili in un errore client leggibile.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUnreadableBody(HttpMessageNotReadableException ex) {
        return new ErrorResponse("BAD_REQUEST", "Il corpo della richiesta non e leggibile.");
    }

    // Traduce le validazioni di dominio in errori leggibili per il client.
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(IllegalArgumentException ex) {
        return new ErrorResponse("BAD_REQUEST", ex.getMessage());
    }

    // Restituisce 404 quando il client chiede una sessione non ancora salvata.
    @ExceptionHandler(GameSessionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleSessionNotFound(GameSessionNotFoundException ex) {
        return new ErrorResponse("SESSION_NOT_FOUND", ex.getMessage());
    }

    // Restituisce 409 quando si prova a creare due volte la stessa sessione.
    @ExceptionHandler(DuplicateGameSessionException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicateSession(DuplicateGameSessionException ex) {
        return new ErrorResponse("DUPLICATE_SESSION", ex.getMessage());
    }
}
