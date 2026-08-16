package it.uniupo.boardhub.eventservice.controller;

import it.uniupo.boardhub.eventservice.controller.dto.ContinueTrapMovementRequest;
import it.uniupo.boardhub.eventservice.controller.dto.PieceMoveResponse;
import it.uniupo.boardhub.eventservice.controller.dto.RollTrapSaveRequest;
import it.uniupo.boardhub.eventservice.controller.dto.TrapResolutionResponse;
import it.uniupo.boardhub.eventservice.controller.dto.TrapRollResponse;
import it.uniupo.boardhub.eventservice.model.piece.PieceMoveResult;
import it.uniupo.boardhub.eventservice.model.trap.TrapResolution;
import it.uniupo.boardhub.eventservice.model.trap.TrapRollResult;
import it.uniupo.boardhub.eventservice.service.DmAccessService;
import it.uniupo.boardhub.eventservice.service.TrapResolutionService;
import it.uniupo.boardhub.eventservice.service.command.ContinueTrapMovementCommand;
import it.uniupo.boardhub.eventservice.service.command.RollTrapSaveCommand;
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
@RequestMapping("/api/v1/dm/sessions/{sessionId}/trap-resolutions/{resolutionId}")
public class DmTrapResolutionController {

    private final DmAccessService dmAccessService;
    private final TrapResolutionService service;

    public DmTrapResolutionController(DmAccessService dmAccessService, TrapResolutionService service) {
        this.dmAccessService = dmAccessService;
        this.service = service;
    }

    @GetMapping
    public TrapResolutionResponse get(
            @PathVariable String sessionId,
            @PathVariable UUID resolutionId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        dmAccessService.requireAuthorized(sessionId, authorization);
        TrapResolution resolution = service.getAsDm(sessionId, resolutionId);
        return new TrapResolutionResponse(
                resolution.resolutionId(), resolution.status().name(), resolution.sessionPieceId(),
                resolution.characterId(), resolution.requestedDestination(), resolution.triggerCell(),
                resolution.movementRemaining(), resolution.version()
        );
    }

    @PostMapping("/roll")
    public TrapRollResponse roll(
            @PathVariable String sessionId,
            @PathVariable UUID resolutionId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody RollTrapSaveRequest request
    ) {
        var dm = dmAccessService.requireAuthorized(sessionId, authorization);
        TrapRollResult result = service.rollAsDm(
                sessionId, resolutionId, dm.participantId(),
                request == null ? null : new RollTrapSaveCommand(
                        request.expectedVersion(), request.commandId()
                )
        );
        return new TrapRollResponse(
                result.resolutionId(), result.status().name(), result.d20First(), result.d20Second(),
                result.selectedD20(), result.saveBonus(), result.saveTotal(), result.success(),
                result.damageRolls(), result.damageTotal(), result.hpCurrent(),
                result.tacticalStatus().name(), result.movementDecision().name(),
                result.movementRemaining(), result.version()
        );
    }

    @PostMapping("/continue")
    public ResponseEntity<PieceMoveResponse> continueMovement(
            @PathVariable String sessionId,
            @PathVariable UUID resolutionId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody ContinueTrapMovementRequest request
    ) {
        var dm = dmAccessService.requireAuthorized(sessionId, authorization);
        PieceMoveResult result = service.continueMovementAsDm(
                sessionId, resolutionId, dm.participantId(),
                request == null ? null : new ContinueTrapMovementCommand(
                        request.expectedVersion(), request.commandId()
                )
        );
        PieceMoveResponse response = new PieceMoveResponse(
                result.status().name(), result.commandId(), result.eventId(),
                result.sessionPieceId(), result.characterId(), result.from(), result.to(),
                result.path(), result.cost(), result.version(), result.visibleTrapsOnPath(),
                result.resolutionId(), result.requestedDestination(), result.movementRemaining()
        );
        return ResponseEntity.status(
                result.status() == PieceMoveResult.Status.TRAP_PENDING
                        ? HttpStatus.ACCEPTED
                        : HttpStatus.OK
        ).body(response);
    }
}
