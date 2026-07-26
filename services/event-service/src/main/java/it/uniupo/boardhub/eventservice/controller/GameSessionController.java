package it.uniupo.boardhub.eventservice.controller;

import it.uniupo.boardhub.eventservice.controller.dto.CreateGameSessionRequest;
import it.uniupo.boardhub.eventservice.controller.dto.CreateGameSessionResponse;
import it.uniupo.boardhub.eventservice.controller.mapper.ApiRequestMapper;
import it.uniupo.boardhub.eventservice.model.session.CreatedGameSession;
import it.uniupo.boardhub.eventservice.model.session.GameSession;
import it.uniupo.boardhub.eventservice.service.GameSessionCreationService;
import it.uniupo.boardhub.eventservice.service.DmAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions")
public class GameSessionController {

    private final GameSessionCreationService creationService;
    private final DmAccessService dmAccessService;

    public GameSessionController(
            GameSessionCreationService creationService,
            DmAccessService dmAccessService
    ) {
        this.creationService = creationService;
        this.dmAccessService = dmAccessService;
    }

    // Crea una sessione D&D con la configurazione iniziale della griglia.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateGameSessionResponse createSession(
            @RequestHeader(value = "X-BoardHub-DM-Key", required = false) String dmKey,
            @RequestBody CreateGameSessionRequest request
    ) {
        dmAccessService.requireAuthorized(dmKey);
        CreatedGameSession created = creationService.createSession(ApiRequestMapper.toCommand(request));
        GameSession session = created.session();
        return new CreateGameSessionResponse(
                session.sessionId(),
                session.venueId(),
                session.tableId(),
                created.table().tablePublicId(),
                created.table().displayName(),
                session.title(),
                session.gameType(),
                session.publicSummary(),
                session.acceptingJoinRequests(),
                session.status().name(),
                session.gridWidth(),
                session.gridHeight(),
                session.createdAt().toString()
        );
    }
}
