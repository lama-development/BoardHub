package it.uniupo.boardhub.statsservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void restituisceUnErroreDiValidazioneStabile() {
        ErrorResponse response = handler.handleInvalidRequest(
                new IllegalArgumentException("Nome torneo obbligatorio.")
        );

        assertThat(response.code()).isEqualTo("INVALID_REQUEST");
        assertThat(response.message()).isEqualTo("Nome torneo obbligatorio.");
    }

    @Test
    void traduceUnaRisorsaAssenteNelContrattoComune() {
        ResponseEntity<ErrorResponse> response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Torneo non trovato.")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo(
                new ErrorResponse("RESOURCE_NOT_FOUND", "Torneo non trovato.")
        );
    }
}
