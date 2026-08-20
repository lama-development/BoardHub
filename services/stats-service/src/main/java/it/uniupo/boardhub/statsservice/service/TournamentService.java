package it.uniupo.boardhub.statsservice.service;

import it.uniupo.boardhub.statsservice.model.LeaderboardEntry;
import it.uniupo.boardhub.statsservice.model.Tournament;
import it.uniupo.boardhub.statsservice.repository.SessionResultRepository;
import it.uniupo.boardhub.statsservice.repository.TournamentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TournamentService {

    private static final int MAX_NAME_LENGTH = 120;

    private final TournamentRepository tournamentRepository;
    private final SessionResultRepository resultRepository;
    private final Clock clock;

    public TournamentService(
            TournamentRepository tournamentRepository,
            SessionResultRepository resultRepository,
            Clock clock
    ) {
        this.tournamentRepository = tournamentRepository;
        this.resultRepository = resultRepository;
        this.clock = clock;
    }

    // Include le sessioni gia concluse dello stesso gioco e locale: l'insieme resta
    // fisso e la classifica non cambia quando altrove finisce un'altra partita.
    @Transactional
    public Tournament create(String name, String gameType, String venueId) {
        String safeName = requireText(name, "name");
        if (safeName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Il nome del torneo supera i " + MAX_NAME_LENGTH + " caratteri.");
        }
        String safeGameType = requireText(gameType, "gameType");
        String safeVenueId = requireText(venueId, "venueId");

        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        Tournament tournament = new Tournament(UUID.randomUUID(), safeName, safeGameType, safeVenueId, now);
        tournamentRepository.save(tournament);

        for (String sessionId : resultRepository.findSessionIdsFor(safeGameType, safeVenueId)) {
            tournamentRepository.addSession(tournament.tournamentId(), sessionId, now);
        }
        return tournament;
    }

    public List<Tournament> findAll() {
        return tournamentRepository.findAll();
    }

    public Optional<Tournament> find(UUID tournamentId) {
        return tournamentRepository.findById(tournamentId);
    }

    public List<LeaderboardEntry> leaderboard(UUID tournamentId) {
        return tournamentRepository.leaderboard(tournamentId);
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " e obbligatorio.");
        }
        return value.trim();
    }
}
