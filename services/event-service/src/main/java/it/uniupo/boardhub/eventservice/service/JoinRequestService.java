package it.uniupo.boardhub.eventservice.service;

import it.uniupo.boardhub.eventservice.config.JoinProperties;
import it.uniupo.boardhub.eventservice.model.join.JoinAcceptance;
import it.uniupo.boardhub.eventservice.model.join.JoinRequestStatus;
import it.uniupo.boardhub.eventservice.model.join.ParticipantRole;
import it.uniupo.boardhub.eventservice.model.join.ParticipantStatus;
import it.uniupo.boardhub.eventservice.model.join.PlayerJoinStatus;
import it.uniupo.boardhub.eventservice.model.join.SessionJoinRequest;
import it.uniupo.boardhub.eventservice.model.join.SessionParticipant;
import it.uniupo.boardhub.eventservice.model.session.GameSession;
import it.uniupo.boardhub.eventservice.model.session.GameSessionStatus;
import it.uniupo.boardhub.eventservice.repository.GameSessionRepository;
import it.uniupo.boardhub.eventservice.repository.GameTableRepository;
import it.uniupo.boardhub.eventservice.repository.JoinRequestRepository;
import it.uniupo.boardhub.eventservice.repository.SessionParticipantRepository;
import it.uniupo.boardhub.eventservice.service.command.CreateJoinRequestCommand;
import it.uniupo.boardhub.eventservice.service.exception.GameSessionNotFoundException;
import it.uniupo.boardhub.eventservice.service.exception.JoinRequestConflictException;
import it.uniupo.boardhub.eventservice.service.exception.JoinRequestNotFoundException;
import it.uniupo.boardhub.eventservice.service.exception.JoinRequestRateLimitException;
import it.uniupo.boardhub.eventservice.service.exception.SessionCapacityException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class JoinRequestService {

    private static final Pattern PLAYER_REFERENCE_PATTERN =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{2,99}");

    private final GameSessionRepository sessionRepository;
    private final GameTableRepository tableRepository;
    private final JoinRequestRepository requestRepository;
    private final SessionParticipantRepository participantRepository;
    private final JoinProperties properties;
    private final SessionTokenService tokenService;
    private final Clock clock;

    public JoinRequestService(
            GameSessionRepository sessionRepository,
            GameTableRepository tableRepository,
            JoinRequestRepository requestRepository,
            SessionParticipantRepository participantRepository,
            JoinProperties properties,
            SessionTokenService tokenService,
            Clock clock
    ) {
        this.sessionRepository = sessionRepository;
        this.tableRepository = tableRepository;
        this.requestRepository = requestRepository;
        this.participantRepository = participantRepository;
        this.properties = properties;
        this.tokenService = tokenService;
        this.clock = clock;
        validateConfiguration();
    }

    // Accoda una richiesta idempotente dopo aver verificato sessione, limiti e duplicati.
    @Transactional
    public SessionJoinRequest create(
            String sessionId,
            String idempotencyKey,
            CreateJoinRequestCommand request
    ) {
        UUID parsedKey = parseUuid(idempotencyKey, "Idempotency-Key");
        String normalizedSessionId = requireText(sessionId, "sessionId", 100);
        String playerReference = normalizePlayerReference(request);
        String displayName = normalizeDisplayName(request);
        String fingerprint = fingerprint(normalizedSessionId, playerReference, displayName);
        OffsetDateTime now = now();

        requestRepository.expireDueRequests(now);
        SessionJoinRequest previous = requestRepository.findByIdempotencyKey(parsedKey).orElse(null);
        if (previous != null) {
            return verifyIdempotentRetry(previous, fingerprint);
        }

        GameSession session = requireActiveSessionForUpdate(normalizedSessionId);
        if (!session.acceptingJoinRequests() || !tableRepository.isActiveSession(normalizedSessionId)) {
            throw new JoinRequestConflictException("La sessione non accetta nuove richieste di ingresso.");
        }
        if (participantRepository.findBySessionAndPlayerReference(normalizedSessionId, playerReference).isPresent()) {
            throw new JoinRequestConflictException("Il giocatore partecipa gia alla sessione.");
        }
        if (requestRepository.countRecentRequests(
                normalizedSessionId,
                playerReference,
                now.minusMinutes(1)
        ) >= properties.maxRequestsPerMinute()) {
            throw new JoinRequestRateLimitException();
        }

        SessionJoinRequest created = new SessionJoinRequest(
                UUID.randomUUID(),
                parsedKey,
                fingerprint,
                normalizedSessionId,
                playerReference,
                displayName,
                JoinRequestStatus.PENDING,
                now,
                now.plus(properties.requestTtl()),
                null
        );
        try {
            requestRepository.save(created);
            return created;
        } catch (DataIntegrityViolationException ex) {
            SessionJoinRequest concurrent = requestRepository.findByIdempotencyKey(parsedKey).orElse(null);
            if (concurrent != null) {
                return verifyIdempotentRetry(concurrent, fingerprint);
            }
            throw new JoinRequestConflictException("Esiste gia una richiesta pendente per questo giocatore.");
        }
    }

    // Elenca le richieste della sessione dopo aver aggiornato quelle scadute.
    @Transactional
    public List<SessionJoinRequest> list(String sessionId, JoinRequestStatus status) {
        String normalizedSessionId = requireText(sessionId, "sessionId", 100);
        requestRepository.expireDueRequests(now());
        sessionRepository.findSessionById(normalizedSessionId)
                .orElseThrow(() -> new GameSessionNotFoundException(normalizedSessionId));
        return requestRepository.findBySessionAndStatus(normalizedSessionId, status);
    }

    // Consente solo al dispositivo che possiede la chiave originaria di seguire la richiesta.
    @Transactional
    public PlayerJoinStatus getPlayerStatus(
            String sessionId,
            UUID requestId,
            String joinClaim
    ) {
        String normalizedSessionId = requireText(sessionId, "sessionId", 100);
        UUID parsedClaim = parseUuid(joinClaim, "X-BoardHub-Join-Claim");
        requestRepository.expireDueRequests(now());

        SessionJoinRequest request = requestRepository.findByIdempotencyKey(parsedClaim)
                .filter(candidate -> candidate.requestId().equals(requestId))
                .filter(candidate -> candidate.sessionId().equals(normalizedSessionId))
                .orElseThrow(() -> new JoinRequestNotFoundException(requestId));

        if (request.status() != JoinRequestStatus.ACCEPTED) {
            return new PlayerJoinStatus(request, null, null);
        }

        SessionParticipant participant = participantRepository.findByJoinRequestId(requestId)
                .orElseThrow(() -> new IllegalStateException(
                        "Richiesta accettata senza partecipante associato."
                ));
        return new PlayerJoinStatus(request, participant, tokenService.issuePlayer(participant));
    }

    // Accetta una sola volta la richiesta e restituisce lo stesso token negli eventuali retry.
    @Transactional
    public JoinAcceptance accept(String sessionId, UUID requestId) {
        String normalizedSessionId = requireText(sessionId, "sessionId", 100);
        OffsetDateTime now = now();
        requestRepository.expireDueRequests(now);
        requireActiveSessionForUpdate(normalizedSessionId);
        SessionJoinRequest request = requireRequestForSession(requestId, normalizedSessionId);

        if (request.status() == JoinRequestStatus.ACCEPTED) {
            SessionParticipant existing = participantRepository.findByJoinRequestId(requestId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Richiesta accettata senza partecipante associato."
                    ));
            return new JoinAcceptance(request, existing, tokenService.issuePlayer(existing));
        }
        requirePending(request);
        if (participantRepository.countActivePlayers(normalizedSessionId) >= properties.maxPlayersPerSession()) {
            throw new SessionCapacityException("La sessione ha raggiunto il numero massimo di giocatori.");
        }

        SessionParticipant participant = new SessionParticipant(
                UUID.randomUUID(), request.requestId(), normalizedSessionId,
                request.playerReference(), ParticipantRole.PLAYER,
                request.displayName(), ParticipantStatus.ACTIVE, now, null
        );
        try {
            participantRepository.save(participant);
        } catch (DataIntegrityViolationException ex) {
            throw new JoinRequestConflictException("Il giocatore partecipa gia alla sessione.");
        }
        if (!requestRepository.resolve(requestId, JoinRequestStatus.ACCEPTED, now)) {
            throw new JoinRequestConflictException("La richiesta non e piu in attesa.");
        }
        SessionJoinRequest accepted = resolved(request, JoinRequestStatus.ACCEPTED, now);
        return new JoinAcceptance(accepted, participant, tokenService.issuePlayer(participant));
    }

    // Rifiuta in modo idempotente una richiesta ancora in attesa.
    @Transactional
    public SessionJoinRequest reject(String sessionId, UUID requestId) {
        String normalizedSessionId = requireText(sessionId, "sessionId", 100);
        OffsetDateTime now = now();
        requestRepository.expireDueRequests(now);
        requireActiveSessionForUpdate(normalizedSessionId);
        SessionJoinRequest request = requireRequestForSession(requestId, normalizedSessionId);
        if (request.status() == JoinRequestStatus.REJECTED) {
            return request;
        }
        requirePending(request);
        if (!requestRepository.resolve(requestId, JoinRequestStatus.REJECTED, now)) {
            throw new JoinRequestConflictException("La richiesta non e piu in attesa.");
        }
        return resolved(request, JoinRequestStatus.REJECTED, now);
    }

    public List<SessionParticipant> listActiveParticipants(String sessionId) {
        String normalizedSessionId = requireText(sessionId, "sessionId", 100);
        sessionRepository.findSessionById(normalizedSessionId)
                .orElseThrow(() -> new GameSessionNotFoundException(normalizedSessionId));
        return participantRepository.findActiveBySessionId(normalizedSessionId);
    }

    private GameSession requireActiveSessionForUpdate(String sessionId) {
        GameSession session = sessionRepository.findSessionByIdForUpdate(sessionId)
                .orElseThrow(() -> new GameSessionNotFoundException(sessionId));
        if (session.status() != GameSessionStatus.ACTIVE) {
            throw new JoinRequestConflictException("La sessione non e attiva.");
        }
        return session;
    }

    private SessionJoinRequest requireRequestForSession(UUID requestId, String sessionId) {
        SessionJoinRequest request = requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new JoinRequestNotFoundException(requestId));
        if (!request.sessionId().equals(sessionId)) {
            throw new JoinRequestNotFoundException(requestId);
        }
        return request;
    }

    private void requirePending(SessionJoinRequest request) {
        if (request.status() != JoinRequestStatus.PENDING) {
            throw new JoinRequestConflictException(
                    "La richiesta e gia nello stato " + request.status().name() + "."
            );
        }
    }

    private SessionJoinRequest verifyIdempotentRetry(SessionJoinRequest previous, String fingerprint) {
        if (!MessageDigest.isEqual(
                previous.requestFingerprint().getBytes(StandardCharsets.US_ASCII),
                fingerprint.getBytes(StandardCharsets.US_ASCII)
        )) {
            throw new JoinRequestConflictException(
                    "La chiave di idempotenza e gia stata usata con dati differenti."
            );
        }
        return previous;
    }

    private SessionJoinRequest resolved(
            SessionJoinRequest request,
            JoinRequestStatus status,
            OffsetDateTime resolvedAt
    ) {
        return new SessionJoinRequest(
                request.requestId(), request.idempotencyKey(), request.requestFingerprint(),
                request.sessionId(), request.playerReference(), request.displayName(),
                status, request.requestedAt(), request.expiresAt(), resolvedAt
        );
    }

    private String normalizePlayerReference(CreateJoinRequestCommand request) {
        if (request == null || request.playerReference() == null) {
            throw new IllegalArgumentException("playerReference e obbligatorio.");
        }
        String value = request.playerReference().trim();
        if (!PLAYER_REFERENCE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "playerReference deve contenere da 3 a 100 caratteri alfanumerici, punto, trattino o underscore."
            );
        }
        return value;
    }

    private String normalizeDisplayName(CreateJoinRequestCommand request) {
        return requireText(request == null ? null : request.displayName(), "displayName", 80);
    }

    private String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " e obbligatorio.");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + " non puo superare " + maxLength + " caratteri.");
        }
        return normalized;
    }

    private UUID parseUuid(String value, String field) {
        try {
            return UUID.fromString(requireText(value, field, 36));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(field + " deve essere un UUID valido.");
        }
    }

    private String fingerprint(String sessionId, String playerReference, String displayName) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(
                    (sessionId + "\n" + playerReference + "\n" + displayName)
                            .getBytes(StandardCharsets.UTF_8)
            );
            return HexFormat.of().formatHex(bytes);
        } catch (Exception ex) {
            throw new IllegalStateException("Impossibile calcolare l'impronta della richiesta.", ex);
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
    }

    private void validateConfiguration() {
        if (properties.requestTtl() == null || properties.requestTtl().isZero()
                || properties.requestTtl().isNegative()) {
            throw new IllegalStateException("boardhub.join.request-ttl deve essere positivo.");
        }
        if (properties.maxPlayersPerSession() < 1 || properties.maxActiveTables() < 1
                || properties.maxRequestsPerMinute() < 1) {
            throw new IllegalStateException("I limiti boardhub.join devono essere maggiori di zero.");
        }
    }
}
