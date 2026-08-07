package it.uniupo.boardhub.eventservice.controller;

import it.uniupo.boardhub.eventservice.controller.dto.MoveSessionPieceRequest;
import it.uniupo.boardhub.eventservice.controller.dto.PieceMoveResponse;
import it.uniupo.boardhub.eventservice.controller.dto.PieceReachabilityResponse;
import it.uniupo.boardhub.eventservice.controller.mapper.MovementDtoMapper;
import it.uniupo.boardhub.eventservice.model.piece.PieceMoveResult;
import it.uniupo.boardhub.eventservice.model.piece.PieceReachability;
import it.uniupo.boardhub.eventservice.service.SessionPieceMovementService;
import it.uniupo.boardhub.eventservice.service.command.MoveSessionPieceCommand;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/player/sessions/{sessionId}/pieces/{sessionPieceId}")
public class PlayerSessionPieceMovementController {

    private final SessionPieceMovementService movementService;

    public PlayerSessionPieceMovementController(SessionPieceMovementService movementService) {
        this.movementService = movementService;
    }

    @GetMapping("/reachable-cells")
    public PieceReachabilityResponse reachableCells(
            @PathVariable String sessionId,
            @PathVariable UUID sessionPieceId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        PieceReachability result = movementService.reachableCells(
                sessionId,
                sessionPieceId,
                authorization
        );
        return new PieceReachabilityResponse(
                result.sessionPieceId(),
                result.currentCell(),
                result.movementPoints(),
                result.version(),
                result.reachableCells().stream()
                        .map(MovementDtoMapper::toResponse)
                        .toList()
        );
    }

    @PostMapping("/moves")
    public PieceMoveResponse move(
            @PathVariable String sessionId,
            @PathVariable UUID sessionPieceId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody MoveSessionPieceRequest request
    ) {
        MoveSessionPieceCommand command = request == null
                ? null
                : new MoveSessionPieceCommand(
                        request.destination(),
                        request.expectedVersion(),
                        request.commandId()
                );
        PieceMoveResult result = movementService.move(
                sessionId,
                sessionPieceId,
                authorization,
                command
        );
        return new PieceMoveResponse(
                "CONFIRMED",
                result.commandId(),
                result.eventId(),
                result.sessionPieceId(),
                result.characterId(),
                result.from(),
                result.to(),
                result.path(),
                result.cost(),
                result.version(),
                result.visibleTrapsOnPath()
        );
    }
}
