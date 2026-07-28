package it.uniupo.boardhub.eventservice.service;

import it.uniupo.boardhub.eventservice.config.JoinProperties;
import it.uniupo.boardhub.eventservice.model.character.PartyVisibility;
import it.uniupo.boardhub.eventservice.model.character.PlayerCharacter;
import it.uniupo.boardhub.eventservice.model.grid.GridPosition;
import it.uniupo.boardhub.eventservice.model.grid.TerrainType;
import it.uniupo.boardhub.eventservice.model.join.ParticipantRole;
import it.uniupo.boardhub.eventservice.model.join.ParticipantStatus;
import it.uniupo.boardhub.eventservice.model.join.SessionParticipant;
import it.uniupo.boardhub.eventservice.model.session.GameSession;
import it.uniupo.boardhub.eventservice.model.session.GameSessionStatus;
import it.uniupo.boardhub.eventservice.model.session.GridCellState;
import it.uniupo.boardhub.eventservice.repository.CharacterRepository;
import it.uniupo.boardhub.eventservice.repository.GameSessionRepository;
import it.uniupo.boardhub.eventservice.repository.SessionParticipantRepository;
import it.uniupo.boardhub.eventservice.repository.SessionPieceRepository;
import it.uniupo.boardhub.eventservice.service.command.CreateSessionPieceCommand;
import it.uniupo.boardhub.eventservice.service.exception.CharacterNotFoundException;
import it.uniupo.boardhub.eventservice.service.exception.PlayerAuthenticationException;
import it.uniupo.boardhub.eventservice.service.exception.SessionPieceConflictException;
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

class SessionPieceServiceTest {

    private static final String SESSION_ID = "session-pieces-001";
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-07-27T14:00:00Z"), ZoneOffset.UTC);

    private SessionPieceService service;
    private SessionGridService gridService;
    private GameSessionRepository sessionRepository;
    private SessionParticipantRepository participantRepository;
    private CharacterRepository characterRepository;
    private SessionTokenService tokenService;
    private SessionParticipant owner;
    private SessionParticipant otherPlayer;

    @BeforeEach
    void setUp() {
        JdbcTemplate jdbcTemplate = MigratedTestDatabase.create();
        sessionRepository = new GameSessionRepository(jdbcTemplate);
        participantRepository = new SessionParticipantRepository(jdbcTemplate);
        characterRepository = new CharacterRepository(jdbcTemplate);
        SessionPieceRepository pieceRepository = new SessionPieceRepository(jdbcTemplate);
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
        gridService = new SessionGridService(sessionRepository, pieceRepository);
        service = new SessionPieceService(
                accessService,
                characterRepository,
                pieceRepository,
                sessionRepository,
                gridService,
                CLOCK
        );

        OffsetDateTime now = OffsetDateTime.now(CLOCK);
        sessionRepository.saveSession(new GameSession(
                SESSION_ID,
                "venue-01",
                "table-01",
                "Cripta",
                "DND",
                "Sessione di prova.",
                true,
                GameSessionStatus.ACTIVE,
                5,
                5,
                now
        ));
        sessionRepository.saveCell(new GridCellState(
                SESSION_ID,
                "C3",
                TerrainType.OBSTACLE,
                null
        ));
        owner = participant("device-owner", "Andrea", now);
        otherPlayer = participant("device-other", "Giulia", now);
        participantRepository.save(owner);
        participantRepository.save(otherPlayer);
    }

    @Test
    void creaPedinaVirtualeNormalizzaLaCellaEOccupaLaGriglia() {
        PlayerCharacter character = saveCharacter(owner, "Elaria");

        var created = service.create(
                SESSION_ID,
                bearer(owner),
                new CreateSessionPieceCommand(character.characterId(), null, " b2 ")
        );

        assertThat(created.characterId()).isEqualTo(character.characterId());
        assertThat(created.participantId()).isEqualTo(owner.participantId());
        assertThat(created.representationMode().name()).isEqualTo("VIRTUAL");
        assertThat(created.currentCell()).isEqualTo("B2");
        assertThat(created.version()).isZero();
        assertThat(created.createdAt()).isEqualTo(OffsetDateTime.now(CLOCK));
        assertThat(service.listOwned(SESSION_ID, bearer(owner)))
                .extracting(item -> item.currentCell())
                .containsExactly("B2");
        assertThat(service.listOwned(SESSION_ID, bearer(otherPlayer))).isEmpty();
        assertThat(service.listForDm(SESSION_ID))
                .extracting(item -> item.characterId())
                .containsExactly(character.characterId());
        assertThat(gridService.loadGrid(SESSION_ID).cellAt(GridPosition.fromCell("B2")).occupied())
                .isTrue();
    }

    @Test
    void impedisceDiAssociarePersonaggiAltruiODueVolteLoStessoPersonaggio() {
        PlayerCharacter ownerCharacter = saveCharacter(owner, "Elaria");
        PlayerCharacter otherCharacter = saveCharacter(otherPlayer, "Borin");

        assertThatThrownBy(() -> service.create(
                SESSION_ID,
                bearer(owner),
                new CreateSessionPieceCommand(otherCharacter.characterId(), "VIRTUAL", "A1")
        )).isInstanceOf(CharacterNotFoundException.class);

        service.create(
                SESSION_ID,
                bearer(owner),
                new CreateSessionPieceCommand(ownerCharacter.characterId(), "VIRTUAL", "A1")
        );

        assertThatThrownBy(() -> service.create(
                SESSION_ID,
                bearer(owner),
                new CreateSessionPieceCommand(ownerCharacter.characterId(), "VIRTUAL", "A2")
        )).isInstanceOf(SessionPieceConflictException.class)
                .hasMessageContaining("gia una pedina");
    }

    @Test
    void impedisceCollisioniCelleNonPercorribiliEModalitaNonSupportate() {
        PlayerCharacter first = saveCharacter(owner, "Elaria");
        PlayerCharacter second = saveCharacter(owner, "Kael");
        service.create(
                SESSION_ID,
                bearer(owner),
                new CreateSessionPieceCommand(first.characterId(), "VIRTUAL", "A1")
        );

        assertThatThrownBy(() -> service.create(
                SESSION_ID,
                bearer(owner),
                new CreateSessionPieceCommand(second.characterId(), "VIRTUAL", "A1")
        )).isInstanceOf(SessionPieceConflictException.class)
                .hasMessageContaining("non e disponibile");
        assertThatThrownBy(() -> service.create(
                SESSION_ID,
                bearer(owner),
                new CreateSessionPieceCommand(second.characterId(), "VIRTUAL", "C3")
        )).isInstanceOf(SessionPieceConflictException.class);
        assertThatThrownBy(() -> service.create(
                SESSION_ID,
                bearer(owner),
                new CreateSessionPieceCommand(second.characterId(), "VIRTUAL", "F1")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non appartiene");
        assertThatThrownBy(() -> service.create(
                SESSION_ID,
                bearer(owner),
                new CreateSessionPieceCommand(second.characterId(), "PHYSICAL", "A2")
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("VIRTUAL");
    }

    @Test
    void revocaOperazioniQuandoSessioneConclusa() {
        PlayerCharacter character = saveCharacter(owner, "Elaria");
        sessionRepository.endSession(SESSION_ID);

        assertThatThrownBy(() -> service.create(
                SESSION_ID,
                bearer(owner),
                new CreateSessionPieceCommand(character.characterId(), "VIRTUAL", "A1")
        )).isInstanceOf(PlayerAuthenticationException.class);
        assertThatThrownBy(() -> service.listOwned(SESSION_ID, bearer(owner)))
                .isInstanceOf(PlayerAuthenticationException.class);
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

    private PlayerCharacter saveCharacter(SessionParticipant participant, String name) {
        OffsetDateTime now = OffsetDateTime.now(CLOCK);
        PlayerCharacter character = new PlayerCharacter(
                UUID.randomUUID(),
                SESSION_ID,
                participant.participantId(),
                name,
                "Umano",
                30,
                "Guerriero",
                3,
                6,
                20,
                20,
                15,
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
