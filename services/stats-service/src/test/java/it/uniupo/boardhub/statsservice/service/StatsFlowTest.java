package it.uniupo.boardhub.statsservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.boardhub.statsservice.model.LeaderboardEntry;
import it.uniupo.boardhub.statsservice.model.SessionResult;
import it.uniupo.boardhub.statsservice.model.Tournament;
import it.uniupo.boardhub.statsservice.repository.SessionResultRepository;
import it.uniupo.boardhub.statsservice.repository.TournamentRepository;
import it.uniupo.boardhub.statsservice.support.MigratedTestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Verifica il percorso completo: fatto ricevuto, risultato salvato, classifica.
class StatsFlowTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private SessionResultIngestionService ingestion;
    private TournamentService tournaments;
    private SessionResultRepository resultRepository;

    @BeforeEach
    void setUp() {
        JdbcTemplate jdbcTemplate = MigratedTestDatabase.create();
        resultRepository = new SessionResultRepository(jdbcTemplate);
        ingestion = new SessionResultIngestionService(resultRepository);
        tournaments = new TournamentService(
                new TournamentRepository(jdbcTemplate),
                resultRepository,
                Clock.fixed(Instant.parse("2026-08-18T10:00:00Z"), ZoneOffset.UTC)
        );
    }

    private String fact(String sessionId, String andreaSaves, boolean andreaSurvived, String biancaSaves) {
        return """
                {
                  "factId": "result-%1$s",
                  "factType": "SESSION_COMPLETED",
                  "sessionId": "%1$s",
                  "venueId": "venue-01",
                  "tableId": "table-04",
                  "title": "Cripta del Re Caduto",
                  "gameType": "DND",
                  "startedAt": "2026-08-18T20:00:00Z",
                  "endedAt": "2026-08-18T22:30:00Z",
                  "durationMinutes": 150,
                  "participants": [
                    {
                      "playerReference": "player-device-01",
                      "displayName": "Andrea",
                      "characterName": "Elaria",
                      "className": "Ladro",
                      "species": "Elfo",
                      "level": 3,
                      "survived": %2$s,
                      "movesConfirmed": 12,
                      "cellsTravelled": 27,
                      "trapsTriggered": 2,
                      "savesSucceeded": %3$s,
                      "savesFailed": 1,
                      "damageTaken": 7
                    },
                    {
                      "playerReference": "player-device-02",
                      "displayName": "Bianca",
                      "characterName": "Kaelen",
                      "className": "Guerriero",
                      "species": "Nano",
                      "level": 3,
                      "survived": true,
                      "movesConfirmed": 9,
                      "cellsTravelled": 18,
                      "trapsTriggered": 1,
                      "savesSucceeded": %4$s,
                      "savesFailed": 0,
                      "damageTaken": 0
                    }
                  ]
                }
                """.formatted(sessionId, andreaSurvived, andreaSaves, biancaSaves);
    }

    private void ingest(String payload) throws Exception {
        ingestion.ingest(ingestion.parse(objectMapper.readTree(payload)));
    }

    @Test
    void unaSessioneConclusaDiventaUnRisultatoConsultabile() throws Exception {
        ingest(fact("session-001", "2", true, "0"));

        SessionResult stored = ingestion.findSession("session-001").orElseThrow();
        assertThat(stored.title()).isEqualTo("Cripta del Re Caduto");
        assertThat(stored.durationMinutes()).isEqualTo(150);
        assertThat(stored.participants()).hasSize(2);
    }

    @Test
    void unaConsegnaRipetutaNonRaddoppiaIDati() throws Exception {
        ingest(fact("session-001", "2", true, "0"));
        ingest(fact("session-001", "2", true, "0"));

        assertThat(resultRepository.findPlayers("session-001")).hasSize(2);
        assertThat(ingestion.findAllSessions()).hasSize(1);
    }

    @Test
    void ilPunteggioVienePersistitoAllIngestione() throws Exception {
        ingest(fact("session-001", "2", true, "0"));

        var andrea = resultRepository.findPlayers("session-001").stream()
                .filter(player -> player.playerReference().equals("player-device-01"))
                .findFirst().orElseThrow();
        // 3 sessione completata + 2 sopravvissuto + 2 tiri riusciti.
        assertThat(andrea.points()).isEqualTo(7);
    }

    @Test
    void laClassificaDelTorneoSommaLeSessioniIncluse() throws Exception {
        ingest(fact("session-001", "2", true, "0"));
        ingest(fact("session-002", "1", true, "4"));

        Tournament tournament = tournaments.create("Coppa del Locale", "DND", "venue-01");
        List<LeaderboardEntry> leaderboard = tournaments.leaderboard(tournament.tournamentId());

        assertThat(leaderboard).hasSize(2);
        // Bianca: (3+2+0) + (3+2+4) = 14. Andrea: (3+2+2) + (3+2+1) = 13.
        assertThat(leaderboard.get(0).displayName()).isEqualTo("Bianca");
        assertThat(leaderboard.get(0).points()).isEqualTo(14);
        assertThat(leaderboard.get(0).position()).isEqualTo(1);
        assertThat(leaderboard.get(1).displayName()).isEqualTo("Andrea");
        assertThat(leaderboard.get(1).points()).isEqualTo(13);
        assertThat(leaderboard.get(1).sessionsPlayed()).isEqualTo(2);
    }

    @Test
    void laClassificaRegistraIPersonaggiAbbattuti() throws Exception {
        ingest(fact("session-001", "0", false, "0"));

        Tournament tournament = tournaments.create("Coppa del Locale", "DND", "venue-01");
        LeaderboardEntry andrea = tournaments.leaderboard(tournament.tournamentId()).stream()
                .filter(entry -> entry.playerReference().equals("player-device-01"))
                .findFirst().orElseThrow();

        assertThat(andrea.timesDowned()).isEqualTo(1);
        assertThat(andrea.points()).isEqualTo(2);
    }

    @Test
    void leStatisticheDelGiocatoreAggreganoPiuSessioni() throws Exception {
        ingest(fact("session-001", "2", true, "0"));
        ingest(fact("session-002", "1", false, "0"));

        var statistiche = ingestion.findPlayer("player-device-01").orElseThrow();
        assertThat(statistiche.sessionsPlayed()).isEqualTo(2);
        assertThat(statistiche.sessionsSurvived()).isEqualTo(1);
        assertThat(statistiche.survivalRate()).isEqualTo(50.0);
        assertThat(statistiche.totalCellsTravelled()).isEqualTo(54);
    }

    @Test
    void unTorneoIncludeSoloLoStessoTipoDiGiocoEIlSuoLocale() throws Exception {
        ingest(fact("session-001", "0", true, "0"));

        Tournament altroLocale = tournaments.create("Coppa Altrove", "DND", "venue-99");

        assertThat(tournaments.leaderboard(altroLocale.tournamentId())).isEmpty();
    }
}
