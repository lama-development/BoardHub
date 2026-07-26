package it.uniupo.boardhub.eventservice.service;

import it.uniupo.boardhub.eventservice.model.session.GameSession;
import it.uniupo.boardhub.eventservice.model.session.GameSessionStatus;
import it.uniupo.boardhub.eventservice.repository.GameSessionRepository;
import it.uniupo.boardhub.eventservice.repository.GameTableRepository;
import it.uniupo.boardhub.eventservice.repository.JoinRequestRepository;
import it.uniupo.boardhub.eventservice.repository.SessionParticipantRepository;
import it.uniupo.boardhub.eventservice.service.exception.GameSessionNotFoundException;
import it.uniupo.boardhub.eventservice.service.exception.JoinRequestConflictException;
import it.uniupo.boardhub.eventservice.service.exception.TableConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class SessionLifecycleService {

    private final GameSessionRepository sessionRepository;
    private final GameTableRepository tableRepository;
    private final JoinRequestRepository requestRepository;
    private final SessionParticipantRepository participantRepository;
    private final Clock clock;

    public SessionLifecycleService(
            GameSessionRepository sessionRepository,
            GameTableRepository tableRepository,
            JoinRequestRepository requestRepository,
            SessionParticipantRepository participantRepository,
            Clock clock
    ) {
        this.sessionRepository = sessionRepository;
        this.tableRepository = tableRepository;
        this.requestRepository = requestRepository;
        this.participantRepository = participantRepository;
        this.clock = clock;
    }

    // Conclude in modo atomico sessione, ingressi e partecipazioni, poi libera il tavolo.
    @Transactional
    public OffsetDateTime close(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId e obbligatorio.");
        }
        GameSession session = sessionRepository.findSessionByIdForUpdate(sessionId)
                .orElseThrow(() -> new GameSessionNotFoundException(sessionId));
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        if (session.status() == GameSessionStatus.ENDED) {
            return now;
        }
        if (session.status() != GameSessionStatus.ACTIVE) {
            throw new JoinRequestConflictException("La sessione non puo essere conclusa nello stato corrente.");
        }

        sessionRepository.endSession(sessionId);
        requestRepository.expirePendingForSession(sessionId, now);
        participantRepository.closeActiveParticipants(sessionId, now);
        if (!tableRepository.releaseSession(sessionId, Timestamp.from(now.toInstant()))) {
            throw new TableConflictException("Il tavolo associato alla sessione non e stato trovato.");
        }
        return now;
    }
}
