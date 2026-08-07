package it.uniupo.boardhub.eventservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.boardhub.eventservice.config.JoinProperties;
import it.uniupo.boardhub.eventservice.model.character.PartyVisibility;
import it.uniupo.boardhub.eventservice.model.character.PlayerCharacter;
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
import it.uniupo.boardhub.eventservice.repository.CharacterRepository;
import it.uniupo.boardhub.eventservice.repository.GameEventRepository;
import it.uniupo.boardhub.eventservice.repository.GameSessionRepository;
import it.uniupo.boardhub.eventservice.repository.SessionParticipantRepository;
import it.uniupo.boardhub.eventservice.repository.SessionPieceRepository;
import it.uniupo.boardhub.eventservice.service.command.MoveSessionPieceCommand;
import it.uniupo.boardhub.eventservice.service.exception.MoveCommandConflictException;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionPieceMovementServiceTest {

    private static final String SESSION_ID = "session-move-001";
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-07-29T10:00:00Z"), ZoneOffset.UTC);

    private JdbcTemplate jdbcTemplate;
    private SessionPieceMovementService service;
    private SessionPieceRepository pieceRepository;
    private GameEventRepository eventRepository;
    private GameSessionRepository sessionRepository;
    private CharacterRepository characterRepository;
    private SessionParticipantRepository participantRepository;
    private SessionTokenService tokenService;
    private SessionParticipant owner;
    private SessionParticipant otherPlayer;
    private SessionPiece piece;

    @BeforeEach
    void setUp() {
        jdbcTemplate = MigratedTestDatabase.create();
        sessionRepository = new GameSessionRepository(jdbcTemplate);
        participantRepository = new SessionParticipantRepository(jdbcTemplate);
        characterRepository = new CharacterRepository(jdbcTemplate);
        pieceRepository = new SessionPieceRepository(jdbcTemplate);
        eventRepository = new GameEventRepository(jdbcTemplate, new ObjectMapper());
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
        participantRepository.save(owner);
        participantRepository.save(otherPlayer);
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
        sessionRepository.saveTrap(new GridTrapState(
                SESSION_ID,
                "trap-hidden",
                "C2",
                TrapVisibility.HIDDEN,
                true
        ));

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

    private String bearer(SessionParticipant participant) {
        return "Bearer " + tokenService.issuePlayer(participant);
    }
}
