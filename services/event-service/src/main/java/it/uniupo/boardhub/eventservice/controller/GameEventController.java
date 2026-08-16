package it.uniupo.boardhub.eventservice.controller;

import it.uniupo.boardhub.eventservice.controller.dto.SessionEventNotification;
import it.uniupo.boardhub.eventservice.repository.GameEventRepository;
import it.uniupo.boardhub.eventservice.service.SessionEventStreamService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sessions")
public class GameEventController {

    private final GameEventRepository repository;
    private final SessionEventStreamService projectionService;

    public GameEventController(
            GameEventRepository repository,
            SessionEventStreamService projectionService
    ) {
        this.repository = repository;
        this.projectionService = projectionService;
    }

    // Mantiene la dashboard pubblica, ma restituisce soltanto eventi sanitizzati.
    @GetMapping("/{sessionId}/events")
    public List<SessionEventNotification> findPublicEvents(@PathVariable String sessionId) {
        return projectionService.projectPublic(repository.findBySessionId(sessionId));
    }
}
