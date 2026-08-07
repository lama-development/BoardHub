package it.uniupo.boardhub.eventservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniupo.boardhub.eventservice.model.GameEvent;
import it.uniupo.boardhub.eventservice.model.character.PlayerCharacter;
import it.uniupo.boardhub.eventservice.model.grid.GameGrid;
import it.uniupo.boardhub.eventservice.model.grid.GridPosition;
import it.uniupo.boardhub.eventservice.model.grid.GridTrap;
import it.uniupo.boardhub.eventservice.model.grid.MovementRequest;
import it.uniupo.boardhub.eventservice.model.grid.ReachableCell;
import it.uniupo.boardhub.eventservice.model.join.SessionParticipant;
import it.uniupo.boardhub.eventservice.model.piece.PieceMoveResult;
import it.uniupo.boardhub.eventservice.model.piece.PieceReachability;
import it.uniupo.boardhub.eventservice.model.piece.SessionPiece;
import it.uniupo.boardhub.eventservice.model.session.GameSession;
import it.uniupo.boardhub.eventservice.repository.CharacterRepository;
import it.uniupo.boardhub.eventservice.repository.GameEventRepository;
import it.uniupo.boardhub.eventservice.repository.GameSessionRepository;
import it.uniupo.boardhub.eventservice.repository.SessionPieceRepository;
import it.uniupo.boardhub.eventservice.service.command.MoveSessionPieceCommand;
import it.uniupo.boardhub.eventservice.service.exception.CharacterNotFoundException;
import it.uniupo.boardhub.eventservice.service.exception.MoveCommandConflictException;
import it.uniupo.boardhub.eventservice.service.exception.MoveRejectedException;
import it.uniupo.boardhub.eventservice.service.exception.SessionPieceNotFoundException;
import it.uniupo.boardhub.eventservice.service.exception.StalePieceStateException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SessionPieceMovementService {

    private static final int MAX_SESSION_ID_LENGTH = 100;
    private static final String EVENT_TYPE = "MOVE_CONFIRMED";
    private static final String EVENT_SOURCE = "BACKEND";
    private static final String EVENT_PREFIX = "move-";

    private final ParticipantAccessService participantAccessService;
    private final SessionPieceRepository pieceRepository;
    private final CharacterRepository characterRepository;
    private final GameSessionRepository sessionRepository;
    private final GameEventRepository eventRepository;
    private final SessionGridService gridService;
    private final MovementService movementService;
    private final Clock clock;

    public SessionPieceMovementService(
            ParticipantAccessService participantAccessService,
            SessionPieceRepository pieceRepository,
            CharacterRepository characterRepository,
            GameSessionRepository sessionRepository,
            GameEventRepository eventRepository,
            SessionGridService gridService,
            MovementService movementService,
            Clock clock
    ) {
        this.participantAccessService = participantAccessService;
        this.pieceRepository = pieceRepository;
        this.characterRepository = characterRepository;
        this.sessionRepository = sessionRepository;
        this.eventRepository = eventRepository;
        this.gridService = gridService;
        this.movementService = movementService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PieceReachability reachableCells(
            String sessionId,
            UUID sessionPieceId,
            String authorizationHeader
    ) {
        String normalizedSessionId = requireSessionId(sessionId);
        SessionParticipant participant = participantAccessService.requireActiveParticipant(
                normalizedSessionId,
                authorizationHeader
        );
        SessionPiece piece = requireOwnedPiece(
                normalizedSessionId,
                sessionPieceId,
                participant,
                false
        );
        PlayerCharacter character = requireCharacter(piece);
        List<ReachableCell> reachable = calculateReachable(piece, character);
        return new PieceReachability(
                piece.sessionPieceId(),
                piece.currentCell(),
                character.speedCells(),
                piece.version(),
                reachable
        );
    }

    @Transactional
    public PieceMoveResult move(
            String sessionId,
            UUID sessionPieceId,
            String authorizationHeader,
            MoveSessionPieceCommand command
    ) {
        String normalizedSessionId = requireSessionId(sessionId);
        MoveData data = validate(command);
        SessionParticipant participant = participantAccessService.requireActiveParticipantForUpdate(
                normalizedSessionId,
                authorizationHeader
        );
        SessionPiece piece = requireOwnedPiece(
                normalizedSessionId,
                sessionPieceId,
                participant,
                true
        );
        PlayerCharacter character = requireCharacter(piece);
        String eventId = EVENT_PREFIX + data.commandId();

        var existing = eventRepository.findByEventId(eventId);
        if (existing.isPresent()) {
            return replay(existing.get(), normalizedSessionId, piece, data);
        }
        if (piece.version() != data.expectedVersion()) {
            throw new StalePieceStateException(piece.version());
        }
        if (piece.currentCell().equals(data.destination())) {
            throw new MoveRejectedException(
                    "La destinazione coincide con la posizione corrente della pedina."
            );
        }

        ReachableCell destination = calculateReachable(piece, character).stream()
                .filter(cell -> cell.position().toCell().equals(data.destination()))
                .findFirst()
                .orElseThrow(() -> new MoveRejectedException(
                        "La cella " + data.destination()
                                + " non e raggiungibile con i punti movimento disponibili."
                ));

        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        try {
            if (!pieceRepository.move(
                    piece.sessionPieceId(),
                    normalizedSessionId,
                    data.destination(),
                    data.expectedVersion(),
                    now
            )) {
                throw new StalePieceStateException(piece.version());
            }
        } catch (DuplicateKeyException ex) {
            throw new MoveRejectedException(
                    "La cella " + data.destination() + " e gia occupata."
            );
        }

        long resultingVersion = piece.version() + 1;
        List<String> path = destination.path().stream().map(GridPosition::toCell).toList();
        List<String> visibleTraps = destination.trapsOnPath().stream()
                .filter(GridTrap::isVisibleToPlayers)
                .map(GridTrap::trapId)
                .toList();
        ObjectNode payload = createPayload(
                data.commandId(),
                data.expectedVersion(),
                piece,
                data.destination(),
                path,
                destination.cost(),
                resultingVersion,
                visibleTraps
        );
        GameSession session = sessionRepository.findSessionById(normalizedSessionId)
                .orElseThrow(() -> new IllegalStateException(
                        "Sessione scomparsa durante lo spostamento."
                ));
        GameEvent event = new GameEvent(
                eventId,
                EVENT_TYPE,
                session.venueId(),
                session.tableId(),
                normalizedSessionId,
                EVENT_SOURCE,
                now.toString(),
                sessionRepository.nextServerEventSequence(normalizedSessionId),
                payload
        );
        try {
            if (!eventRepository.save(event)) {
                throw new MoveCommandConflictException();
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Impossibile serializzare l'evento di movimento.", ex);
        }

        return new PieceMoveResult(
                data.commandId(),
                eventId,
                piece.sessionPieceId(),
                piece.characterId(),
                piece.currentCell(),
                data.destination(),
                path,
                destination.cost(),
                resultingVersion,
                visibleTraps
        );
    }

    private SessionPiece requireOwnedPiece(
            String sessionId,
            UUID sessionPieceId,
            SessionParticipant participant,
            boolean forUpdate
    ) {
        if (sessionPieceId == null) {
            throw new IllegalArgumentException("sessionPieceId e obbligatorio.");
        }
        SessionPiece piece = forUpdate
                ? pieceRepository.findByIdAndSessionForUpdate(sessionPieceId, sessionId)
                        .orElseThrow(SessionPieceNotFoundException::new)
                : pieceRepository.findByParticipant(sessionId, participant.participantId()).stream()
                        .filter(item -> item.sessionPieceId().equals(sessionPieceId))
                        .findFirst()
                        .orElseThrow(SessionPieceNotFoundException::new);
        if (!piece.participantId().equals(participant.participantId())) {
            throw new SessionPieceNotFoundException();
        }
        return piece;
    }

    private PlayerCharacter requireCharacter(SessionPiece piece) {
        return characterRepository.findByIdAndSession(piece.characterId(), piece.sessionId())
                .filter(character -> character.participantId().equals(piece.participantId()))
                .orElseThrow(CharacterNotFoundException::new);
    }

    private List<ReachableCell> calculateReachable(
            SessionPiece piece,
            PlayerCharacter character
    ) {
        GameGrid grid = gridService.loadGrid(piece.sessionId());
        return movementService.calculateReachableCells(
                grid,
                new MovementRequest(
                        character.characterId().toString(),
                        GridPosition.fromCell(piece.currentCell()),
                        character.speedCells()
                )
        );
    }

    private ObjectNode createPayload(
            UUID commandId,
            long expectedVersion,
            SessionPiece piece,
            String destination,
            List<String> path,
            int cost,
            long resultingVersion,
            List<String> visibleTraps
    ) {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("commandId", commandId.toString());
        payload.put("expectedVersion", expectedVersion);
        payload.put("sessionPieceId", piece.sessionPieceId().toString());
        payload.put("characterId", piece.characterId().toString());
        payload.put("from", piece.currentCell());
        payload.put("to", destination);
        payload.put("cost", cost);
        payload.put("resultingVersion", resultingVersion);
        addStrings(payload.putArray("path"), path);
        addStrings(payload.putArray("visibleTrapsOnPath"), visibleTraps);
        return payload;
    }

    private void addStrings(ArrayNode array, List<String> values) {
        values.forEach(array::add);
    }

    private PieceMoveResult replay(
            GameEvent event,
            String sessionId,
            SessionPiece piece,
            MoveData data
    ) {
        JsonNode payload = event.payload();
        if (!EVENT_TYPE.equals(event.eventType())
                || !EVENT_SOURCE.equals(event.source())
                || !sessionId.equals(event.sessionId())
                || !piece.sessionPieceId().toString().equals(text(payload, "sessionPieceId"))
                || !piece.characterId().toString().equals(text(payload, "characterId"))
                || !data.commandId().toString().equals(text(payload, "commandId"))
                || !data.destination().equals(text(payload, "to"))
                || data.expectedVersion() != longValue(payload, "expectedVersion")) {
            throw new MoveCommandConflictException();
        }
        return new PieceMoveResult(
                data.commandId(),
                event.eventId(),
                piece.sessionPieceId(),
                piece.characterId(),
                text(payload, "from"),
                text(payload, "to"),
                stringList(payload, "path"),
                integer(payload, "cost"),
                longValue(payload, "resultingVersion"),
                stringList(payload, "visibleTrapsOnPath")
        );
    }

    private String text(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new IllegalStateException("Evento di movimento incompleto: " + field);
        }
        return value.asText();
    }

    private int integer(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        if (value == null || !value.canConvertToInt()) {
            throw new IllegalStateException("Evento di movimento incompleto: " + field);
        }
        return value.asInt();
    }

    private long longValue(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        if (value == null || !value.canConvertToLong()) {
            throw new IllegalStateException("Evento di movimento incompleto: " + field);
        }
        return value.asLong();
    }

    private List<String> stringList(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        if (value == null || !value.isArray()) {
            throw new IllegalStateException("Evento di movimento incompleto: " + field);
        }
        List<String> values = new ArrayList<>();
        value.forEach(item -> {
            if (!item.isTextual()) {
                throw new IllegalStateException("Evento di movimento non valido: " + field);
            }
            values.add(item.asText());
        });
        return List.copyOf(values);
    }

    private MoveData validate(MoveSessionPieceCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Il corpo della richiesta e obbligatorio.");
        }
        if (command.destination() == null || command.destination().isBlank()) {
            throw new IllegalArgumentException("destination e obbligatorio.");
        }
        String destination = GridPosition.fromCell(command.destination().trim()).toCell();
        if (command.expectedVersion() == null || command.expectedVersion() < 0) {
            throw new IllegalArgumentException(
                    "expectedVersion deve essere maggiore o uguale a zero."
            );
        }
        if (command.commandId() == null) {
            throw new IllegalArgumentException("commandId e obbligatorio.");
        }
        return new MoveData(destination, command.expectedVersion(), command.commandId());
    }

    private String requireSessionId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("sessionId e obbligatorio.");
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_SESSION_ID_LENGTH) {
            throw new IllegalArgumentException(
                    "sessionId non puo superare " + MAX_SESSION_ID_LENGTH + " caratteri."
            );
        }
        return normalized;
    }

    private record MoveData(String destination, long expectedVersion, UUID commandId) {
    }
}
