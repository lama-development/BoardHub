package it.uniupo.boardhub.eventservice.controller;

import it.uniupo.boardhub.eventservice.controller.dto.ErrorResponse;
import it.uniupo.boardhub.eventservice.service.exception.CharacterCapacityException;
import it.uniupo.boardhub.eventservice.service.exception.DmAuthenticationException;
import it.uniupo.boardhub.eventservice.service.exception.DuplicateGameSessionException;
import it.uniupo.boardhub.eventservice.service.exception.GameSessionNotFoundException;
import it.uniupo.boardhub.eventservice.service.exception.JoinRequestConflictException;
import it.uniupo.boardhub.eventservice.service.exception.JoinRequestNotFoundException;
import it.uniupo.boardhub.eventservice.service.exception.JoinRequestRateLimitException;
import it.uniupo.boardhub.eventservice.service.exception.PlayerAuthenticationException;
import it.uniupo.boardhub.eventservice.service.exception.SessionCapacityException;
import it.uniupo.boardhub.eventservice.service.exception.TableConflictException;
import it.uniupo.boardhub.eventservice.service.exception.TableSessionNotFoundException;
import it.uniupo.boardhub.eventservice.service.exception.VenueAuthenticationException;
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

    @ExceptionHandler({TableSessionNotFoundException.class, JoinRequestNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleJoinResourceNotFound(RuntimeException ex) {
        return new ErrorResponse("RESOURCE_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler({TableConflictException.class, JoinRequestConflictException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleJoinConflict(RuntimeException ex) {
        return new ErrorResponse("JOIN_CONFLICT", ex.getMessage());
    }

    @ExceptionHandler(SessionCapacityException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleCapacity(SessionCapacityException ex) {
        return new ErrorResponse("CAPACITY_REACHED", ex.getMessage());
    }

    @ExceptionHandler(CharacterCapacityException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleCharacterCapacity(CharacterCapacityException ex) {
        return new ErrorResponse("CHARACTER_CAPACITY_REACHED", ex.getMessage());
    }

    @ExceptionHandler(JoinRequestRateLimitException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ErrorResponse handleRateLimit(JoinRequestRateLimitException ex) {
        return new ErrorResponse("RATE_LIMITED", ex.getMessage());
    }

    @ExceptionHandler(DmAuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleDmAuthentication(DmAuthenticationException ex) {
        return new ErrorResponse("DM_UNAUTHORIZED", ex.getMessage());
    }

    @ExceptionHandler(PlayerAuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handlePlayerAuthentication(PlayerAuthenticationException ex) {
        return new ErrorResponse("PLAYER_UNAUTHORIZED", ex.getMessage());
    }

    @ExceptionHandler(VenueAuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleVenueAuthentication(VenueAuthenticationException ex) {
        return new ErrorResponse("VENUE_UNAUTHORIZED", ex.getMessage());
    }
}
