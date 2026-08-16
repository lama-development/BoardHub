package it.uniupo.boardhub.eventservice.controller;

import it.uniupo.boardhub.eventservice.controller.dto.MoveSessionPieceRequest;
import it.uniupo.boardhub.eventservice.controller.dto.PieceMoveResponse;
import it.uniupo.boardhub.eventservice.controller.dto.PieceReachabilityResponse;
import it.uniupo.boardhub.eventservice.controller.mapper.MovementDtoMapper;
import it.uniupo.boardhub.eventservice.model.piece.PieceMoveResult;
import it.uniupo.boardhub.eventservice.service.DmAccessService;
import it.uniupo.boardhub.eventservice.service.SessionPieceMovementService;
import it.uniupo.boardhub.eventservice.service.command.MoveSessionPieceCommand;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dm/sessions/{sessionId}/pieces/{sessionPieceId}")
public class DmControlledPieceMovementController {

    private final DmAccessService dmAccessService;
    private final SessionPieceMovementService movementService;

    public DmControlledPieceMovementController(
            DmAccessService dmAccessService,
            SessionPieceMovementService movementService
    ) {
        this.dmAccessService = dmAccessService;
        this.movementService = movementService;
    }

    @GetMapping("/reachable-cells")
    public PieceReachabilityResponse reachableCells(
            @PathVariable String sessionId,
            @PathVariable UUID sessionPieceId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        var dm = dmAccessService.requireAuthorized(sessionId, authorization);
        var result = movementService.reachableCellsAsDm(
                sessionId, sessionPieceId, dm.participantId()
        );
        return new PieceReachabilityResponse(
                result.sessionPieceId(), result.currentCell(), result.movementPoints(),
                result.version(), result.reachableCells().stream()
                        .map(MovementDtoMapper::toResponse)
                        .toList()
        );
    }

    @PostMapping("/moves")
    public ResponseEntity<PieceMoveResponse> move(
            @PathVariable String sessionId,
            @PathVariable UUID sessionPieceId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody MoveSessionPieceRequest request
    ) {
        var dm = dmAccessService.requireAuthorized(sessionId, authorization);
        MoveSessionPieceCommand command = request == null ? null : new MoveSessionPieceCommand(
                request.destination(), request.expectedVersion(), request.commandId()
        );
        PieceMoveResult result = movementService.moveAsDm(
                sessionId, sessionPieceId, dm.participantId(), command
        );
        PieceMoveResponse response = toResponse(result);
        return ResponseEntity.status(
                result.status() == PieceMoveResult.Status.TRAP_PENDING
                        ? HttpStatus.ACCEPTED
                        : HttpStatus.OK
        ).body(response);
    }

    private PieceMoveResponse toResponse(PieceMoveResult result) {
        return new PieceMoveResponse(
                result.status().name(), result.commandId(), result.eventId(),
                result.sessionPieceId(), result.characterId(), result.from(), result.to(),
                result.path(), result.cost(), result.version(), result.visibleTrapsOnPath(),
                result.resolutionId(), result.requestedDestination(), result.movementRemaining()
        );
    }
}
