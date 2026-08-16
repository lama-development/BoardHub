package it.uniupo.boardhub.eventservice.controller;

import it.uniupo.boardhub.eventservice.controller.dto.SessionEventNotification;
import it.uniupo.boardhub.eventservice.repository.GameEventRepository;
import it.uniupo.boardhub.eventservice.service.DmAccessService;
import it.uniupo.boardhub.eventservice.service.ParticipantAccessService;
import it.uniupo.boardhub.eventservice.service.SessionEventStreamService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class AuthenticatedGameEventController {

    private final GameEventRepository repository;
    private final ParticipantAccessService participantAccessService;
    private final DmAccessService dmAccessService;
    private final SessionEventStreamService projectionService;

    public AuthenticatedGameEventController(
            GameEventRepository repository,
            ParticipantAccessService participantAccessService,
            DmAccessService dmAccessService,
            SessionEventStreamService projectionService
    ) {
        this.repository = repository;
        this.participantAccessService = participantAccessService;
        this.dmAccessService = dmAccessService;
        this.projectionService = projectionService;
    }

    @GetMapping("/player/sessions/{sessionId}/events")
    public List<SessionEventNotification> findPlayerEvents(
            @PathVariable String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        var participant = participantAccessService.requireActiveParticipant(sessionId, authorization);
        return projectionService.projectPlayer(
                repository.findBySessionId(sessionId), participant.participantId()
        );
    }

    @GetMapping("/dm/sessions/{sessionId}/events")
    public List<SessionEventNotification> findDmEvents(
            @PathVariable String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        dmAccessService.requireAuthorized(sessionId, authorization);
        return projectionService.projectDm(repository.findBySessionId(sessionId));
    }
}
