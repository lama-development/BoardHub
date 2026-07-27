package it.uniupo.boardhub.eventservice.service;

import it.uniupo.boardhub.eventservice.config.JoinProperties;
import it.uniupo.boardhub.eventservice.model.join.ParticipantRole;
import it.uniupo.boardhub.eventservice.model.join.ParticipantStatus;
import it.uniupo.boardhub.eventservice.model.join.SessionParticipant;
import it.uniupo.boardhub.eventservice.service.exception.PlayerAuthenticationException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SessionTokenServiceTest {

    private final SessionTokenService tokenService = new SessionTokenService(
            new JoinProperties(
                    Duration.ofMinutes(10), 8, 8, 5,
                    "test-token-secret-at-least-32-chars"
            )
    );

    @Test
    void verificaTokenFirmatoPerLaSessioneCorretta() {
        SessionParticipant participant = participant();
        String token = tokenService.issuePlayer(participant);

        assertEquals(
                participant.participantId(),
                tokenService.verifyPlayer(token, participant.sessionId())
        );
    }

    @Test
    void rifiutaTokenAlteratoSessioneDiversaEFormatoNonValido() {
        SessionParticipant participant = participant();
        String token = tokenService.issuePlayer(participant);

        assertThrows(
                PlayerAuthenticationException.class,
                () -> tokenService.verifyPlayer(token + "alterato", participant.sessionId())
        );
        assertThrows(
                PlayerAuthenticationException.class,
                () -> tokenService.verifyPlayer(token, "sessione-diversa")
        );
        assertThrows(
                PlayerAuthenticationException.class,
                () -> tokenService.verifyPlayer("token-non-valido", participant.sessionId())
        );
    }

    private SessionParticipant participant() {
        return new SessionParticipant(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "session-token-001",
                "device-token-001",
                ParticipantRole.PLAYER,
                "Andrea",
                ParticipantStatus.ACTIVE,
                OffsetDateTime.parse("2026-07-25T10:00:00Z"),
                null
        );
    }
}
