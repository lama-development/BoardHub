package it.uniupo.boardhub.eventservice.controller;

import it.uniupo.boardhub.eventservice.controller.dto.CreateSessionPieceRequest;
import it.uniupo.boardhub.eventservice.controller.dto.SessionPieceResponse;
import it.uniupo.boardhub.eventservice.controller.mapper.SessionPieceDtoMapper;
import it.uniupo.boardhub.eventservice.repository.CharacterControlRepository;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/player/sessions/{sessionId}/pieces")
public class PlayerSessionPieceController {

    private final SessionPieceService pieceService;
    private final CharacterControlRepository controlRepository;

    public PlayerSessionPieceController(
            SessionPieceService pieceService,
            CharacterControlRepository controlRepository
    ) {
        this.pieceService = pieceService;
        this.controlRepository = controlRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionPieceResponse create(
            @PathVariable String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody CreateSessionPieceRequest request
    ) {
        var piece = pieceService.create(
                sessionId,
                authorization,
                SessionPieceDtoMapper.toCommand(request)
        );
        return toResponse(piece);
    }

    @GetMapping
    public List<SessionPieceResponse> listOwned(
            @PathVariable String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        var pieces = pieceService.listOwned(sessionId, authorization);
        Set<UUID> controlledCharacters = controlledCharacters(sessionId);
        return pieces.stream()
                .map(piece -> SessionPieceDtoMapper.toResponse(
                        piece, controlledCharacters.contains(piece.characterId())
                ))
                .toList();
    }

    private SessionPieceResponse toResponse(
            it.uniupo.boardhub.eventservice.model.piece.SessionPiece piece
    ) {
        boolean controlled = controlRepository.find(
                piece.sessionId(), piece.characterId()
        ).isPresent();
        return SessionPieceDtoMapper.toResponse(piece, controlled);
    }

    private Set<UUID> controlledCharacters(String sessionId) {
        return controlRepository.findBySession(sessionId).stream()
                .map(control -> control.characterId())
                .collect(Collectors.toUnmodifiableSet());
    }
}
