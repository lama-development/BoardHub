package it.uniupo.boardhub.eventservice.controller;

import it.uniupo.boardhub.eventservice.model.GameEvent;
import it.uniupo.boardhub.eventservice.repository.GameEventRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sessions")
public class GameEventController {

    private final GameEventRepository repository;

    public GameEventController(GameEventRepository repository) {
        this.repository = repository;
    }

    // Espone alla dashboard gli eventi persistiti di una sessione.
    @GetMapping("/{sessionId}/events")
    public List<GameEvent> findEventsBySession(@PathVariable String sessionId) {
        return repository.findBySessionId(sessionId);
    }
}
