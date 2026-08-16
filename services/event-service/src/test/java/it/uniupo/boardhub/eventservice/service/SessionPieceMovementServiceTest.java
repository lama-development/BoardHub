package it.uniupo.boardhub.eventservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.boardhub.eventservice.config.JoinProperties;
import it.uniupo.boardhub.eventservice.model.GameEvent;
import it.uniupo.boardhub.eventservice.model.character.PartyVisibility;
import it.uniupo.boardhub.eventservice.model.character.PlayerCharacter;
import it.uniupo.boardhub.eventservice.model.character.CharacterTacticalStatus;
import it.uniupo.boardhub.eventservice.model.grid.TerrainType;
import it.uniupo.boardhub.eventservice.model.grid.TrapVisibility;
import it.uniupo.boardhub.eventservice.model.join.ParticipantRole;
import it.uniupo.boardhub.eventservice.model.join.ParticipantStatus;
import it.uniupo.boardhub.eventservice.model.join.SessionParticipant;
import it.uniupo.boardhub.eventservice.model.piece.RepresentationMode;
import it.uniupo.boardhub.eventservice.model.piece.SessionPiece;
import it.uniupo.boardhub.eventservice.model.session.GameSession;
import it.uniupo.boardhub.eventservice.model.session.GameSessionStatus;
import it.uniupo.boardhub.eventservice.model.session.GridCellState;
import it.uniupo.boardhub.eventservice.model.session.GridTrapState;
import it.uniupo.boardhub.eventservice.model.trap.TrapDefinition;
import it.uniupo.boardhub.eventservice.model.trap.TrapResolution;
import it.uniupo.boardhub.eventservice.repository.CharacterRepository;
import it.uniupo.boardhub.eventservice.repository.CharacterControlRepository;
import it.uniupo.boardhub.eventservice.repository.GameEventRepository;
import it.uniupo.boardhub.eventservice.repository.GameSessionRepository;
import it.uniupo.boardhub.eventservice.repository.SessionParticipantRepository;
import it.uniupo.boardhub.eventservice.repository.SessionPieceRepository;
import it.uniupo.boardhub.eventservice.repository.TrapRepository;
import it.uniupo.boardhub.eventservice.repository.TrapResolutionRepository;
import it.uniupo.boardhub.eventservice.service.command.ContinueTrapMovementCommand;
import it.uniupo.boardhub.eventservice.service.command.MoveSessionPieceCommand;
import it.uniupo.boardhub.eventservice.service.command.RollTrapSaveCommand;
import it.uniupo.boardhub.eventservice.service.exception.MoveCommandConflictException;
import it.uniupo.boardhub.eventservice.service.exception.CharacterControlConflictException;
import it.uniupo.boardhub.eventservice.service.exception.MoveRejectedException;
import it.uniupo.boardhub.eventservice.service.exception.SessionPieceNotFoundException;
import it.uniupo.boardhub.eventservice.service.exception.StalePieceStateException;
import it.uniupo.boardhub.eventservice.support.MigratedTestDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionPieceMovementServiceTest {

    private static final String SESSION_ID = "session-move-001";
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-07-29T10:00:00Z"), ZoneOffset.UTC);

    private JdbcTemplate jdbcTemplate;
    private SessionPieceMovementService service;
    private TrapResolutionService trapResolutionService;
    private SessionPieceRepository pieceRepository;
    private GameEventRepository eventRepository;
    private GameSessionRepository sessionRepository;
    private CharacterRepository characterRepository;
    private SessionParticipantRepository participantRepository;
    private SessionTokenService tokenService;
    private TrapRepository trapRepository;
    private TrapResolutionRepository trapResolutionRepository;
    private CharacterControlRepository controlRepository;
    private ArrayDeque<Integer> diceRolls;
    private SessionParticipant owner;
    private SessionParticipant otherPlayer;
    private SessionParticipant dm;
    private SessionPiece piece;
    private CharacterControlService characterControlService;
    private DmAccessService dmAccessService;

    @BeforeEach
    void setUp() {
        jdbcTemplate = MigratedTestDatabase.create();
        sessionRepository = new GameSessionRepository(jdbcTemplate);
        participantRepository = new SessionParticipantRepository(jdbcTemplate);
        characterRepository = new CharacterRepository(jdbcTemplate);
        pieceRepository = new SessionPieceRepository(jdbcTemplate);
        eventRepository = new GameEventRepository(jdbcTemplate, new ObjectMapper());
        trapRepository = new TrapRepository(jdbcTemplate);
        trapResolutionRepository = new TrapResolutionRepository(jdbcTemplate, new ObjectMapper());
        controlRepository = new CharacterControlRepository(jdbcTemplate);
        diceRolls = new ArrayDeque<>();
        JoinProperties properties = new JoinProperties(
                Duration.ofMinutes(10),
                8,
                8,
                5,
                "test-token-secret-at-least-32-chars"
        );
        tokenService = new SessionTokenService(properties);
        ParticipantAccessService accessService = new ParticipantAccessService(
                tokenService,
                participantRepository,
                sessionRepository
        );
        dmAccessService = new DmAccessService(
                tokenService, participantRepository, sessionRepository
        );
        SessionGridService gridService = new SessionGridService(
                sessionRepository,
                pieceRepository
        );
        service = new SessionPieceMovementService(
                accessService,
                pieceRepository,
                characterRepository,
                sessionRepository,
                eventRepository,
                gridService,
                new MovementService(),
                trapRepository,
                trapResolutionRepository,
                controlRepository,
                new SessionEventStreamService(pieceRepository),
                CLOCK
        );
        trapResolutionService = new TrapResolutionService(
                accessService,
                trapResolutionRepository,
                trapRepository,
                characterRepository,
                sessionRepository,
                eventRepository,
                pieceRepository,
                gridService,
                sides -> {
                    Integer value = diceRolls.pollFirst();
                    if (value == null || value < 1 || value > sides) {
                        throw new IllegalStateException("Tiro di test non valido per d" + sides + ".");
                    }
                    return value;
                },
                controlRepository,
                new SessionEventStreamService(pieceRepository),
                CLOCK
        );

        OffsetDateTime now = OffsetDateTime.now(CLOCK);
        sessionRepository.saveSession(new GameSession(
                SESSION_ID,
                "venue-01",
                "table-02",
                "Cripta",
                "DND",
                "Sessione di prova.",
                true,
                GameSessionStatus.ACTIVE,
                5,
                5,
                now
        ));
        owner = participant("device-owner", "Andrea", now);
        otherPlayer = participant("device-other", "Giulia", now);
        dm = new SessionParticipant(
                UUID.randomUUID(), null, SESSION_ID, "dm-device", ParticipantRole.DM,
                "Dungeon Master", ParticipantStatus.ACTIVE, now, null
        );
        participantRepository.save(owner);
        participantRepository.save(otherPlayer);
        participantRepository.save(dm);
        PlayerCharacter character = saveCharacter(owner, "Elaria", 2);
        piece = new SessionPiece(
                UUID.randomUUID(),
                SESSION_ID,
                character.characterId(),
                owner.participantId(),
                RepresentationMode.VIRTUAL,
                "B2",
                0,
                now,
                now
        );
        pieceRepository.save(piece);
        characterControlService = new CharacterControlService(
                dmAccessService,
                characterRepository,
                controlRepository,
                sessionRepository,
                eventRepository,
                new SessionEventStreamService(pieceRepository),
                CLOCK
        );
    }

    @Test
    void calcolaLeCelleDallaPosizionePersistitaEDallaVelocitaDelPersonaggio() {
        var result = service.reachableCells(
                SESSION_ID,
                piece.sessionPieceId(),
                bearer(owner)
        );

        assertThat(result.currentCell()).isEqualTo("B2");
        assertThat(result.movementPoints()).isEqualTo(2);
        assertThat(result.version()).isZero();
        assertThat(result.reachableCells())
                .extracting(cell -> cell.position().toCell())
                .contains("B2", "C2", "D2");
    }

    @Test
    void spostaLaPedinaIncrementaLaVersioneESalvaUnEventoIdempotente() {
        UUID commandId = UUID.fromString("10000000-0000-0000-0000-000000000001");

        var first = service.move(
                SESSION_ID,
                piece.sessionPieceId(),
                bearer(owner),
                new MoveSessionPieceCommand("C2", 0L, commandId)
        );
        var replay = service.move(
                SESSION_ID,
                piece.sessionPieceId(),
                bearer(owner),
                new MoveSessionPieceCommand("C2", 0L, commandId)
        );

        assertThat(first.from()).isEqualTo("B2");
        assertThat(first.to()).isEqualTo("C2");
        assertThat(first.path()).containsExactly("B2", "C2");
        assertThat(first.cost()).isEqualTo(1);
        assertThat(first.version()).isEqualTo(1);
        assertThat(first.visibleTrapsOnPath()).isEmpty();
        assertThat(replay).isEqualTo(first);
        assertThat(pieceRepository.findByParticipant(SESSION_ID, owner.participantId()))
                .singleElement()
                .satisfies(updated -> {
                    assertThat(updated.currentCell()).isEqualTo("C2");
                    assertThat(updated.version()).isEqualTo(1);
                });
        assertThat(eventRepository.findBySessionId(SESSION_ID))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.eventType()).isEqualTo("MOVE_CONFIRMED");
                    assertThat(event.source()).isEqualTo("BACKEND");
                    assertThat(event.sequenceNumber()).isEqualTo(1);
                    assertThat(event.payload().get("commandId").asText())
                            .isEqualTo(commandId.toString());
                });
        assertThat(jdbcTemplate.queryForObject("""
                SELECT server_event_sequence
                FROM game_schema.game_sessions
                WHERE session_id = ?
                """, Long.class, SESSION_ID)).isEqualTo(1L);
    }

    @Test
    void interrompeIlMovimentoAllaTrappolaERiprendeDopoUnTiroRiuscito() {
        saveTrap(
                "trap-persistent", "C2", TrapDefinition.LifecyclePolicy.PERSISTENT,
                10, "1d6", TrapDefinition.DamagePolicy.NONE,
                TrapDefinition.MovementDecision.CONTINUE,
                TrapDefinition.DamagePolicy.FULL,
                TrapDefinition.MovementDecision.STOP
        );
        UUID moveCommandId = UUID.fromString("20000000-0000-0000-0000-000000000001");

        var interrupted = service.move(
                SESSION_ID,
                piece.sessionPieceId(),
                bearer(owner),
                new MoveSessionPieceCommand("D2", 0L, moveCommandId)
        );

        assertThat(interrupted.status()).isEqualTo(it.uniupo.boardhub.eventservice.model.piece.PieceMoveResult.Status.TRAP_PENDING);
        assertThat(interrupted.to()).isEqualTo("C2");
        assertThat(interrupted.requestedDestination()).isEqualTo("D2");
        assertThat(interrupted.movementRemaining()).isEqualTo(1);
        assertThat(interrupted.resolutionId()).isNotNull();
        assertThat(service.move(
                SESSION_ID,
                piece.sessionPieceId(),
                bearer(owner),
                new MoveSessionPieceCommand("D2", 0L, moveCommandId)
        )).isEqualTo(interrupted);
        assertThat(trapRepository.findBySessionAndCell(SESSION_ID, "C2"))
                .get()
                .extracting(TrapDefinition::lifecycleState)
                .isEqualTo(TrapDefinition.LifecycleState.TRIGGERED_ACTIVE);

        diceRolls.add(12);
        UUID rollCommandId = UUID.fromString("20000000-0000-0000-0000-000000000002");
        var roll = trapResolutionService.roll(
                SESSION_ID,
                interrupted.resolutionId(),
                bearer(owner),
                new RollTrapSaveCommand(0L, rollCommandId)
        );

        assertThat(roll.success()).isTrue();
        assertThat(roll.damageTotal()).isZero();
        assertThat(roll.status()).isEqualTo(TrapResolution.Status.CONTINUATION_ALLOWED);
        assertThat(trapResolutionService.roll(
                SESSION_ID,
                interrupted.resolutionId(),
                bearer(owner),
                new RollTrapSaveCommand(0L, rollCommandId)
        )).isEqualTo(roll);

        UUID continueCommandId = UUID.fromString("20000000-0000-0000-0000-000000000003");
        var completed = trapResolutionService.continueMovement(
                SESSION_ID,
                interrupted.resolutionId(),
                bearer(owner),
                new ContinueTrapMovementCommand(1L, continueCommandId)
        );

        assertThat(completed.status()).isEqualTo(it.uniupo.boardhub.eventservice.model.piece.PieceMoveResult.Status.CONFIRMED);
        assertThat(completed.from()).isEqualTo("C2");
        assertThat(completed.to()).isEqualTo("D2");
        assertThat(completed.version()).isEqualTo(2);
        assertThat(trapResolutionService.continueMovement(
                SESSION_ID,
                interrupted.resolutionId(),
                bearer(owner),
                new ContinueTrapMovementCommand(1L, continueCommandId)
        )).isEqualTo(completed);
        assertThat(pieceRepository.findByParticipant(SESSION_ID, owner.participantId()))
                .singleElement()
                .extracting(SessionPiece::currentCell)
                .isEqualTo("D2");
        assertThat(eventRepository.findBySessionId(SESSION_ID))
                .extracting(event -> event.eventType())
                .containsExactly(
                        "TRAP_TRIGGERED",
                        "TRAP_ROLL_RESOLVED",
                        "MOVE_CONFIRMED",
                        "TRAP_RESOLUTION_COMPLETED"
                );
    }

    @Test
    void applicaIlDannoUnaSolaVoltaEBloccaIlPersonaggioAtterrato() {
        saveTrap(
                "trap-lethal", "C2", TrapDefinition.LifecyclePolicy.ONE_SHOT,
                20, "3d6", TrapDefinition.DamagePolicy.NONE,
                TrapDefinition.MovementDecision.CONTINUE,
                TrapDefinition.DamagePolicy.FULL,
                TrapDefinition.MovementDecision.CONTINUE
        );
        var interrupted = service.move(
                SESSION_ID,
                piece.sessionPieceId(),
                bearer(owner),
                new MoveSessionPieceCommand("D2", 0L, UUID.randomUUID())
        );
        diceRolls.addAll(List.of(1, 6, 6, 6));
        UUID rollCommandId = UUID.fromString("30000000-0000-0000-0000-000000000001");

        var first = trapResolutionService.roll(
                SESSION_ID,
                interrupted.resolutionId(),
                bearer(owner),
                new RollTrapSaveCommand(0L, rollCommandId)
        );
        var replay = trapResolutionService.roll(
                SESSION_ID,
                interrupted.resolutionId(),
                bearer(owner),
                new RollTrapSaveCommand(0L, rollCommandId)
        );

        assertThat(first.success()).isFalse();
        assertThat(first.damageRolls()).containsExactly(6, 6, 6);
        assertThat(first.damageTotal()).isEqualTo(18);
        assertThat(first.hpCurrent()).isZero();
        assertThat(first.tacticalStatus()).isEqualTo(
                it.uniupo.boardhub.eventservice.model.trap.TrapRollResult.CharacterTacticalOutcome.DOWNED
        );
        assertThat(first.movementDecision()).isEqualTo(TrapDefinition.MovementDecision.STOP);
        assertThat(first.status()).isEqualTo(TrapResolution.Status.COMPLETED);
        assertThat(replay).isEqualTo(first);
        assertThat(diceRolls).isEmpty();
        assertThat(characterRepository.findByIdAndSession(piece.characterId(), SESSION_ID))
                .get()
                .satisfies(character -> {
                    assertThat(character.hpCurrent()).isZero();
                    assertThat(character.tacticalStatus()).isEqualTo(CharacterTacticalStatus.DOWNED);
                    assertThat(character.version()).isEqualTo(1);
                });
        assertThat(trapRepository.findBySessionAndCell(SESSION_ID, "C2"))
                .get()
                .extracting(TrapDefinition::lifecycleState)
                .isEqualTo(TrapDefinition.LifecycleState.SPENT);
    }

    @Test
    void rifiutaVersioneObsoletaSenzaMutareLaPedina() {
        service.move(
                SESSION_ID,
                piece.sessionPieceId(),
                bearer(owner),
                new MoveSessionPieceCommand("C2", 0L, UUID.randomUUID())
        );

        assertThatThrownBy(() -> service.move(
                SESSION_ID,
                piece.sessionPieceId(),
                bearer(owner),
                new MoveSessionPieceCommand("D2", 0L, UUID.randomUUID())
        )).isInstanceOf(StalePieceStateException.class)
                .hasMessageContaining("versione 1");
        assertThat(pieceRepository.findByParticipant(SESSION_ID, owner.participantId()))
                .singleElement()
                .extracting(SessionPiece::currentCell)
                .isEqualTo("C2");
    }

    @Test
    void rifiutaIlRiusoDelCommandIdConUnMovimentoDiverso() {
        UUID commandId = UUID.fromString("10000000-0000-0000-0000-000000000002");
        service.move(
                SESSION_ID,
                piece.sessionPieceId(),
                bearer(owner),
                new MoveSessionPieceCommand("C2", 0L, commandId)
        );

        assertThatThrownBy(() -> service.move(
                SESSION_ID,
                piece.sessionPieceId(),
                bearer(owner),
                new MoveSessionPieceCommand("D2", 1L, commandId)
        )).isInstanceOf(MoveCommandConflictException.class);
        assertThat(pieceRepository.findByParticipant(SESSION_ID, owner.participantId()))
                .singleElement()
                .satisfies(updated -> {
                    assertThat(updated.currentCell()).isEqualTo("C2");
                    assertThat(updated.version()).isEqualTo(1);
                });
        assertThat(eventRepository.findBySessionId(SESSION_ID)).hasSize(1);
    }

    @Test
    void rifiutaDestinazioniBloccateONonRaggiungibili() {
        sessionRepository.saveCell(new GridCellState(
                SESSION_ID,
                "C2",
                TerrainType.OBSTACLE,
                null
        ));

        assertThatThrownBy(() -> service.move(
                SESSION_ID,
                piece.sessionPieceId(),
                bearer(owner),
                new MoveSessionPieceCommand("C2", 0L, UUID.randomUUID())
        )).isInstanceOf(MoveRejectedException.class)
                .hasMessageContaining("non e raggiungibile");
        assertThat(eventRepository.findBySessionId(SESSION_ID)).isEmpty();
    }

    @Test
    void nonEsponeNeConsenteDiMuoverePedineAltrui() {
        assertThatThrownBy(() -> service.reachableCells(
                SESSION_ID,
                piece.sessionPieceId(),
                bearer(otherPlayer)
        )).isInstanceOf(SessionPieceNotFoundException.class);
        assertThatThrownBy(() -> service.move(
                SESSION_ID,
                piece.sessionPieceId(),
                bearer(otherPlayer),
                new MoveSessionPieceCommand("C2", 0L, UUID.randomUUID())
        )).isInstanceOf(SessionPieceNotFoundException.class);
    }

    @Test
    void ilDmAssumeIlControlloMuoveERestituisceIlPersonaggio() {
        String dmAuthorization = "Bearer " + tokenService.issueDm(dm);
        var control = characterControlService.assume(
                SESSION_ID, piece.characterId(), dmAuthorization
        );
        var replayedControl = characterControlService.assume(
                SESSION_ID, piece.characterId(), dmAuthorization
        );

        assertThat(control.dmParticipantId()).isEqualTo(dm.participantId());
        assertThat(replayedControl.version()).isEqualTo(control.version());
        assertThatThrownBy(() -> service.reachableCells(
                SESSION_ID, piece.sessionPieceId(), bearer(owner)
        )).isInstanceOf(CharacterControlConflictException.class);
        assertThatThrownBy(() -> service.move(
                SESSION_ID,
                piece.sessionPieceId(),
                bearer(owner),
                new MoveSessionPieceCommand("C2", 0L, UUID.randomUUID())
        )).isInstanceOf(CharacterControlConflictException.class);

        assertThat(service.reachableCellsAsDm(
                SESSION_ID, piece.sessionPieceId(), dm.participantId()
        ).currentCell()).isEqualTo("B2");
        var moved = service.moveAsDm(
                SESSION_ID,
                piece.sessionPieceId(),
                dm.participantId(),
                new MoveSessionPieceCommand("C2", 0L, UUID.randomUUID())
        );
        assertThat(moved.to()).isEqualTo("C2");

        characterControlService.release(SESSION_ID, piece.characterId(), dmAuthorization);
        characterControlService.release(SESSION_ID, piece.characterId(), dmAuthorization);
        assertThat(service.reachableCells(
                SESSION_ID, piece.sessionPieceId(), bearer(owner)
        ).currentCell()).isEqualTo("C2");
        assertThat(eventRepository.findBySessionId(SESSION_ID))
                .extracting(GameEvent::eventType)
                .containsExactly(
                        "CHARACTER_CONTROL_ASSUMED",
                        "MOVE_CONFIRMED",
                        "CHARACTER_CONTROL_RELEASED"
                );
        assertThat(eventRepository.findBySessionId(SESSION_ID))
                .allSatisfy(event -> {
                    assertThat(event.payload().path("actorRole").asText()).isEqualTo("DM");
                    assertThat(event.payload().path("actorId").asText())
                            .isEqualTo(dm.participantId().toString());
                });
    }

    private SessionParticipant participant(
            String reference,
            String displayName,
            OffsetDateTime joinedAt
    ) {
        return new SessionParticipant(
                UUID.randomUUID(),
                null,
                SESSION_ID,
                reference,
                ParticipantRole.PLAYER,
                displayName,
                ParticipantStatus.ACTIVE,
                joinedAt,
                null
        );
    }

    private PlayerCharacter saveCharacter(
            SessionParticipant participant,
            String name,
            int speedCells
    ) {
        OffsetDateTime now = OffsetDateTime.now(CLOCK);
        PlayerCharacter character = new PlayerCharacter(
                UUID.randomUUID(),
                SESSION_ID,
                participant.participantId(),
                name,
                "Elfa",
                120,
                "Maga",
                3,
                speedCells,
                18,
                18,
                12,
                PartyVisibility.OWNER_ONLY,
                0,
                now,
                now
        );
        characterRepository.save(character);
        return character;
    }

    private void saveTrap(
            String trapId,
            String cell,
            TrapDefinition.LifecyclePolicy lifecyclePolicy,
            int saveDc,
            String damageExpression,
            TrapDefinition.DamagePolicy successDamage,
            TrapDefinition.MovementDecision successMovement,
            TrapDefinition.DamagePolicy failureDamage,
            TrapDefinition.MovementDecision failureMovement
    ) {
        sessionRepository.saveCell(new GridCellState(
                SESSION_ID, "C1", TerrainType.OBSTACLE, null
        ));
        sessionRepository.saveCell(new GridCellState(
                SESSION_ID, "C3", TerrainType.OBSTACLE, null
        ));
        sessionRepository.saveTrap(new GridTrapState(
                SESSION_ID,
                trapId,
                cell,
                TrapVisibility.HIDDEN,
                true,
                lifecyclePolicy,
                TrapDefinition.LifecycleState.ARMED,
                TrapDefinition.SaveAbility.DEXTERITY,
                saveDc,
                TrapDefinition.RollMode.NORMAL,
                damageExpression,
                successDamage,
                successMovement,
                failureDamage,
                failureMovement,
                0,
                OffsetDateTime.now(CLOCK)
        ));
    }

    private String bearer(SessionParticipant participant) {
        return "Bearer " + tokenService.issuePlayer(participant);
    }
}
