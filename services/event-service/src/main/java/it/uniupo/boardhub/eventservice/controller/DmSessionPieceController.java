package it.uniupo.boardhub.eventservice.controller;

import it.uniupo.boardhub.eventservice.controller.dto.SessionPieceResponse;
import it.uniupo.boardhub.eventservice.controller.mapper.SessionPieceDtoMapper;
import it.uniupo.boardhub.eventservice.repository.CharacterControlRepository;
import it.uniupo.boardhub.eventservice.service.DmAccessService;
import it.uniupo.boardhub.eventservice.service.SessionPieceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/dm/sessions/{sessionId}/pieces")
public class DmSessionPieceController {

    private final DmAccessService dmAccessService;
    private final SessionPieceService pieceService;
    private final CharacterControlRepository controlRepository;

    public DmSessionPieceController(
            DmAccessService dmAccessService,
            SessionPieceService pieceService,
            CharacterControlRepository controlRepository
    ) {
        this.dmAccessService = dmAccessService;
        this.pieceService = pieceService;
        this.controlRepository = controlRepository;
    }

    @GetMapping
    public List<SessionPieceResponse> listForDm(
            @PathVariable String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        dmAccessService.requireAuthorized(sessionId, authorization);
        Set<UUID> controlledCharacters = controlRepository.findBySession(sessionId).stream()
                .map(control -> control.characterId())
                .collect(Collectors.toUnmodifiableSet());
        return pieceService.listForDm(sessionId).stream()
                .map(piece -> SessionPieceDtoMapper.toResponse(
                        piece,
                        controlledCharacters.contains(piece.characterId())
                ))
                .toList();
    }
}
