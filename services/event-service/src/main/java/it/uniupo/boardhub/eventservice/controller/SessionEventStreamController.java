package it.uniupo.boardhub.eventservice.controller;

import it.uniupo.boardhub.eventservice.service.DmAccessService;
import it.uniupo.boardhub.eventservice.service.ParticipantAccessService;
import it.uniupo.boardhub.eventservice.service.SessionEventStreamService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1")
public class SessionEventStreamController {

    private final ParticipantAccessService participantAccessService;
    private final DmAccessService dmAccessService;
    private final SessionEventStreamService streamService;

    public SessionEventStreamController(
            ParticipantAccessService participantAccessService,
            DmAccessService dmAccessService,
            SessionEventStreamService streamService
    ) {
        this.participantAccessService = participantAccessService;
        this.dmAccessService = dmAccessService;
        this.streamService = streamService;
    }

    @GetMapping(
            value = "/player/sessions/{sessionId}/events/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter playerStream(
            @PathVariable String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        var participant = participantAccessService.requireActiveParticipant(sessionId, authorization);
        return streamService.subscribePlayer(sessionId, participant.participantId());
    }

    @GetMapping(
            value = "/dm/sessions/{sessionId}/events/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter dmStream(
            @PathVariable String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        dmAccessService.requireAuthorized(sessionId, authorization);
        return streamService.subscribeDm(sessionId);
    }
}
