package it.uniupo.boardhub.eventservice.controller;

import it.uniupo.boardhub.eventservice.controller.dto.TrapResolutionResponse;
import it.uniupo.boardhub.eventservice.model.trap.TrapResolution;
import it.uniupo.boardhub.eventservice.repository.TrapResolutionRepository;
import it.uniupo.boardhub.eventservice.service.DmAccessService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dm/sessions/{sessionId}/trap-resolutions")
public class DmTrapResolutionCollectionController {

    private final DmAccessService dmAccessService;
    private final TrapResolutionRepository repository;

    public DmTrapResolutionCollectionController(
            DmAccessService dmAccessService,
            TrapResolutionRepository repository
    ) {
        this.dmAccessService = dmAccessService;
        this.repository = repository;
    }

    @GetMapping
    public List<TrapResolutionResponse> findPending(
            @PathVariable String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        dmAccessService.requireAuthorized(sessionId, authorization);
        return repository.findPendingBySession(sessionId).stream()
                .map(DmTrapResolutionCollectionController::toResponse)
                .toList();
    }

    private static TrapResolutionResponse toResponse(TrapResolution resolution) {
        return new TrapResolutionResponse(
                resolution.resolutionId(), resolution.status().name(), resolution.sessionPieceId(),
                resolution.characterId(), resolution.requestedDestination(), resolution.triggerCell(),
                resolution.movementRemaining(), resolution.version()
        );
    }
}
