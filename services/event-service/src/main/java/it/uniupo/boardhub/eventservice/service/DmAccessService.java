package it.uniupo.boardhub.eventservice.service;

import it.uniupo.boardhub.eventservice.model.join.DmAccessGrant;
import it.uniupo.boardhub.eventservice.model.join.ParticipantRole;
import it.uniupo.boardhub.eventservice.model.join.ParticipantStatus;
import it.uniupo.boardhub.eventservice.model.join.SessionParticipant;
import it.uniupo.boardhub.eventservice.model.session.GameSessionStatus;
import it.uniupo.boardhub.eventservice.repository.GameSessionRepository;
import it.uniupo.boardhub.eventservice.repository.SessionParticipantRepository;
import it.uniupo.boardhub.eventservice.service.exception.DmAuthenticationException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class DmAccessService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final SessionTokenService tokenService;
    private final SessionParticipantRepository participantRepository;
    private final GameSessionRepository sessionRepository;

    public DmAccessService(
            SessionTokenService tokenService,
            SessionParticipantRepository participantRepository,
            GameSessionRepository sessionRepository
    ) {
        this.tokenService = tokenService;
        this.participantRepository = participantRepository;
        this.sessionRepository = sessionRepository;
    }

    // Crea l'identita DM della sessione e restituisce il token firmato al dispositivo autore.
    public DmAccessGrant issue(String sessionId, OffsetDateTime joinedAt) {
        UUID participantId = UUID.randomUUID();
        SessionParticipant participant = new SessionParticipant(
                participantId,
                null,
                sessionId,
                "dm-device-" + participantId,
                ParticipantRole.DM,
                "Dungeon Master",
                ParticipantStatus.ACTIVE,
                joinedAt,
                null
        );
        participantRepository.save(participant);
        return new DmAccessGrant(participant, tokenService.issueDm(participant));
    }

    // Accetta soltanto il token del DM attivo appartenente alla sessione richiesta.
    public SessionParticipant requireAuthorized(String sessionId, String authorizationHeader) {
        UUID participantId;
        try {
            participantId = tokenService.verifyDm(
                    extractBearerToken(authorizationHeader),
                    sessionId
            );
        } catch (RuntimeException ex) {
            throw new DmAuthenticationException();
        }

        var session = sessionRepository.findSessionById(sessionId)
                .orElseThrow(DmAuthenticationException::new);
        SessionParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(DmAuthenticationException::new);
        if (!participant.sessionId().equals(sessionId)
                || participant.role() != ParticipantRole.DM
                || participant.status() != ParticipantStatus.ACTIVE
                || session.status() != GameSessionStatus.ACTIVE) {
            throw new DmAuthenticationException();
        }
        return participant;
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null
                || !authorizationHeader.regionMatches(
                        true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length()
                )) {
            throw new DmAuthenticationException();
        }
        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new DmAuthenticationException();
        }
        return token;
    }
}
