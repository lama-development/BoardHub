package it.uniupo.boardhub.eventservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniupo.boardhub.eventservice.controller.dto.SessionEventNotification;
import it.uniupo.boardhub.eventservice.model.GameEvent;
import it.uniupo.boardhub.eventservice.mqtt.MqttBoardCommandPublisher;
import it.uniupo.boardhub.eventservice.repository.SessionPieceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionEventStreamService {

    private static final long STREAM_TIMEOUT_MILLIS = 30 * 60 * 1000L;

    private static final Set<String> PLAYER_FIELDS = Set.of(
            "commandId", "resolutionId", "sessionPieceId", "characterId", "from", "to",
            "requestedDestination", "expectedVersion", "resultingVersion", "cost", "path",
            "visibleTrapsOnPath", "success", "damage", "hpCurrent", "tacticalStatus",
            "movementDecision", "actorRole", "controlVersion"
    );

    private static final Set<String> PUBLIC_EVENT_TYPES = Set.of(
            "SESSION_START", "SESSION_END", "ROUND_END", "MOVE", "MOVE_CONFIRMED",
            "SPAWN_MONSTER", "ATTACK", "DAMAGE"
    );

    private static final Set<String> PUBLIC_FIELDS = Set.of(
            "characterId", "monsterId", "from", "to", "position", "cell", "at",
            "damage", "hpCurrent", "tacticalStatus"
    );

    private final SessionPieceRepository pieceRepository;
    private final MqttBoardCommandPublisher boardCommandPublisher;
    private final ConcurrentHashMap<String, Set<Subscriber>> subscribers = new ConcurrentHashMap<>();

    public SessionEventStreamService(SessionPieceRepository pieceRepository) {
        this(pieceRepository, null);
    }

    @Autowired
    public SessionEventStreamService(
            SessionPieceRepository pieceRepository,
            MqttBoardCommandPublisher boardCommandPublisher
    ) {
        this.pieceRepository = pieceRepository;
        this.boardCommandPublisher = boardCommandPublisher;
    }

    public SseEmitter subscribePlayer(String sessionId, UUID participantId) {
        return subscribe(sessionId, Audience.PLAYER, participantId);
    }

    public SseEmitter subscribeDm(String sessionId) {
        return subscribe(sessionId, Audience.DM, null);
    }

    public List<SessionEventNotification> projectPublic(List<GameEvent> events) {
        return events.stream()
                .map(this::projectPublic)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public List<SessionEventNotification> projectPlayer(
            List<GameEvent> events,
            UUID participantId
    ) {
        Subscriber player = new Subscriber(null, Audience.PLAYER, participantId);
        return events.stream()
                .map(event -> project(event, player))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public List<SessionEventNotification> projectDm(List<GameEvent> events) {
        return events.stream().map(this::toNotification).toList();
    }

    private SseEmitter subscribe(String sessionId, Audience audience, UUID participantId) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        Subscriber subscriber = new Subscriber(emitter, audience, participantId);
        Set<Subscriber> sessionSubscribers = subscribers.computeIfAbsent(
                sessionId, ignored -> ConcurrentHashMap.newKeySet()
        );
        sessionSubscribers.add(subscriber);
        Runnable remove = () -> remove(sessionId, subscriber);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(error -> remove.run());
        try {
            emitter.send(SseEmitter.event().name("connected").data(sessionId));
        } catch (IOException ex) {
            remove.run();
            emitter.completeWithError(ex);
        }
        return emitter;
    }

    public void publishAfterCommit(GameEvent event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish(event);
                }
            });
            return;
        }
        publish(event);
    }

    private void publish(GameEvent event) {
        if (boardCommandPublisher != null) {
            boardCommandPublisher.publishFor(event);
        }
        Set<Subscriber> sessionSubscribers = subscribers.get(event.sessionId());
        if (sessionSubscribers == null) {
            return;
        }
        for (Subscriber subscriber : Set.copyOf(sessionSubscribers)) {
            SessionEventNotification notification = project(event, subscriber);
            if (notification == null) {
                continue;
            }
            try {
                subscriber.emitter().send(SseEmitter.event()
                        .id(event.eventId())
                        .name(event.eventType())
                        .data(notification));
            } catch (IOException | IllegalStateException ex) {
                remove(event.sessionId(), subscriber);
                subscriber.emitter().complete();
            }
        }
    }

    private SessionEventNotification project(GameEvent event, Subscriber subscriber) {
        JsonNode payload = event.payload();
        if (subscriber.audience() == Audience.PLAYER) {
            String pieceId = payload.path("sessionPieceId").asText("");
            if (!pieceId.isBlank() && !ownsPiece(event.sessionId(), subscriber.participantId(), pieceId)) {
                return null;
            }
            String characterId = payload.path("characterId").asText("");
            if (pieceId.isBlank() && !characterId.isBlank()
                    && !ownsCharacter(event.sessionId(), subscriber.participantId(), characterId)) {
                return null;
            }
            ObjectNode filtered = JsonNodeFactory.instance.objectNode();
            JsonNode sourcePayload = payload;
            PLAYER_FIELDS.forEach(field -> {
                JsonNode value = sourcePayload.get(field);
                if (value != null) {
                    filtered.set(field, value);
                }
            });
            payload = filtered;
        }
        return new SessionEventNotification(
                event.eventId(), event.eventType(), event.sessionId(), event.occurredAt(),
                event.sequenceNumber(), payload
        );
    }

    private SessionEventNotification projectPublic(GameEvent event) {
        if (!PUBLIC_EVENT_TYPES.contains(event.eventType())) {
            return null;
        }
        ObjectNode filtered = JsonNodeFactory.instance.objectNode();
        PUBLIC_FIELDS.forEach(field -> {
            JsonNode value = event.payload().get(field);
            if (value != null) {
                filtered.set(field, value);
            }
        });
        return new SessionEventNotification(
                event.eventId(), event.eventType(), event.sessionId(), event.occurredAt(),
                event.sequenceNumber(), filtered
        );
    }

    private SessionEventNotification toNotification(GameEvent event) {
        return new SessionEventNotification(
                event.eventId(), event.eventType(), event.sessionId(), event.occurredAt(),
                event.sequenceNumber(), event.payload()
        );
    }

    private boolean ownsPiece(String sessionId, UUID participantId, String pieceId) {
        try {
            UUID expected = UUID.fromString(pieceId);
            return pieceRepository.findByParticipant(sessionId, participantId).stream()
                    .anyMatch(piece -> piece.sessionPieceId().equals(expected));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private boolean ownsCharacter(String sessionId, UUID participantId, String characterId) {
        try {
            UUID expected = UUID.fromString(characterId);
            return pieceRepository.findByParticipant(sessionId, participantId).stream()
                    .anyMatch(piece -> piece.characterId().equals(expected));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private void remove(String sessionId, Subscriber subscriber) {
        subscribers.computeIfPresent(sessionId, (ignored, sessionSubscribers) -> {
            sessionSubscribers.remove(subscriber);
            return sessionSubscribers.isEmpty() ? null : sessionSubscribers;
        });
    }

    private enum Audience { PLAYER, DM }
    private record Subscriber(SseEmitter emitter, Audience audience, UUID participantId) { }
}
