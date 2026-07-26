package it.uniupo.boardhub.eventservice.controller;

import it.uniupo.boardhub.eventservice.controller.dto.ParticipantResponse;
import it.uniupo.boardhub.eventservice.controller.mapper.JoinDtoMapper;
import it.uniupo.boardhub.eventservice.service.ParticipantAccessService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/player/sessions/{sessionId}")
public class PlayerSessionController {

    private final ParticipantAccessService participantAccessService;

    public PlayerSessionController(ParticipantAccessService participantAccessService) {
        this.participantAccessService = participantAccessService;
    }

    // Verifica la credenziale e restituisce l'identita attiva del giocatore nella sessione.
    @GetMapping("/me")
    public ParticipantResponse getCurrentParticipant(
            @PathVariable String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return JoinDtoMapper.toResponse(
                participantAccessService.requireActiveParticipant(sessionId, authorization)
        );
    }
}
