package it.uniupo.boardhub.eventservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniupo.boardhub.eventservice.model.GameEvent;
import it.uniupo.boardhub.eventservice.model.character.CharacterTacticalStatus;
import it.uniupo.boardhub.eventservice.model.character.PlayerCharacter;
import it.uniupo.boardhub.eventservice.model.join.SessionParticipant;
import it.uniupo.boardhub.eventservice.model.session.GameSession;
import it.uniupo.boardhub.eventservice.model.trap.DiceExpression;
import it.uniupo.boardhub.eventservice.model.trap.TrapDefinition;
import it.uniupo.boardhub.eventservice.model.trap.TrapResolution;
import it.uniupo.boardhub.eventservice.model.trap.TrapRollResult;
import it.uniupo.boardhub.eventservice.repository.CharacterRepository;
import it.uniupo.boardhub.eventservice.repository.CharacterControlRepository;
import it.uniupo.boardhub.eventservice.repository.GameEventRepository;
import it.uniupo.boardhub.eventservice.repository.GameSessionRepository;
import it.uniupo.boardhub.eventservice.repository.TrapRepository;
import it.uniupo.boardhub.eventservice.repository.TrapResolutionRepository;
import it.uniupo.boardhub.eventservice.repository.SessionPieceRepository;
import it.uniupo.boardhub.eventservice.model.piece.PieceMoveResult;
import it.uniupo.boardhub.eventservice.model.piece.SessionPiece;
import it.uniupo.boardhub.eventservice.model.grid.GameGrid;
import it.uniupo.boardhub.eventservice.model.grid.GridPosition;
import it.uniupo.boardhub.eventservice.model.grid.TrapVisibility;
import it.uniupo.boardhub.eventservice.service.command.ContinueTrapMovementCommand;
import it.uniupo.boardhub.eventservice.service.exception.MoveRejectedException;
import it.uniupo.boardhub.eventservice.service.exception.StalePieceStateException;
import org.springframework.dao.DuplicateKeyException;
import it.uniupo.boardhub.eventservice.service.command.RollTrapSaveCommand;
import it.uniupo.boardhub.eventservice.service.exception.CharacterNotFoundException;
import it.uniupo.boardhub.eventservice.service.exception.TrapResolutionConflictException;
import it.uniupo.boardhub.eventservice.service.exception.TrapResolutionNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class TrapResolutionService {

    private static final String EVENT_SOURCE = "BACKEND";

    private final ParticipantAccessService participantAccessService;
    private final TrapResolutionRepository resolutionRepository;
    private final TrapRepository trapRepository;
    private final CharacterRepository characterRepository;
    private final GameSessionRepository sessionRepository;
    private final GameEventRepository eventRepository;
    private final SessionPieceRepository pieceRepository;
    private final SessionGridService gridService;
    private final DiceRoller diceRoller;
    private final CharacterControlRepository controlRepository;
    private final SessionEventStreamService eventStreamService;
    private final Clock clock;

    public TrapResolutionService(
            ParticipantAccessService participantAccessService,
            TrapResolutionRepository resolutionRepository,
            TrapRepository trapRepository,
            CharacterRepository characterRepository,
            GameSessionRepository sessionRepository,
            GameEventRepository eventRepository,
            SessionPieceRepository pieceRepository,
            SessionGridService gridService,
            DiceRoller diceRoller,
            CharacterControlRepository controlRepository,
            SessionEventStreamService eventStreamService,
            Clock clock
    ) {
        this.participantAccessService = participantAccessService;
        this.resolutionRepository = resolutionRepository;
        this.trapRepository = trapRepository;
        this.characterRepository = characterRepository;
        this.sessionRepository = sessionRepository;
        this.eventRepository = eventRepository;
        this.pieceRepository = pieceRepository;
        this.gridService = gridService;
        this.diceRoller = diceRoller;
        this.controlRepository = controlRepository;
        this.eventStreamService = eventStreamService;
        this.clock = clock;
    }

    @Transactional
    public PieceMoveResult continueMovement(
            String sessionId,
            UUID resolutionId,
            String authorizationHeader,
            ContinueTrapMovementCommand command
    ) {
        ContinueData data = validate(command);
        SessionParticipant participant = participantAccessService.requireActiveParticipantForUpdate(
                sessionId, authorizationHeader
        );
        TrapResolution resolution = resolutionRepository.findByIdForUpdate(sessionId, resolutionId)
                .orElseThrow(TrapResolutionNotFoundException::new);
        requireOwner(resolution, participant);
        requirePlayerControlAvailable(resolution);
        return continueLocked(
                resolution,
                data,
                new Actor("PLAYER", participant.participantId())
        );
    }

    @Transactional
    public PieceMoveResult continueMovementAsDm(
            String sessionId,
            UUID resolutionId,
            UUID dmParticipantId,
            ContinueTrapMovementCommand command
    ) {
        ContinueData data = validate(command);
        TrapResolution resolution = resolutionRepository.findByIdForUpdate(sessionId, resolutionId)
                .orElseThrow(TrapResolutionNotFoundException::new);
        requireDmControl(resolution, dmParticipantId);
        return continueLocked(resolution, data, new Actor("DM", dmParticipantId));
    }

    private PieceMoveResult continueLocked(
            TrapResolution resolution,
            ContinueData data,
            Actor actor
    ) {
        String sessionId = resolution.sessionId();

        if (resolution.continueCommandId() != null) {
            if (!resolution.continueCommandId().equals(data.commandId())) {
                throw new TrapResolutionConflictException(
                        "La continuazione e gia stata eseguita con un altro commandId."
                );
            }
            return replayContinuation(resolution, data.commandId());
        }
        if (resolution.status() != TrapResolution.Status.CONTINUATION_ALLOWED) {
            throw new TrapResolutionConflictException(
                    "La risoluzione non consente di continuare il movimento."
            );
        }
        if (resolution.version() != data.expectedVersion()) {
            throw new TrapResolutionConflictException(
                    "La risoluzione e stata aggiornata: versione corrente " + resolution.version() + "."
            );
        }

        SessionPiece piece = pieceRepository.findByIdAndSessionForUpdate(
                resolution.sessionPieceId(), sessionId
        ).orElseThrow(TrapResolutionNotFoundException::new);
        if (!piece.participantId().equals(resolution.participantId())
                || !piece.currentCell().equals(resolution.triggerCell())) {
            throw new TrapResolutionConflictException(
                    "La posizione della pedina non coincide con la trappola risolta."
            );
        }

        OffsetDateTime now = now();
        GameGrid grid = gridService.loadGrid(sessionId);
        NextTrap nextTrap = findNextTrap(sessionId, resolution, grid);
        if (nextTrap == null) {
            movePiece(piece, resolution.requestedDestination(), now);
            if (!resolutionRepository.completeContinuation(
                    resolution.resolutionId(), data.expectedVersion(), data.commandId(), now
            )) {
                throw new TrapResolutionConflictException("Continuazione aggiornata da un'altra richiesta.");
            }
            List<String> path = continuationPath(resolution);
            int cost = remainingCost(grid, resolution.triggerCell(), resolution.remainingPath());
            ObjectNode payload = JsonNodeFactory.instance.objectNode();
            payload.put("commandId", data.commandId().toString());
            payload.put("sessionPieceId", piece.sessionPieceId().toString());
            payload.put("characterId", piece.characterId().toString());
            payload.put("from", piece.currentCell());
            payload.put("to", resolution.requestedDestination());
            payload.put("cost", cost);
            payload.put("resultingVersion", piece.version() + 1);
            addActor(payload, actor);
            saveSimpleEvent(sessionId, "continue-" + data.commandId(), "MOVE_CONFIRMED", payload, now);
            saveResolutionCompletedEvent(resolution, data.commandId(), actor, now);
            return new PieceMoveResult(
                    data.commandId(), "continue-" + data.commandId(), piece.sessionPieceId(),
                    piece.characterId(), piece.currentCell(), resolution.requestedDestination(),
                    path, cost, piece.version() + 1, List.of()
            );
        }

        movePiece(piece, nextTrap.trap().cell(), now);
        TrapDefinition lockedTrap = trapRepository.findByIdForUpdate(
                sessionId, nextTrap.trap().trapId()
        ).orElseThrow(TrapResolutionNotFoundException::new);
        if (!lockedTrap.triggersOnEntry()) {
            throw new TrapResolutionConflictException("La trappola successiva e gia stata modificata.");
        }
        TrapDefinition.LifecycleState state = lockedTrap.lifecyclePolicy()
                == TrapDefinition.LifecyclePolicy.ONE_SHOT
                ? TrapDefinition.LifecycleState.SPENT
                : TrapDefinition.LifecycleState.TRIGGERED_ACTIVE;
        TrapVisibility visibility = lockedTrap.visibility() == TrapVisibility.HIDDEN
                ? TrapVisibility.REVEALED
                : lockedTrap.visibility();
        if (!trapRepository.markTriggered(
                sessionId, lockedTrap.trapId(), lockedTrap.version(), state, visibility, now
        )) {
            throw new TrapResolutionConflictException("La trappola successiva e gia stata modificata.");
        }
        if (!resolutionRepository.completeContinuation(
                resolution.resolutionId(), data.expectedVersion(), data.commandId(), now
        )) {
            throw new TrapResolutionConflictException("Continuazione aggiornata da un'altra richiesta.");
        }

        List<String> path = continuationPath(resolution);
        int triggerIndex = path.indexOf(lockedTrap.cell());
        List<String> remaining = List.copyOf(path.subList(triggerIndex + 1, path.size()));
        UUID nextResolutionId = UUID.randomUUID();
        TrapResolution next = new TrapResolution(
                nextResolutionId, sessionId, lockedTrap.trapId(), piece.sessionPieceId(),
                piece.characterId(), piece.participantId(), data.commandId(), piece.version(),
                TrapResolution.Status.AWAITING_SAVE_ROLL, piece.currentCell(),
                resolution.requestedDestination(), lockedTrap.cell(), path, remaining,
                resolution.movementRemaining(), nextTrap.cost(),
                Math.max(0, resolution.movementRemaining() - nextTrap.cost()), null,
                null, null, null, null, null, null, List.of(), null, null,
                null, null, null, 0, now, now, null
        );
        resolutionRepository.save(next);
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("commandId", data.commandId().toString());
        payload.put("resolutionId", nextResolutionId.toString());
        payload.put("sessionPieceId", piece.sessionPieceId().toString());
        payload.put("characterId", piece.characterId().toString());
        payload.put("from", piece.currentCell());
        payload.put("to", lockedTrap.cell());
        payload.put("requestedDestination", resolution.requestedDestination());
        payload.put("resultingVersion", piece.version() + 1);
        addActor(payload, actor);
        saveResolutionCompletedEvent(resolution, data.commandId(), actor, now);
        saveSimpleEvent(sessionId, "trap-" + data.commandId(), "TRAP_TRIGGERED", payload, now);
        List<String> traversed = List.copyOf(path.subList(0, triggerIndex + 1));
        return new PieceMoveResult(
                data.commandId(), "trap-" + data.commandId(), piece.sessionPieceId(),
                piece.characterId(), piece.currentCell(), lockedTrap.cell(), traversed,
                nextTrap.cost(), piece.version() + 1,
                visibility == TrapVisibility.REVEALED ? List.of(lockedTrap.trapId()) : List.of(),
                PieceMoveResult.Status.TRAP_PENDING, nextResolutionId,
                resolution.requestedDestination(), next.movementRemaining()
        );
    }

    @Transactional(readOnly = true)
    public TrapResolution getOwned(
            String sessionId,
            UUID resolutionId,
            String authorizationHeader
    ) {
        SessionParticipant participant = participantAccessService.requireActiveParticipant(
                sessionId, authorizationHeader
        );
        TrapResolution resolution = resolutionRepository.findById(sessionId, resolutionId)
                .orElseThrow(TrapResolutionNotFoundException::new);
        requireOwner(resolution, participant);
        requirePlayerControlAvailable(resolution);
        return resolution;
    }

    @Transactional(readOnly = true)
    public TrapResolution getAsDm(
            String sessionId,
            UUID resolutionId
    ) {
        return resolutionRepository.findById(sessionId, resolutionId)
                .orElseThrow(TrapResolutionNotFoundException::new);
    }

    @Transactional
    public TrapRollResult roll(
            String sessionId,
            UUID resolutionId,
            String authorizationHeader,
            RollTrapSaveCommand command
    ) {
        RollData data = validate(command);
        SessionParticipant participant = participantAccessService.requireActiveParticipantForUpdate(
                sessionId, authorizationHeader
        );
        TrapResolution resolution = resolutionRepository.findByIdForUpdate(sessionId, resolutionId)
                .orElseThrow(TrapResolutionNotFoundException::new);
        requireOwner(resolution, participant);
        requirePlayerControlAvailable(resolution);
        return rollLocked(
                resolution,
                data,
                new Actor("PLAYER", participant.participantId())
        );
    }

    @Transactional
    public TrapRollResult rollAsDm(
            String sessionId,
            UUID resolutionId,
            UUID dmParticipantId,
            RollTrapSaveCommand command
    ) {
        RollData data = validate(command);
        TrapResolution resolution = resolutionRepository.findByIdForUpdate(sessionId, resolutionId)
                .orElseThrow(TrapResolutionNotFoundException::new);
        requireDmControl(resolution, dmParticipantId);
        return rollLocked(resolution, data, new Actor("DM", dmParticipantId));
    }

    private TrapRollResult rollLocked(TrapResolution resolution, RollData data, Actor actor) {
        String sessionId = resolution.sessionId();

        String fingerprint = fingerprint(data.expectedVersion());
        if (resolution.rollCommandId() != null) {
            if (!resolution.rollCommandId().equals(data.commandId())
                    || !fingerprint.equals(resolution.rollFingerprint())) {
                throw new TrapResolutionConflictException(
                        "Il commandId del tiro e gia stato usato con dati differenti."
                );
            }
            return replay(resolution);
        }
        if (resolution.status() != TrapResolution.Status.AWAITING_SAVE_ROLL) {
            throw new TrapResolutionConflictException(
                    "La risoluzione non e in attesa di un tiro salvezza."
            );
        }
        if (resolution.version() != data.expectedVersion()) {
            throw new TrapResolutionConflictException(
                    "La risoluzione e stata aggiornata: versione corrente " + resolution.version() + "."
            );
        }

        PlayerCharacter character = characterRepository.findByIdAndSessionForUpdate(
                resolution.characterId(), sessionId
        ).orElseThrow(CharacterNotFoundException::new);
        TrapDefinition trap = trapRepository.findByIdForUpdate(sessionId, resolution.trapId())
                .orElseThrow(TrapResolutionNotFoundException::new);

        int first = diceRoller.roll(20);
        Integer second = trap.rollMode() == TrapDefinition.RollMode.NORMAL
                ? null
                : diceRoller.roll(20);
        int selected = select(first, second, trap.rollMode());
        int bonus = character.saveBonus(trap.saveAbility());
        int total = selected + bonus;
        boolean success = total >= trap.saveDc();

        TrapDefinition.DamagePolicy damagePolicy = success
                ? trap.successDamage()
                : trap.failureDamage();
        Damage damage = rollDamage(trap.damageExpression(), damagePolicy);
        int hpCurrent = Math.max(0, character.hpCurrent() - damage.total());
        CharacterTacticalStatus tacticalStatus = hpCurrent == 0
                ? CharacterTacticalStatus.DOWNED
                : CharacterTacticalStatus.ACTIVE;
        if (damage.total() > 0 && !characterRepository.applyDamage(
                character.characterId(), sessionId, damage.total(), character.version(), now()
        )) {
            throw new TrapResolutionConflictException(
                    "La scheda del personaggio e stata aggiornata durante il tiro."
            );
        }

        TrapDefinition.MovementDecision movement = success
                ? trap.successMovement()
                : trap.failureMovement();
        if (tacticalStatus == CharacterTacticalStatus.DOWNED) {
            movement = TrapDefinition.MovementDecision.STOP;
        }
        TrapResolution.Status status = movement == TrapDefinition.MovementDecision.CONTINUE
                && !resolution.remainingPath().isEmpty()
                ? TrapResolution.Status.CONTINUATION_ALLOWED
                : TrapResolution.Status.COMPLETED;
        OffsetDateTime now = now();
        if (!resolutionRepository.recordRoll(
                resolution.resolutionId(), data.expectedVersion(), data.commandId(), fingerprint,
                first, second, selected, bonus, total, success, damage.rolls(), damage.total(),
                movement, status, now
        )) {
            throw new TrapResolutionConflictException(
                    "La risoluzione della trappola e stata aggiornata durante il tiro."
            );
        }

        saveEvent(
                sessionId,
                "trap-roll-" + data.commandId(),
                resolution,
                success,
                damage.total(),
                hpCurrent,
                tacticalStatus,
                movement,
                actor,
                now
        );
        if (tacticalStatus == CharacterTacticalStatus.DOWNED) {
            ObjectNode downed = JsonNodeFactory.instance.objectNode();
            downed.put("resolutionId", resolution.resolutionId().toString());
            downed.put("sessionPieceId", resolution.sessionPieceId().toString());
            downed.put("characterId", resolution.characterId().toString());
            downed.put("hpCurrent", hpCurrent);
            downed.put("tacticalStatus", tacticalStatus.name());
            addActor(downed, actor);
            saveSimpleEvent(
                    sessionId, "character-downed-" + data.commandId(),
                    "CHARACTER_DOWNED", downed, now
            );
        }
        if (status == TrapResolution.Status.COMPLETED) {
            saveResolutionCompletedEvent(resolution, data.commandId(), actor, now);
        }
        return new TrapRollResult(
                resolution.resolutionId(), status, first, second, selected, bonus, total,
                success, damage.rolls(), damage.total(), hpCurrent,
                TrapRollResult.CharacterTacticalOutcome.valueOf(tacticalStatus.name()),
                movement, resolution.movementRemaining(), resolution.version() + 1
        );
    }

    private TrapRollResult replay(TrapResolution resolution) {
        PlayerCharacter character = characterRepository.findByIdAndSession(
                resolution.characterId(), resolution.sessionId()
        ).orElseThrow(CharacterNotFoundException::new);
        return new TrapRollResult(
                resolution.resolutionId(), resolution.status(), resolution.saveD20First(),
                resolution.saveD20Second(), resolution.saveSelected(), resolution.saveBonus(),
                resolution.saveTotal(), Boolean.TRUE.equals(resolution.saveSuccess()),
                resolution.damageRolls(), resolution.damageTotal() == null ? 0 : resolution.damageTotal(),
                character.hpCurrent(),
                TrapRollResult.CharacterTacticalOutcome.valueOf(character.tacticalStatus().name()),
                resolution.movementDecision(), resolution.movementRemaining(), resolution.version()
        );
    }

    private Damage rollDamage(String expression, TrapDefinition.DamagePolicy policy) {
        if (policy == TrapDefinition.DamagePolicy.NONE) {
            return new Damage(List.of(), 0);
        }
        DiceExpression dice = DiceExpression.parse(expression);
        List<Integer> rolls = new ArrayList<>();
        int raw = dice.modifier();
        for (int index = 0; index < dice.count(); index++) {
            int roll = diceRoller.roll(dice.sides());
            rolls.add(roll);
            raw += roll;
        }
        raw = Math.max(0, raw);
        int total = policy == TrapDefinition.DamagePolicy.HALF ? raw / 2 : raw;
        return new Damage(List.copyOf(rolls), total);
    }

    private int select(int first, Integer second, TrapDefinition.RollMode mode) {
        if (second == null) {
            return first;
        }
        return mode == TrapDefinition.RollMode.ADVANTAGE
                ? Math.max(first, second)
                : Math.min(first, second);
    }

    private void requireOwner(TrapResolution resolution, SessionParticipant participant) {
        if (!resolution.participantId().equals(participant.participantId())) {
            throw new TrapResolutionNotFoundException();
        }
    }

    private void requirePlayerControlAvailable(TrapResolution resolution) {
        if (controlRepository.find(resolution.sessionId(), resolution.characterId()).isPresent()) {
            throw new it.uniupo.boardhub.eventservice.service.exception.CharacterControlConflictException(
                    "Il personaggio e temporaneamente controllato dal Dungeon Master."
            );
        }
    }

    private void requireDmControl(TrapResolution resolution, UUID dmParticipantId) {
        if (dmParticipantId == null) {
            throw new IllegalArgumentException("Dungeon Master obbligatorio.");
        }
        var control = controlRepository.findForUpdate(
                        resolution.sessionId(), resolution.characterId()
                )
                .orElseThrow(() -> new it.uniupo.boardhub.eventservice.service.exception.CharacterControlConflictException(
                        "Il Dungeon Master deve prima assumere il controllo del personaggio."
                ));
        if (!control.dmParticipantId().equals(dmParticipantId)) {
            throw new it.uniupo.boardhub.eventservice.service.exception.CharacterControlConflictException(
                    "Il personaggio e controllato da un altro dispositivo Dungeon Master."
            );
        }
    }

    private RollData validate(RollTrapSaveCommand command) {
        if (command == null || command.expectedVersion() == null || command.expectedVersion() < 0) {
            throw new IllegalArgumentException("expectedVersion deve essere maggiore o uguale a zero.");
        }
        if (command.commandId() == null) {
            throw new IllegalArgumentException("commandId e obbligatorio.");
        }
        return new RollData(command.expectedVersion(), command.commandId());
    }

    private ContinueData validate(ContinueTrapMovementCommand command) {
        if (command == null || command.expectedVersion() == null || command.expectedVersion() < 0) {
            throw new IllegalArgumentException("expectedVersion deve essere maggiore o uguale a zero.");
        }
        if (command.commandId() == null) {
            throw new IllegalArgumentException("commandId e obbligatorio.");
        }
        return new ContinueData(command.expectedVersion(), command.commandId());
    }

    private void movePiece(SessionPiece piece, String destination, OffsetDateTime now) {
        try {
            if (!pieceRepository.move(
                    piece.sessionPieceId(), piece.sessionId(), destination, piece.version(), now
            )) {
                throw new StalePieceStateException(piece.version());
            }
        } catch (DuplicateKeyException ex) {
            throw new MoveRejectedException("La cella " + destination + " e gia occupata.");
        }
    }

    private NextTrap findNextTrap(String sessionId, TrapResolution resolution, GameGrid grid) {
        var traps = trapRepository.findBySession(sessionId).stream()
                .collect(java.util.stream.Collectors.toMap(TrapDefinition::cell, java.util.function.Function.identity()));
        int cost = 0;
        for (String cell : resolution.remainingPath()) {
            cost += grid.cellAt(GridPosition.fromCell(cell)).movementCost();
            TrapDefinition trap = traps.get(cell);
            if (trap != null && trap.triggersOnEntry()) {
                return new NextTrap(trap, cost);
            }
        }
        return null;
    }

    private int remainingCost(GameGrid grid, String fromCell, List<String> remainingPath) {
        int cost = 0;
        for (String cell : remainingPath) {
            cost += grid.cellAt(GridPosition.fromCell(cell)).movementCost();
        }
        return cost;
    }

    private List<String> continuationPath(TrapResolution resolution) {
        List<String> path = new ArrayList<>();
        path.add(resolution.triggerCell());
        path.addAll(resolution.remainingPath());
        return List.copyOf(path);
    }

    private PieceMoveResult replayContinuation(TrapResolution resolution, UUID commandId) {
        var next = resolutionRepository.findByMoveCommand(commandId);
        if (next.isPresent()) {
            TrapResolution pending = next.get();
            int index = pending.path().indexOf(pending.triggerCell());
            return new PieceMoveResult(
                    commandId, "trap-" + commandId, pending.sessionPieceId(), pending.characterId(),
                    pending.fromCell(), pending.triggerCell(),
                    List.copyOf(pending.path().subList(0, index + 1)), pending.costToTrigger(),
                    pending.expectedPieceVersion() + 1, List.of(), PieceMoveResult.Status.TRAP_PENDING,
                    pending.resolutionId(), pending.requestedDestination(), pending.movementRemaining()
            );
        }
        return new PieceMoveResult(
                commandId, "continue-" + commandId, resolution.sessionPieceId(),
                resolution.characterId(), resolution.triggerCell(), resolution.requestedDestination(),
                continuationPath(resolution),
                resolution.movementRemaining(), resolution.expectedPieceVersion() + 2,
                List.of()
        );
    }

    private void saveSimpleEvent(
            String sessionId, String eventId, String eventType, ObjectNode payload, OffsetDateTime now
    ) {
        GameSession session = sessionRepository.findSessionById(sessionId)
                .orElseThrow(() -> new IllegalStateException("Sessione scomparsa durante il movimento."));
        GameEvent event = new GameEvent(
                eventId, eventType, session.venueId(), session.tableId(), sessionId,
                EVENT_SOURCE, now.toString(), sessionRepository.nextServerEventSequence(sessionId), payload
        );
        try {
            if (!eventRepository.save(event)) {
                throw new TrapResolutionConflictException("Il comando e gia stato registrato.");
            }
            eventStreamService.publishAfterCommit(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Impossibile serializzare l'evento di continuazione.", ex);
        }
    }

    private String fingerprint(long expectedVersion) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(Long.toString(expectedVersion).getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 non disponibile.", ex);
        }
    }

    private void saveEvent(
            String sessionId,
            String eventId,
            TrapResolution resolution,
            boolean success,
            int damage,
            int hpCurrent,
            CharacterTacticalStatus tacticalStatus,
            TrapDefinition.MovementDecision movement,
            Actor actor,
            OffsetDateTime now
    ) {
        GameSession session = sessionRepository.findSessionById(sessionId)
                .orElseThrow(() -> new IllegalStateException("Sessione scomparsa durante il tiro."));
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("resolutionId", resolution.resolutionId().toString());
        payload.put("sessionPieceId", resolution.sessionPieceId().toString());
        payload.put("characterId", resolution.characterId().toString());
        payload.put("success", success);
        payload.put("damage", damage);
        payload.put("hpCurrent", hpCurrent);
        payload.put("tacticalStatus", tacticalStatus.name());
        payload.put("movementDecision", movement.name());
        addActor(payload, actor);
        GameEvent event = new GameEvent(
                eventId, "TRAP_ROLL_RESOLVED", session.venueId(), session.tableId(), sessionId,
                EVENT_SOURCE, now.toString(), sessionRepository.nextServerEventSequence(sessionId), payload
        );
        try {
            if (!eventRepository.save(event)) {
                throw new TrapResolutionConflictException("Il tiro e gia stato registrato.");
            }
            eventStreamService.publishAfterCommit(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Impossibile serializzare l'evento della trappola.", ex);
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
    }

    private void saveResolutionCompletedEvent(
            TrapResolution resolution,
            UUID commandId,
            Actor actor,
            OffsetDateTime now
    ) {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("commandId", commandId.toString());
        payload.put("resolutionId", resolution.resolutionId().toString());
        payload.put("sessionPieceId", resolution.sessionPieceId().toString());
        payload.put("characterId", resolution.characterId().toString());
        addActor(payload, actor);
        saveSimpleEvent(
                resolution.sessionId(), "trap-complete-" + commandId,
                "TRAP_RESOLUTION_COMPLETED", payload, now
        );
    }

    private void addActor(ObjectNode payload, Actor actor) {
        payload.put("actorRole", actor.role());
        payload.put("actorId", actor.participantId().toString());
    }

    private record RollData(long expectedVersion, UUID commandId) { }
    private record ContinueData(long expectedVersion, UUID commandId) { }
    private record Damage(List<Integer> rolls, int total) { }
    private record NextTrap(TrapDefinition trap, int cost) { }
    private record Actor(String role, UUID participantId) { }
}
