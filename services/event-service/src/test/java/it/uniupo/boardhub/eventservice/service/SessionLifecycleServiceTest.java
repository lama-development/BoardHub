package it.uniupo.boardhub.eventservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.boardhub.eventservice.model.session.GameSession;
import it.uniupo.boardhub.eventservice.model.session.GameSessionStatus;
import it.uniupo.boardhub.eventservice.repository.GameSessionRepository;
import it.uniupo.boardhub.eventservice.repository.GameTableRepository;
import it.uniupo.boardhub.eventservice.repository.JoinRequestRepository;
import it.uniupo.boardhub.eventservice.repository.SessionParticipantRepository;
import it.uniupo.boardhub.eventservice.model.result.SessionResult;
import it.uniupo.boardhub.eventservice.mqtt.MqttSessionResultPublisher;
import it.uniupo.boardhub.eventservice.repository.TrapResolutionRepository;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class SessionLifecycleServiceTest {

    @Test
    void chiudereLaSessioneCancellaLeRisoluzioniPendenti() {
        String sessionId = "session-close-001";
        OffsetDateTime now = OffsetDateTime.parse("2026-08-08T10:00:00Z");
        AtomicBoolean ended = new AtomicBoolean();
        AtomicBoolean trapsCancelled = new AtomicBoolean();
        AtomicBoolean resultPublished = new AtomicBoolean();
        GameSession session = new GameSession(
                sessionId, "venue-01", "table-01", "Cripta", "DND", null,
                true, GameSessionStatus.ACTIVE, 5, 5, now
        );
        GameSessionRepository sessions = new GameSessionRepository(null) {
            @Override
            public Optional<GameSession> findSessionByIdForUpdate(String requestedId) {
                return Optional.of(session);
            }

            @Override
            public boolean endSession(String requestedId) {
                ended.set(true);
                return true;
            }
        };
        TrapResolutionRepository traps = new TrapResolutionRepository(null, new ObjectMapper()) {
            @Override
            public int cancelPendingForSession(String requestedId, OffsetDateTime completedAt) {
                trapsCancelled.set(true);
                return 1;
            }
        };
        SessionLifecycleService service = new SessionLifecycleService(
                sessions,
                new GameTableRepository(null) {
                    @Override
                    public boolean releaseSession(String requestedId, Timestamp updatedAt) {
                        return true;
                    }
                },
                new JoinRequestRepository(null) {
                    @Override
                    public int expirePendingForSession(String requestedId, OffsetDateTime completedAt) {
                        return 0;
                    }
                },
                new SessionParticipantRepository(null) {
                    @Override
                    public int closeActiveParticipants(String requestedId, OffsetDateTime leftAt) {
                        return 0;
                    }
                },
                traps,
                new SessionResultService(null, null, null, null) {
                    @Override
                    public SessionResult build(GameSession closing, OffsetDateTime endedAt) {
                        return new SessionResult(
                                closing.sessionId(), closing.venueId(), closing.tableId(),
                                closing.title(), closing.gameType(), closing.createdAt(),
                                endedAt, 0, java.util.List.of()
                        );
                    }
                },
                new MqttSessionResultPublisher(null, new ObjectMapper()) {
                    @Override
                    public void publishAfterCommit(SessionResult result) {
                        resultPublished.set(true);
                    }
                },
                Clock.fixed(Instant.parse("2026-08-08T10:00:00Z"), ZoneOffset.UTC)
        );

        service.close(sessionId);

        assertThat(ended.get()).isTrue();
        assertThat(trapsCancelled.get()).isTrue();
        assertThat(resultPublished.get()).isTrue();
    }
}
