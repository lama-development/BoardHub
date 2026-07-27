package it.uniupo.boardhub.eventservice.controller;

import it.uniupo.boardhub.eventservice.controller.dto.AcceptJoinRequestResponse;
import it.uniupo.boardhub.eventservice.controller.dto.CharacterResponse;
import it.uniupo.boardhub.eventservice.controller.dto.CloseSessionResponse;
import it.uniupo.boardhub.eventservice.controller.dto.JoinRequestResponse;
import it.uniupo.boardhub.eventservice.controller.dto.ParticipantResponse;
import it.uniupo.boardhub.eventservice.controller.mapper.CharacterDtoMapper;
import it.uniupo.boardhub.eventservice.controller.mapper.JoinDtoMapper;
import it.uniupo.boardhub.eventservice.model.join.JoinAcceptance;
import it.uniupo.boardhub.eventservice.model.join.JoinRequestStatus;
import it.uniupo.boardhub.eventservice.service.CharacterService;
import it.uniupo.boardhub.eventservice.service.DmAccessService;
import it.uniupo.boardhub.eventservice.service.JoinRequestService;
import it.uniupo.boardhub.eventservice.service.SessionLifecycleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dm/sessions/{sessionId}")
public class DmSessionController {

    private final JoinRequestService joinRequestService;
    private final DmAccessService dmAccessService;
    private final SessionLifecycleService lifecycleService;
    private final CharacterService characterService;

    public DmSessionController(
            JoinRequestService joinRequestService,
            DmAccessService dmAccessService,
            SessionLifecycleService lifecycleService,
            CharacterService characterService
    ) {
        this.joinRequestService = joinRequestService;
        this.dmAccessService = dmAccessService;
        this.lifecycleService = lifecycleService;
        this.characterService = characterService;
    }

    // Mostra al DM le richieste filtrate per stato, in ordine di arrivo.
    @GetMapping("/join-requests")
    public List<JoinRequestResponse> listJoinRequests(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        dmAccessService.requireAuthorized(sessionId, authorization);
        JoinRequestStatus parsedStatus = parseStatus(status);
        return joinRequestService.list(sessionId, parsedStatus).stream()
                .map(JoinDtoMapper::toResponse)
                .toList();
    }

    // Approva il giocatore e restituisce la credenziale locale firmata.
    @PostMapping("/join-requests/{requestId}/accept")
    public AcceptJoinRequestResponse accept(
            @PathVariable String sessionId,
            @PathVariable UUID requestId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        dmAccessService.requireAuthorized(sessionId, authorization);
        JoinAcceptance result = joinRequestService.accept(sessionId, requestId);
        return new AcceptJoinRequestResponse(
                JoinDtoMapper.toResponse(result.request()),
                JoinDtoMapper.toResponse(result.participant()),
                result.accessToken()
        );
    }

    // Rifiuta la richiesta senza creare alcun partecipante.
    @PostMapping("/join-requests/{requestId}/reject")
    public JoinRequestResponse reject(
            @PathVariable String sessionId,
            @PathVariable UUID requestId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        dmAccessService.requireAuthorized(sessionId, authorization);
        return JoinDtoMapper.toResponse(joinRequestService.reject(sessionId, requestId));
    }

    // Restituisce al DM i soli partecipanti attualmente attivi.
    @GetMapping("/participants")
    public List<ParticipantResponse> listParticipants(
            @PathVariable String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        dmAccessService.requireAuthorized(sessionId, authorization);
        return joinRequestService.listActiveParticipants(sessionId).stream()
                .map(JoinDtoMapper::toResponse)
                .toList();
    }

    // Mostra al DM tutti i personaggi della sessione, inclusi quelli non visibili al gruppo.
    @GetMapping("/characters")
    public List<CharacterResponse> listCharacters(
            @PathVariable String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        dmAccessService.requireAuthorized(sessionId, authorization);
        return characterService.listForDm(sessionId).stream()
                .map(CharacterDtoMapper::toResponse)
                .toList();
    }

    // Conclude la partita e disabilita il tavolo finché il locale non lo riabilita.
    @PostMapping("/close")
    public CloseSessionResponse closeSession(
            @PathVariable String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        dmAccessService.requireAuthorized(sessionId, authorization);
        return new CloseSessionResponse(
                sessionId, "ENDED", lifecycleService.close(sessionId).toString()
        );
    }

    private JoinRequestStatus parseStatus(String status) {
        try {
            return JoinRequestStatus.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("status della richiesta non valido.");
        }
    }
}
