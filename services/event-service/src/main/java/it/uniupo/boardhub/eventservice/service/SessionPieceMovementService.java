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
import it.uniupo.boardhub.eventservice.repository.CharacterControlRepository;
import it.uniupo.boardhub.eventservice.repository.GameEventRepository;
import it.uniupo.boardhub.eventservice.repository.GameSessionRepository;
import it.uniupo.boardhub.eventservice.repository.SessionPieceRepository;
import it.uniupo.boardhub.eventservice.repository.TrapRepository;
import it.uniupo.boardhub.eventservice.repository.TrapResolutionRepository;
import it.uniupo.boardhub.eventservice.service.command.MoveSessionPieceCommand;
import it.uniupo.boardhub.eventservice.service.exception.CharacterNotFoundException;
import it.uniupo.boardhub.eventservice.service.exception.MoveCommandConflictException;
import it.uniupo.boardhub.eventservice.service.exception.MoveRejectedException;
import it.uniupo.boardhub.eventservice.service.exception.SessionPieceNotFoundException;
import it.uniupo.boardhub.eventservice.service.exception.StalePieceStateException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import it.uniupo.boardhub.eventservice.model.trap.TrapDefinition;
import it.uniupo.boardhub.eventservice.model.trap.TrapResolution;
import it.uniupo.boardhub.eventservice.model.grid.TrapVisibility;

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
    private final TrapRepository trapRepository;
    private final TrapResolutionRepository resolutionRepository;
    private final CharacterControlRepository controlRepository;
    private final SessionEventStreamService eventStreamService;
    private final Clock clock;

    @Autowired
    public SessionPieceMovementService(
            ParticipantAccessService participantAccessService,
            SessionPieceRepository pieceRepository,
            CharacterRepository characterRepository,
            GameSessionRepository sessionRepository,
            GameEventRepository eventRepository,
            SessionGridService gridService,
            MovementService movementService,
            TrapRepository trapRepository,
            TrapResolutionRepository resolutionRepository,
            CharacterControlRepository controlRepository,
            SessionEventStreamService eventStreamService,
            Clock clock
    ) {
        this.participantAccessService = participantAccessService;
        this.pieceRepository = pieceRepository;
        this.characterRepository = characterRepository;
        this.sessionRepository = sessionRepository;
        this.eventRepository = eventRepository;
        this.gridService = gridService;
        this.movementService = movementService;
        this.trapRepository = trapRepository;
        this.resolutionRepository = resolutionRepository;
        this.controlRepository = controlRepository;
        this.eventStreamService = eventStreamService;
        this.clock = clock;
    }

    protected SessionPieceMovementService(
            ParticipantAccessService participantAccessService,
            SessionPieceRepository pieceRepository,
            CharacterRepository characterRepository,
            GameSessionRepository sessionRepository,
            GameEventRepository eventRepository,
            SessionGridService gridService,
            MovementService movementService,
            Clock clock
    ) {
        this(
                participantAccessService, pieceRepository, characterRepository, sessionRepository,
                eventRepository, gridService, movementService, null, null, null, null, clock
        );
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
        requirePlayerControlAvailable(piece);
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
        requirePlayerControlAvailable(piece);
        return executeMove(
                normalizedSessionId,
                piece,
                character,
                data,
                new Actor("PLAYER", participant.participantId())
        );
    }

    @Transactional(readOnly = true)
    public PieceReachability reachableCellsAsDm(
            String sessionId,
            UUID sessionPieceId,
            UUID dmParticipantId
    ) {
        String normalizedSessionId = requireSessionId(sessionId);
        SessionPiece piece = requireControlledPiece(
                normalizedSessionId, sessionPieceId, dmParticipantId, false
        );
        PlayerCharacter character = requireCharacter(piece);
        List<ReachableCell> reachable = calculateReachable(piece, character);
        return new PieceReachability(
                piece.sessionPieceId(), piece.currentCell(), character.speedCells(),
                piece.version(), reachable
        );
    }

    @Transactional
    public PieceMoveResult moveAsDm(
            String sessionId,
            UUID sessionPieceId,
            UUID dmParticipantId,
            MoveSessionPieceCommand command
    ) {
        String normalizedSessionId = requireSessionId(sessionId);
        MoveData data = validate(command);
        SessionPiece piece = requireControlledPiece(
                normalizedSessionId, sessionPieceId, dmParticipantId, true
        );
        PlayerCharacter character = requireCharacter(piece);
        return executeMove(
                normalizedSessionId,
                piece,
                character,
                data,
                new Actor("DM", dmParticipantId)
        );
    }

    private PieceMoveResult executeMove(
            String normalizedSessionId,
            SessionPiece piece,
            PlayerCharacter character,
            MoveData data,
            Actor actor
    ) {
        String eventId = EVENT_PREFIX + data.commandId();

        TrapResolution previousResolution = resolutionRepository == null
                ? null
                : resolutionRepository.findByMoveCommand(data.commandId()).orElse(null);
        if (previousResolution != null) {
            return replayPending(previousResolution, normalizedSessionId, piece, data);
        }

        var existing = eventRepository.findByEventId(eventId);
        if (existing.isPresent()) {
            return replay(existing.get(), normalizedSessionId, piece, data);
        }
        if (piece.version() != data.expectedVersion()) {
            throw new StalePieceStateException(piece.version());
        }
        if (resolutionRepository != null
                && resolutionRepository.findPendingByPiece(normalizedSessionId, piece.sessionPieceId()).isPresent()) {
            throw new MoveRejectedException(
                    "La pedina ha gia una trappola in attesa di risoluzione."
            );
        }
        if (piece.currentCell().equals(data.destination())) {
            throw new MoveRejectedException(
                    "La destinazione coincide con la posizione corrente della pedina."
            );
        }

        List<ReachableCell> reachableCells = calculateReachable(piece, character);
        ReachableCell destination = reachableCells.stream()
                .filter(cell -> cell.position().toCell().equals(data.destination()))
                .findFirst()
                .orElseThrow(() -> new MoveRejectedException(
                        "La cella " + data.destination()
                                + " non e raggiungibile con i punti movimento disponibili."
                ));

        TrapEncounter encounter = firstTrigger(normalizedSessionId, destination, reachableCells);
        if (encounter != null) {
            return triggerTrap(
                    normalizedSessionId, piece, character, data, destination, encounter, actor
            );
        }

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
                visibleTraps,
                actor
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
            publishAfterCommit(event);
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

    private SessionPiece requireControlledPiece(
            String sessionId,
            UUID sessionPieceId,
            UUID dmParticipantId,
            boolean forUpdate
    ) {
        if (sessionPieceId == null || dmParticipantId == null || controlRepository == null) {
            throw new IllegalArgumentException("Pedina e Dungeon Master sono obbligatori.");
        }
        SessionPiece piece = forUpdate
                ? pieceRepository.findByIdAndSessionForUpdate(sessionPieceId, sessionId)
                        .orElseThrow(SessionPieceNotFoundException::new)
                : pieceRepository.findBySession(sessionId).stream()
                        .filter(item -> item.sessionPieceId().equals(sessionPieceId))
                        .findFirst()
                        .orElseThrow(SessionPieceNotFoundException::new);
        var control = (forUpdate
                ? controlRepository.findForUpdate(sessionId, piece.characterId())
                : controlRepository.find(sessionId, piece.characterId()))
                .orElseThrow(() -> new it.uniupo.boardhub.eventservice.service.exception.CharacterControlConflictException(
                        "Il Dungeon Master deve prima assumere il controllo del personaggio."
                ));
        if (!control.dmParticipantId().equals(dmParticipantId)) {
            throw new it.uniupo.boardhub.eventservice.service.exception.CharacterControlConflictException(
                    "Il personaggio e controllato da un altro dispositivo Dungeon Master."
            );
        }
        return piece;
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

    private void requirePlayerControlAvailable(SessionPiece piece) {
        if (controlRepository != null
                && controlRepository.find(piece.sessionId(), piece.characterId()).isPresent()) {
            throw new it.uniupo.boardhub.eventservice.service.exception.CharacterControlConflictException(
                    "Il personaggio e temporaneamente controllato dal Dungeon Master."
            );
        }
    }

    private List<ReachableCell> calculateReachable(
            SessionPiece piece,
            PlayerCharacter character
    ) {
        GameGrid grid = gridService.loadGrid(piece.sessionId());
        GridPosition start = GridPosition.fromCell(piece.currentCell());
        var startCell = grid.cellAt(start);
        grid = grid.withCell(new it.uniupo.boardhub.eventservice.model.grid.GridCell(
                start, startCell.terrainType(), false
        ));
        return movementService.calculateReachableCells(
                grid,
                new MovementRequest(
                        character.characterId().toString(),
                        start,
                        character.speedCells()
                )
        );
    }

    private TrapEncounter firstTrigger(
            String sessionId,
            ReachableCell destination,
            List<ReachableCell> reachableCells
    ) {
        if (trapRepository == null) {
            return null;
        }
        Map<String, TrapDefinition> traps = trapRepository.findBySession(sessionId).stream()
                .collect(Collectors.toMap(TrapDefinition::cell, Function.identity()));
        List<GridPosition> path = destination.path();
        for (int index = 1; index < path.size(); index++) {
            String cell = path.get(index).toCell();
            TrapDefinition trap = traps.get(cell);
            if (trap != null && trap.triggersOnEntry()) {
                GridPosition triggerPosition = path.get(index);
                int cost = reachableCells.stream()
                        .filter(item -> item.position().equals(triggerPosition))
                        .mapToInt(ReachableCell::cost)
                        .findFirst()
                        .orElseThrow();
                return new TrapEncounter(trap, index, cost);
            }
        }
        return null;
    }

    private PieceMoveResult triggerTrap(
            String sessionId,
            SessionPiece piece,
            PlayerCharacter character,
            MoveData data,
            ReachableCell destination,
            TrapEncounter encounter,
            Actor actor
    ) {
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        TrapDefinition lockedTrap = trapRepository.findByIdForUpdate(
                sessionId, encounter.trap().trapId()
        ).orElseThrow(() -> new MoveRejectedException("La trappola non e piu disponibile."));
        if (!lockedTrap.triggersOnEntry()) {
            throw new StalePieceStateException(piece.version());
        }
        String triggerCell = lockedTrap.cell();
        try {
            if (!pieceRepository.move(
                    piece.sessionPieceId(), sessionId, triggerCell, data.expectedVersion(), now
            )) {
                throw new StalePieceStateException(piece.version());
            }
        } catch (DuplicateKeyException ex) {
            throw new MoveRejectedException("La cella " + triggerCell + " e gia occupata.");
        }

        TrapDefinition.LifecycleState nextState = lockedTrap.lifecyclePolicy()
                == TrapDefinition.LifecyclePolicy.ONE_SHOT
                ? TrapDefinition.LifecycleState.SPENT
                : TrapDefinition.LifecycleState.TRIGGERED_ACTIVE;
        TrapVisibility nextVisibility = lockedTrap.visibility() == TrapVisibility.HIDDEN
                ? TrapVisibility.REVEALED
                : lockedTrap.visibility();
        if (!trapRepository.markTriggered(
                sessionId, lockedTrap.trapId(), lockedTrap.version(), nextState,
                nextVisibility, now
        )) {
            throw new StalePieceStateException(piece.version());
        }

        List<String> fullPath = destination.path().stream().map(GridPosition::toCell).toList();
        List<String> traversed = List.copyOf(fullPath.subList(0, encounter.pathIndex() + 1));
        List<String> remaining = List.copyOf(fullPath.subList(encounter.pathIndex() + 1, fullPath.size()));
        UUID resolutionId = UUID.randomUUID();
        TrapResolution resolution = new TrapResolution(
                resolutionId, sessionId, lockedTrap.trapId(), piece.sessionPieceId(),
                piece.characterId(), piece.participantId(), data.commandId(), data.expectedVersion(),
                TrapResolution.Status.AWAITING_SAVE_ROLL, piece.currentCell(), data.destination(),
                triggerCell, fullPath, remaining, character.speedCells(), encounter.cost(),
                Math.max(0, character.speedCells() - encounter.cost()), null,
                null, null, null, null, null, null, List.of(), null, null,
                null, null, null, 0, now, now, null
        );
        resolutionRepository.save(resolution);

        String eventId = "trap-" + data.commandId();
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("commandId", data.commandId().toString());
        payload.put("resolutionId", resolutionId.toString());
        payload.put("sessionPieceId", piece.sessionPieceId().toString());
        payload.put("characterId", piece.characterId().toString());
        payload.put("from", piece.currentCell());
        payload.put("to", triggerCell);
        payload.put("requestedDestination", data.destination());
        payload.put("resultingVersion", piece.version() + 1);
        payload.put("actorRole", actor.role());
        payload.put("actorId", actor.participantId().toString());
        addStrings(payload.putArray("path"), traversed);
        saveBackendEvent(sessionId, eventId, "TRAP_TRIGGERED", payload, now);

        List<String> visibleTraps = nextVisibility == TrapVisibility.REVEALED
                ? List.of(lockedTrap.trapId())
                : List.of();
        return new PieceMoveResult(
                data.commandId(), eventId, piece.sessionPieceId(), piece.characterId(),
                piece.currentCell(), triggerCell, traversed, encounter.cost(), piece.version() + 1,
                visibleTraps, PieceMoveResult.Status.TRAP_PENDING, resolutionId,
                data.destination(), resolution.movementRemaining()
        );
    }

    private void saveBackendEvent(
            String sessionId,
            String eventId,
            String eventType,
            ObjectNode payload,
            OffsetDateTime now
    ) {
        GameSession session = sessionRepository.findSessionById(sessionId)
                .orElseThrow(() -> new IllegalStateException("Sessione scomparsa durante l'operazione."));
        GameEvent event = new GameEvent(
                eventId, eventType, session.venueId(), session.tableId(), sessionId,
                EVENT_SOURCE, now.toString(), sessionRepository.nextServerEventSequence(sessionId), payload
        );
        try {
            if (!eventRepository.save(event)) {
                throw new MoveCommandConflictException();
            }
            publishAfterCommit(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Impossibile serializzare l'evento backend.", ex);
        }
    }

    private PieceMoveResult replayPending(
            TrapResolution resolution,
            String sessionId,
            SessionPiece piece,
            MoveData data
    ) {
        if (!resolution.sessionId().equals(sessionId)
                || !resolution.sessionPieceId().equals(piece.sessionPieceId())
                || !resolution.characterId().equals(piece.characterId())
                || !resolution.requestedDestination().equals(data.destination())
                || resolution.expectedPieceVersion() != data.expectedVersion()) {
            throw new MoveCommandConflictException();
        }
        int triggerIndex = resolution.path().indexOf(resolution.triggerCell());
        List<String> traversed = triggerIndex < 0
                ? List.of(resolution.fromCell(), resolution.triggerCell())
                : List.copyOf(resolution.path().subList(0, triggerIndex + 1));
        List<String> visibleTraps = trapRepository.findBySessionAndCell(
                        sessionId, resolution.triggerCell()
                )
                .filter(trap -> trap.visibility().normalized() == TrapVisibility.REVEALED)
                .map(trap -> List.of(trap.trapId()))
                .orElseGet(List::of);
        return new PieceMoveResult(
                data.commandId(), "trap-" + data.commandId(), piece.sessionPieceId(),
                piece.characterId(), resolution.fromCell(), resolution.triggerCell(), traversed,
                resolution.costToTrigger(), data.expectedVersion() + 1, visibleTraps,
                PieceMoveResult.Status.TRAP_PENDING, resolution.resolutionId(),
                resolution.requestedDestination(), resolution.movementRemaining()
        );
    }

    private void publishAfterCommit(GameEvent event) {
        if (eventStreamService != null) {
            eventStreamService.publishAfterCommit(event);
        }
    }

    private ObjectNode createPayload(
            UUID commandId,
            long expectedVersion,
            SessionPiece piece,
            String destination,
            List<String> path,
            int cost,
            long resultingVersion,
            List<String> visibleTraps,
            Actor actor
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
        payload.put("actorRole", actor.role());
        payload.put("actorId", actor.participantId().toString());
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

    private record TrapEncounter(TrapDefinition trap, int pathIndex, int cost) {
    }

    private record Actor(String role, UUID participantId) {
    }
}
