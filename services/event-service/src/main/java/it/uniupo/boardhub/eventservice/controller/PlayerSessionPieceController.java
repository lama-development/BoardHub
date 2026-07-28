package it.uniupo.boardhub.eventservice.controller;

import it.uniupo.boardhub.eventservice.controller.dto.CreateSessionPieceRequest;
import it.uniupo.boardhub.eventservice.controller.dto.SessionPieceResponse;
import it.uniupo.boardhub.eventservice.controller.mapper.SessionPieceDtoMapper;
import it.uniupo.boardhub.eventservice.service.SessionPieceService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/player/sessions/{sessionId}/pieces")
public class PlayerSessionPieceController {

    private final SessionPieceService pieceService;

    public PlayerSessionPieceController(SessionPieceService pieceService) {
        this.pieceService = pieceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionPieceResponse create(
            @PathVariable String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody CreateSessionPieceRequest request
    ) {
        return SessionPieceDtoMapper.toResponse(
                pieceService.create(
                        sessionId,
                        authorization,
                        SessionPieceDtoMapper.toCommand(request)
                )
        );
    }

    @GetMapping
    public List<SessionPieceResponse> listOwned(
            @PathVariable String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return pieceService.listOwned(sessionId, authorization).stream()
                .map(SessionPieceDtoMapper::toResponse)
                .toList();
    }
}
