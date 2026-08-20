package it.uniupo.boardhub.statsservice.controller;

import it.uniupo.boardhub.statsservice.model.LeaderboardEntry;
import it.uniupo.boardhub.statsservice.model.Tournament;
import it.uniupo.boardhub.statsservice.service.TournamentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tournaments")
public class TournamentController {

    private final TournamentService tournamentService;

    public TournamentController(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    public record CreateTournamentRequest(String name, String gameType, String venueId) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Tournament create(@RequestBody CreateTournamentRequest request) {
        return tournamentService.create(request.name(), request.gameType(), request.venueId());
    }

    @GetMapping
    public List<Tournament> all() {
        return tournamentService.findAll();
    }

    @GetMapping("/{tournamentId}")
    public Tournament one(@PathVariable UUID tournamentId) {
        return tournamentService.find(tournamentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Torneo non trovato."));
    }

    @GetMapping("/{tournamentId}/leaderboard")
    public List<LeaderboardEntry> leaderboard(@PathVariable UUID tournamentId) {
        tournamentService.find(tournamentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Torneo non trovato."));
        return tournamentService.leaderboard(tournamentId);
    }
}
