package it.uniupo.boardhub.eventservice.service;

import it.uniupo.boardhub.eventservice.model.join.ParticipantStatus;
import it.uniupo.boardhub.eventservice.model.join.ParticipantRole;
import it.uniupo.boardhub.eventservice.model.join.SessionParticipant;
import it.uniupo.boardhub.eventservice.model.session.GameSession;
import it.uniupo.boardhub.eventservice.model.session.GameSessionStatus;
import it.uniupo.boardhub.eventservice.repository.GameSessionRepository;
import it.uniupo.boardhub.eventservice.repository.SessionParticipantRepository;
import it.uniupo.boardhub.eventservice.service.exception.PlayerAuthenticationException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ParticipantAccessService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final SessionTokenService tokenService;
    private final SessionParticipantRepository participantRepository;
    private final GameSessionRepository sessionRepository;

    public ParticipantAccessService(
            SessionTokenService tokenService,
            SessionParticipantRepository participantRepository,
            GameSessionRepository sessionRepository
    ) {
        this.tokenService = tokenService;
        this.participantRepository = participantRepository;
        this.sessionRepository = sessionRepository;
    }

    // Autentica la richiesta e revoca implicitamente l'accesso quando sessione o partecipante non sono attivi.
    public SessionParticipant requireActiveParticipant(
            String sessionId,
            String authorizationHeader
    ) {
        String token = extractBearerToken(authorizationHeader);
        UUID participantId = tokenService.verifyPlayer(token, sessionId);
        GameSession session = sessionRepository.findSessionById(sessionId)
                .orElseThrow(PlayerAuthenticationException::new);
        SessionParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(PlayerAuthenticationException::new);

        validateActive(sessionId, participant, session);
        return participant;
    }

    // Blocca sessione e partecipante nell'ordine condiviso dalle operazioni transazionali.
    public SessionParticipant requireActiveParticipantForUpdate(
            String sessionId,
            String authorizationHeader
    ) {
        String token = extractBearerToken(authorizationHeader);
        UUID participantId = tokenService.verifyPlayer(token, sessionId);
        GameSession session = sessionRepository.findSessionByIdForUpdate(sessionId)
                .orElseThrow(PlayerAuthenticationException::new);
        SessionParticipant participant = participantRepository.findByIdForUpdate(participantId)
                .orElseThrow(PlayerAuthenticationException::new);

        validateActive(sessionId, participant, session);
        return participant;
    }

    private void validateActive(
            String sessionId,
            SessionParticipant participant,
            GameSession session
    ) {
        if (!participant.sessionId().equals(sessionId)
                || participant.role() != ParticipantRole.PLAYER
                || participant.status() != ParticipantStatus.ACTIVE
                || session.status() != GameSessionStatus.ACTIVE) {
            throw new PlayerAuthenticationException();
        }
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null
                || !authorizationHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            throw new PlayerAuthenticationException();
        }
        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new PlayerAuthenticationException();
        }
        return token;
    }
}
