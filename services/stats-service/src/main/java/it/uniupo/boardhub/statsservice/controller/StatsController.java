package it.uniupo.boardhub.statsservice.controller;

import it.uniupo.boardhub.statsservice.model.PlayerResult;
import it.uniupo.boardhub.statsservice.model.PlayerStatistics;
import it.uniupo.boardhub.statsservice.model.SessionResult;
import it.uniupo.boardhub.statsservice.repository.SessionResultRepository;
import it.uniupo.boardhub.statsservice.service.ScoringRule;
import it.uniupo.boardhub.statsservice.service.SessionResultIngestionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stats")
public class StatsController {

    private final SessionResultIngestionService ingestionService;
    private final SessionResultRepository repository;

    public StatsController(
            SessionResultIngestionService ingestionService,
            SessionResultRepository repository
    ) {
        this.ingestionService = ingestionService;
        this.repository = repository;
    }

    @GetMapping("/sessions")
    public List<SessionResult> sessions() {
        return ingestionService.findAllSessions();
    }

    @GetMapping("/sessions/{sessionId}")
    public SessionResult session(@PathVariable String sessionId) {
        return ingestionService.findSession(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Nessun risultato per la sessione " + sessionId
                ));
    }

    @GetMapping("/sessions/{sessionId}/players")
    public List<PlayerResult> players(@PathVariable String sessionId) {
        return repository.findPlayers(sessionId);
    }

    @GetMapping("/players/{playerReference}")
    public PlayerStatistics player(@PathVariable String playerReference) {
        return ingestionService.findPlayer(playerReference)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Nessuna statistica per il giocatore " + playerReference
                ));
    }

    // Espone la formula usata, cosi una classifica resta verificabile da chi la legge.
    @GetMapping("/scoring-rule")
    public Map<String, Object> scoringRule() {
        return Map.of(
                "descrizione", ScoringRule.describe(),
                "sessioneCompletata", ScoringRule.POINTS_SESSION_COMPLETED,
                "sopravvissuto", ScoringRule.POINTS_SURVIVED,
                "tiroSalvezzaRiuscito", ScoringRule.POINTS_PER_SAVE_SUCCEEDED,
                "abbattuto", ScoringRule.POINTS_DOWNED,
                "nota", "Convenzione dimostrativa di BoardHub: D&D e cooperativo e non prevede un vincitore."
        );
    }
}
