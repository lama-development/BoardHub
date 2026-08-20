package it.uniupo.boardhub.eventservice.repository;

import it.uniupo.boardhub.eventservice.model.result.SessionResultOutboxEntry;
import it.uniupo.boardhub.eventservice.model.session.GameSession;
import it.uniupo.boardhub.eventservice.model.session.GameSessionStatus;
import it.uniupo.boardhub.eventservice.support.MigratedTestDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SessionResultOutboxRepositoryTest {

    @Test
    void conservaIlRisultatoFincheIlBrokerNonLoConferma() {
        JdbcTemplate jdbcTemplate = MigratedTestDatabase.create();
        GameSessionRepository sessions = new GameSessionRepository(jdbcTemplate);
        SessionResultOutboxRepository outbox = new SessionResultOutboxRepository(jdbcTemplate);
        OffsetDateTime now = OffsetDateTime.parse("2026-08-20T10:00:00Z");
        sessions.saveSession(new GameSession(
                "session-result-001", "venue-01", "table-01", "Cripta", "DND",
                "Sessione di prova.", true, GameSessionStatus.ACTIVE, 3, 3, now
        ));

        outbox.enqueue(
                "result-session-result-001",
                "session-result-001",
                "boardhub/v1/venues/venue-01/session-results",
                "{\"factId\":\"result-session-result-001\"}",
                now
        );

        SessionResultOutboxEntry due = outbox.findDue(now, 10).get(0);
        assertThat(due.attemptCount()).isZero();

        outbox.scheduleRetry(due.factId(), 1, now.plusSeconds(2), "broker assente");
        assertThat(outbox.findDue(now.plusSeconds(1), 10)).isEmpty();
        assertThat(outbox.findDue(now.plusSeconds(2), 10)).hasSize(1);

        assertThat(outbox.markPublished(due.factId(), now.plusSeconds(2))).isTrue();
        assertThat(outbox.findDue(now.plusMinutes(1), 10)).isEmpty();
    }
}
