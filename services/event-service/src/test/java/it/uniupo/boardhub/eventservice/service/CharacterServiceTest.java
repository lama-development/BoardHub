package it.uniupo.boardhub.eventservice.service;

import it.uniupo.boardhub.eventservice.config.CharacterProperties;
import it.uniupo.boardhub.eventservice.config.JoinProperties;
import it.uniupo.boardhub.eventservice.model.character.PartyVisibility;
import it.uniupo.boardhub.eventservice.model.join.ParticipantRole;
import it.uniupo.boardhub.eventservice.model.join.ParticipantStatus;
import it.uniupo.boardhub.eventservice.model.join.SessionParticipant;
import it.uniupo.boardhub.eventservice.model.session.GameSession;
import it.uniupo.boardhub.eventservice.model.session.GameSessionStatus;
import it.uniupo.boardhub.eventservice.repository.CharacterRepository;
import it.uniupo.boardhub.eventservice.repository.GameSessionRepository;
import it.uniupo.boardhub.eventservice.repository.SessionParticipantRepository;
import it.uniupo.boardhub.eventservice.service.command.CreateCharacterCommand;
import it.uniupo.boardhub.eventservice.service.exception.CharacterCapacityException;
import it.uniupo.boardhub.eventservice.service.exception.PlayerAuthenticationException;
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

class CharacterServiceTest {

    private static final String SESSION_ID = "session-characters-001";
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-07-26T10:00:00Z"), ZoneOffset.UTC);

    private CharacterService service;
    private GameSessionRepository sessionRepository;
    private SessionTokenService tokenService;
    private SessionParticipant owner;
    private SessionParticipant otherPlayer;

    @BeforeEach
    void setUp() {
        JdbcTemplate jdbcTemplate = MigratedTestDatabase.create();
        sessionRepository = new GameSessionRepository(jdbcTemplate);
        SessionParticipantRepository participantRepository =
                new SessionParticipantRepository(jdbcTemplate);
        CharacterRepository characterRepository = new CharacterRepository(jdbcTemplate);
        JoinProperties joinProperties = new JoinProperties(
                Duration.ofMinutes(10),
                8,
                8,
                5,
                "test-token-secret-at-least-32-chars"
        );
        tokenService = new SessionTokenService(joinProperties);
        ParticipantAccessService accessService = new ParticipantAccessService(
                tokenService, participantRepository, sessionRepository
        );
        service = new CharacterService(
                accessService,
                characterRepository,
                sessionRepository,
                new CharacterProperties(2),
                CLOCK
        );

        OffsetDateTime now = OffsetDateTime.now(CLOCK);
        sessionRepository.saveSession(new GameSession(
                SESSION_ID,
                "venue-01",
                "table-04",
                "Cripta",
                "DND",
                "Sessione di prova.",
                true,
                GameSessionStatus.ACTIVE,
                5,
                5,
                now
        ));
        owner = participant(SESSION_ID, "device-owner", "Andrea", now);
        otherPlayer = participant(SESSION_ID, "device-other", "Giulia", now);
        participantRepository.save(owner);
        participantRepository.save(otherPlayer);
    }

    @Test
    void creaNormalizzaEIsolaIPersonaggiPerProprietario() {
        var created = service.create(
                SESSION_ID,
                bearer(owner),
                command("  Elaria  ", null, null, "party")
        );

        assertThat(created.name()).isEqualTo("Elaria");
        assertThat(created.age()).isNull();
        assertThat(created.hpCurrent()).isEqualTo(18);
        assertThat(created.partyVisibility()).isEqualTo(PartyVisibility.PARTY);
        assertThat(created.version()).isZero();
        assertThat(created.createdAt()).isEqualTo(OffsetDateTime.now(CLOCK));
        assertThat(service.listOwned(SESSION_ID, bearer(owner)))
                .extracting(item -> item.name())
                .containsExactly("Elaria");
        assertThat(service.listOwned(SESSION_ID, bearer(otherPlayer))).isEmpty();
        assertThat(service.listForDm(SESSION_ID))
                .extracting(item -> item.name())
                .containsExactly("Elaria");
    }

    @Test
    void applicaLimiteEValidazioniDiDominio() {
        service.create(SESSION_ID, bearer(owner), command("Elaria", 120, 12, null));
        service.create(SESSION_ID, bearer(owner), command("Borin", 54, 10, "DM_ONLY"));

        assertThatThrownBy(() -> service.create(
                SESSION_ID, bearer(owner), command("Terzo", 30, 8, null)
        )).isInstanceOf(CharacterCapacityException.class);
        assertThatThrownBy(() -> service.create(
                SESSION_ID,
                bearer(otherPlayer),
                new CreateCharacterCommand(
                        "Errore", "Umano", 30, "Guerriero",
                        0, 6, 12, 10, 15, "OWNER_ONLY"
                )
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("level");
        assertThatThrownBy(() -> service.create(
                SESSION_ID,
                bearer(otherPlayer),
                new CreateCharacterCommand(
                        "Errore", "Umano", 30, "Guerriero",
                        2, 6, 11, 10, 15, "OWNER_ONLY"
                )
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hpCurrent");
        assertThatThrownBy(() -> service.create(
                SESSION_ID,
                bearer(otherPlayer),
                command("Errore", 0, 10, null)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("age");
    }

    @Test
    void rifiutaTokenNonValidoESessioneConclusa() {
        assertThatThrownBy(() -> service.listOwned(SESSION_ID, "Bearer token-alterato"))
                .isInstanceOf(PlayerAuthenticationException.class);

        sessionRepository.endSession(SESSION_ID);

        assertThatThrownBy(() -> service.listOwned(SESSION_ID, bearer(owner)))
                .isInstanceOf(PlayerAuthenticationException.class);
        assertThatThrownBy(() -> service.create(
                SESSION_ID, bearer(owner), command("Elaria", 120, 12, null)
        )).isInstanceOf(PlayerAuthenticationException.class);
    }

    private SessionParticipant participant(
            String sessionId,
            String reference,
            String displayName,
            OffsetDateTime joinedAt
    ) {
        return new SessionParticipant(
                UUID.randomUUID(),
                null,
                sessionId,
                reference,
                ParticipantRole.PLAYER,
                displayName,
                ParticipantStatus.ACTIVE,
                joinedAt,
                null
        );
    }

    private String bearer(SessionParticipant participant) {
        return "Bearer " + tokenService.issuePlayer(participant);
    }

    private CreateCharacterCommand command(
            String name,
            Integer age,
            Integer hpCurrent,
            String visibility
    ) {
        return new CreateCharacterCommand(
                name,
                "Elfa",
                age,
                "Maga",
                3,
                6,
                hpCurrent,
                18,
                12,
                visibility
        );
    }
}
