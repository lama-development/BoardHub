package it.uniupo.boardhub.statsservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidRequest(IllegalArgumentException ex) {
        return new ErrorResponse("INVALID_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        String code = ex.getStatusCode().value() == HttpStatus.NOT_FOUND.value()
                ? "RESOURCE_NOT_FOUND"
                : "REQUEST_FAILED";
        String message = ex.getReason() == null ? "Richiesta non completata." : ex.getReason();
        return ResponseEntity.status(ex.getStatusCode()).body(new ErrorResponse(code, message));
    }
}
