package it.uniupo.boardhub.eventservice.controller;

import it.uniupo.boardhub.eventservice.controller.dto.CreateJoinRequestRequest;
import it.uniupo.boardhub.eventservice.controller.dto.JoinRequestResponse;
import it.uniupo.boardhub.eventservice.controller.dto.PlayerJoinStatusResponse;
import it.uniupo.boardhub.eventservice.controller.dto.PublicActiveSessionResponse;
import it.uniupo.boardhub.eventservice.controller.dto.PublicTableStatusResponse;
import it.uniupo.boardhub.eventservice.controller.mapper.ApiRequestMapper;
import it.uniupo.boardhub.eventservice.controller.mapper.JoinDtoMapper;
import it.uniupo.boardhub.eventservice.model.join.PublicSessionInfo;
import it.uniupo.boardhub.eventservice.model.join.PublicTableOverview;
import it.uniupo.boardhub.eventservice.model.join.PlayerJoinStatus;
import it.uniupo.boardhub.eventservice.service.JoinRequestService;
import it.uniupo.boardhub.eventservice.service.TableSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public")
public class PublicTableController {

    private final TableSessionService tableSessionService;
    private final JoinRequestService joinRequestService;

    public PublicTableController(
            TableSessionService tableSessionService,
            JoinRequestService joinRequestService
    ) {
        this.tableSessionService = tableSessionService;
        this.joinRequestService = joinRequestService;
    }

    // Traduce il QR del tavolo nella sola sessione pubblica attualmente disponibile.
    @GetMapping("/tables/{tablePublicId}/active-session")
    public PublicActiveSessionResponse getActiveSession(@PathVariable String tablePublicId) {
        PublicSessionInfo session = tableSessionService.resolveActiveSession(tablePublicId);
        return new PublicActiveSessionResponse(
                session.tablePublicId(), session.tableDisplayName(), session.sessionId(),
                session.title(), session.gameType(), session.publicSummary()
        );
    }

    // Restituisce sempre lo stato di un tavolo configurato, anche quando e libero.
    @GetMapping("/tables/{tablePublicId}")
    public PublicTableStatusResponse getTableStatus(@PathVariable String tablePublicId) {
        PublicTableOverview table = tableSessionService.resolvePublicTable(tablePublicId);
        return VenueTableAdminController.toResponse(table);
    }

    // Inserisce il giocatore nella coda del DM senza autorizzarlo automaticamente.
    @PostMapping("/sessions/{sessionId}/join-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public JoinRequestResponse createJoinRequest(
            @PathVariable String sessionId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody CreateJoinRequestRequest request
    ) {
        return JoinDtoMapper.toResponse(
                joinRequestService.create(
                        sessionId,
                        idempotencyKey,
                        ApiRequestMapper.toCommand(request)
                )
        );
    }

    // Recupera lo stato senza esporlo a chi conosce soltanto il requestId.
    @GetMapping("/sessions/{sessionId}/join-requests/{requestId}")
    public PlayerJoinStatusResponse getJoinRequestStatus(
            @PathVariable String sessionId,
            @PathVariable UUID requestId,
            @RequestHeader("X-BoardHub-Join-Claim") String joinClaim
    ) {
        PlayerJoinStatus status = joinRequestService.getPlayerStatus(
                sessionId, requestId, joinClaim
        );
        return new PlayerJoinStatusResponse(
                JoinDtoMapper.toResponse(status.request()),
                status.participant() == null ? null : JoinDtoMapper.toResponse(status.participant()),
                status.accessToken()
        );
    }
}
