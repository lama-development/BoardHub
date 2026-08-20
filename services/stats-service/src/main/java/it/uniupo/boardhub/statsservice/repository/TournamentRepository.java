package it.uniupo.boardhub.statsservice.repository;

import it.uniupo.boardhub.statsservice.model.LeaderboardEntry;
import it.uniupo.boardhub.statsservice.model.Tournament;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TournamentRepository {

    private final JdbcTemplate jdbcTemplate;

    public TournamentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(Tournament tournament) {
        jdbcTemplate.update("""
                INSERT INTO stats_schema.tournaments (tournament_id, name, game_type, venue_id, created_at)
                VALUES (?, ?, ?, ?, ?)
                """,
                tournament.tournamentId(), tournament.name(), tournament.gameType(),
                tournament.venueId(), Timestamp.from(tournament.createdAt().toInstant())
        );
    }

    public void addSession(UUID tournamentId, String sessionId, OffsetDateTime addedAt) {
        jdbcTemplate.update("""
                INSERT INTO stats_schema.tournament_sessions (tournament_id, session_id, added_at)
                VALUES (?, ?, ?)
                """,
                tournamentId, sessionId, Timestamp.from(addedAt.toInstant())
        );
    }

    public Optional<Tournament> findById(UUID tournamentId) {
        return jdbcTemplate.query(
                "SELECT * FROM stats_schema.tournaments WHERE tournament_id = ?",
                (rs, rowNum) -> new Tournament(
                        UUID.fromString(rs.getString("tournament_id")),
                        rs.getString("name"),
                        rs.getString("game_type"),
                        rs.getString("venue_id"),
                        rs.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC)
                ),
                tournamentId
        ).stream().findFirst();
    }

    public List<Tournament> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM stats_schema.tournaments ORDER BY created_at DESC",
                (rs, rowNum) -> new Tournament(
                        UUID.fromString(rs.getString("tournament_id")),
                        rs.getString("name"),
                        rs.getString("game_type"),
                        rs.getString("venue_id"),
                        rs.getTimestamp("created_at").toInstant().atOffset(ZoneOffset.UTC)
                )
        );
    }

    // Somma sul database punteggi gia fissati all'ingestione delle singole partite.
    public List<LeaderboardEntry> leaderboard(UUID tournamentId) {
        List<LeaderboardEntry> rows = jdbcTemplate.query("""
                SELECT
                    p.player_reference,
                    MAX(p.display_name) AS display_name,
                    COUNT(*) AS sessions_played,
                    SUM(p.points) AS points,
                    SUM(p.saves_succeeded) AS saves_succeeded,
                    SUM(p.cells_travelled) AS cells_travelled,
                    SUM(CASE WHEN p.survived THEN 0 ELSE 1 END) AS times_downed
                FROM stats_schema.player_results p
                JOIN stats_schema.tournament_sessions ts ON ts.session_id = p.session_id
                WHERE ts.tournament_id = ?
                GROUP BY p.player_reference
                ORDER BY points DESC, saves_succeeded DESC, cells_travelled DESC, display_name ASC
                """,
                (rs, rowNum) -> new LeaderboardEntry(
                        0,
                        rs.getString("player_reference"),
                        rs.getString("display_name"),
                        rs.getInt("sessions_played"),
                        rs.getInt("points"),
                        rs.getInt("saves_succeeded"),
                        rs.getInt("cells_travelled"),
                        rs.getInt("times_downed")
                ),
                tournamentId
        );

        // La posizione dipende dalle sessioni incluse, quindi si calcola e non si salva.
        List<LeaderboardEntry> ranked = new java.util.ArrayList<>(rows.size());
        for (int index = 0; index < rows.size(); index++) {
            LeaderboardEntry row = rows.get(index);
            ranked.add(new LeaderboardEntry(
                    index + 1, row.playerReference(), row.displayName(),
                    row.sessionsPlayed(), row.points(), row.savesSucceeded(),
                    row.cellsTravelled(), row.timesDowned()
            ));
        }
        return List.copyOf(ranked);
    }
}
