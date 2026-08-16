package it.uniupo.boardhub.eventservice.controller;

import it.uniupo.boardhub.eventservice.controller.dto.CharacterControlResponse;
import it.uniupo.boardhub.eventservice.service.CharacterControlService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dm/sessions/{sessionId}/characters/{characterId}/control")
public class DmCharacterControlController {

    private final CharacterControlService service;

    public DmCharacterControlController(CharacterControlService service) {
        this.service = service;
    }

    @PostMapping
    public CharacterControlResponse assume(
            @PathVariable String sessionId,
            @PathVariable UUID characterId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        var control = service.assume(sessionId, characterId, authorization);
        return new CharacterControlResponse(
                control.sessionId(), control.characterId(), control.dmParticipantId(),
                control.version(), control.assumedAt().toString()
        );
    }

    @DeleteMapping
    public ResponseEntity<Void> release(
            @PathVariable String sessionId,
            @PathVariable UUID characterId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        service.release(sessionId, characterId, authorization);
        return ResponseEntity.noContent().build();
    }
}
