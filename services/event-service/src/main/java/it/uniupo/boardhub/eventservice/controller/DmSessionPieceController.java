package it.uniupo.boardhub.eventservice.controller;

import it.uniupo.boardhub.eventservice.controller.dto.SessionPieceResponse;
import it.uniupo.boardhub.eventservice.controller.mapper.SessionPieceDtoMapper;
import it.uniupo.boardhub.eventservice.service.DmAccessService;
import it.uniupo.boardhub.eventservice.service.SessionPieceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dm/sessions/{sessionId}/pieces")
public class DmSessionPieceController {

    private final DmAccessService dmAccessService;
    private final SessionPieceService pieceService;

    public DmSessionPieceController(
            DmAccessService dmAccessService,
            SessionPieceService pieceService
    ) {
        this.dmAccessService = dmAccessService;
        this.pieceService = pieceService;
    }

    @GetMapping
    public List<SessionPieceResponse> listForDm(
            @PathVariable String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        dmAccessService.requireAuthorized(sessionId, authorization);
        return pieceService.listForDm(sessionId).stream()
                .map(SessionPieceDtoMapper::toResponse)
                .toList();
    }
}
