package it.uniupo.boardhub.eventservice.controller;

import it.uniupo.boardhub.eventservice.controller.dto.CreateGameSessionRequest;
import it.uniupo.boardhub.eventservice.controller.dto.CreateGameSessionResponse;
import it.uniupo.boardhub.eventservice.model.session.GameSession;
import it.uniupo.boardhub.eventservice.service.GameSessionCreationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions")
public class GameSessionController {

    private final GameSessionCreationService creationService;

    public GameSessionController(GameSessionCreationService creationService) {
        this.creationService = creationService;
    }

    // Crea una sessione D&D con la configurazione iniziale della griglia.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateGameSessionResponse createSession(@RequestBody CreateGameSessionRequest request) {
        GameSession session = creationService.createSession(request);
        return new CreateGameSessionResponse(
                session.sessionId(),
                session.venueId(),
                session.tableId(),
                session.title(),
                session.gameType(),
                session.status().name(),
                session.gridWidth(),
                session.gridHeight(),
                session.createdAt().toString()
        );
    }
}
