package it.uniupo.boardhub.eventservice.controller;

import it.uniupo.boardhub.eventservice.controller.dto.EnableTableRequest;
import it.uniupo.boardhub.eventservice.controller.dto.PublicActiveSessionResponse;
import it.uniupo.boardhub.eventservice.controller.dto.PublicTableStatusResponse;
import it.uniupo.boardhub.eventservice.model.join.PublicSessionInfo;
import it.uniupo.boardhub.eventservice.model.join.PublicTableOverview;
import it.uniupo.boardhub.eventservice.service.SessionLifecycleService;
import it.uniupo.boardhub.eventservice.service.TableSessionService;
import it.uniupo.boardhub.eventservice.service.VenueAccessService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/tables")
public class VenueTableAdminController {

    private final TableSessionService tableSessionService;
    private final VenueAccessService venueAccessService;
    private final SessionLifecycleService lifecycleService;

    public VenueTableAdminController(
            TableSessionService tableSessionService,
            VenueAccessService venueAccessService,
            SessionLifecycleService lifecycleService
    ) {
        this.tableSessionService = tableSessionService;
        this.venueAccessService = venueAccessService;
        this.lifecycleService = lifecycleService;
    }

    // Elenca l'inventario dei tavoli dalla console privata del locale.
    @GetMapping
    public List<PublicTableStatusResponse> listTables(
            @RequestHeader(value = "X-BoardHub-Venue-Key", required = false) String venueKey,
            HttpServletRequest servletRequest
    ) {
        venueAccessService.requireAuthorized(venueKey, servletRequest.getRemoteAddr());
        return tableSessionService.listConfiguredTables().stream()
                .map(VenueTableAdminController::toResponse)
                .toList();
    }

    // Apre una finestra temporanea in cui il primo dispositivo puo creare la sessione.
    @PostMapping("/{tablePublicId}/enable")
    public PublicTableStatusResponse enableTable(
            @PathVariable String tablePublicId,
            @RequestHeader(value = "X-BoardHub-Venue-Key", required = false) String venueKey,
            @RequestBody(required = false) EnableTableRequest request,
            HttpServletRequest servletRequest
    ) {
        venueAccessService.requireAuthorized(venueKey, servletRequest.getRemoteAddr());
        tableSessionService.enableTable(
                tablePublicId,
                request == null ? null : request.durationMinutes()
        );
        return toResponse(tableSessionService.resolvePublicTable(tablePublicId));
    }

    // Revoca una finestra non ancora utilizzata senza alterare sessioni attive.
    @PostMapping("/{tablePublicId}/disable")
    public PublicTableStatusResponse disableTable(
            @PathVariable String tablePublicId,
            @RequestHeader(value = "X-BoardHub-Venue-Key", required = false) String venueKey,
            HttpServletRequest servletRequest
    ) {
        venueAccessService.requireAuthorized(venueKey, servletRequest.getRemoteAddr());
        tableSessionService.disableTable(tablePublicId);
        return toResponse(tableSessionService.resolvePublicTable(tablePublicId));
    }

    // Permette al personale del locale di terminare una sessione rimasta attiva.
    @PostMapping("/{tablePublicId}/close-session")
    public PublicTableStatusResponse closeActiveSession(
            @PathVariable String tablePublicId,
            @RequestHeader(value = "X-BoardHub-Venue-Key", required = false) String venueKey,
            HttpServletRequest servletRequest
    ) {
        venueAccessService.requireAuthorized(venueKey, servletRequest.getRemoteAddr());
        PublicTableOverview table = tableSessionService.resolvePublicTable(tablePublicId);
        if (table.activeSession() == null) {
            throw new IllegalArgumentException("Il tavolo non ospita una sessione attiva.");
        }
        lifecycleService.close(table.activeSession().sessionId());
        return toResponse(tableSessionService.resolvePublicTable(tablePublicId));
    }

    static PublicTableStatusResponse toResponse(PublicTableOverview table) {
        PublicSessionInfo session = table.activeSession();
        PublicActiveSessionResponse activeSession = session == null ? null : new PublicActiveSessionResponse(
                session.tablePublicId(),
                session.tableDisplayName(),
                session.sessionId(),
                session.title(),
                session.gameType(),
                session.publicSummary()
        );
        return new PublicTableStatusResponse(
                table.tablePublicId(),
                table.tableNumber(),
                table.tableDisplayName(),
                table.availability().name(),
                table.claimExpiresAt() == null ? null : table.claimExpiresAt().toString(),
                activeSession
        );
    }
}
