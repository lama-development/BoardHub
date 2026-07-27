package it.uniupo.boardhub.eventservice.service;

import it.uniupo.boardhub.eventservice.config.JoinProperties;
import it.uniupo.boardhub.eventservice.model.join.ParticipantRole;
import it.uniupo.boardhub.eventservice.model.join.SessionParticipant;
import it.uniupo.boardhub.eventservice.service.exception.PlayerAuthenticationException;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

@Service
public class SessionTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String PLAYER_TOKEN_VERSION = "bhp1";
    private static final String DM_TOKEN_VERSION = "bhd1";

    private final byte[] secret;

    public SessionTokenService(JoinProperties properties) {
        if (properties.sessionTokenSecret() == null || properties.sessionTokenSecret().length() < 32) {
            throw new IllegalStateException(
                    "boardhub.join.session-token-secret deve contenere almeno 32 caratteri."
            );
        }
        this.secret = properties.sessionTokenSecret().getBytes(StandardCharsets.UTF_8);
    }

    // Genera la credenziale firmata di un giocatore accettato.
    public String issuePlayer(SessionParticipant participant) {
        return issue(participant, ParticipantRole.PLAYER, PLAYER_TOKEN_VERSION);
    }

    // Genera una credenziale distinta e limitata al DM della singola sessione.
    public String issueDm(SessionParticipant participant) {
        return issue(participant, ParticipantRole.DM, DM_TOKEN_VERSION);
    }

    public UUID verifyPlayer(String token, String expectedSessionId) {
        return verify(token, expectedSessionId, ParticipantRole.PLAYER, PLAYER_TOKEN_VERSION);
    }

    public UUID verifyDm(String token, String expectedSessionId) {
        return verify(token, expectedSessionId, ParticipantRole.DM, DM_TOKEN_VERSION);
    }

    private String issue(SessionParticipant participant, ParticipantRole expectedRole, String version) {
        if (participant.role() != expectedRole) {
            throw new IllegalArgumentException("Ruolo del partecipante incompatibile con il token richiesto.");
        }
        String subject = subject(version, participant.participantId(), participant.sessionId(), expectedRole);
        return version + "." + participant.participantId() + "." + sign(subject);
    }

    // Verifica versione, struttura, ruolo e firma rispetto alla sessione richiesta.
    private UUID verify(
            String token,
            String expectedSessionId,
            ParticipantRole expectedRole,
            String version
    ) {
        if (token == null || token.isBlank() || expectedSessionId == null || expectedSessionId.isBlank()) {
            throw new PlayerAuthenticationException();
        }

        String[] parts = token.trim().split("\\.", -1);
        if (parts.length != 3 || !version.equals(parts[0])) {
            throw new PlayerAuthenticationException();
        }

        UUID participantId;
        try {
            participantId = UUID.fromString(parts[1]);
        } catch (IllegalArgumentException ex) {
            throw new PlayerAuthenticationException();
        }

        String expectedSignature = sign(subject(
                version,
                participantId,
                expectedSessionId.trim(),
                expectedRole
        ));
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.US_ASCII),
                parts[2].getBytes(StandardCharsets.US_ASCII)
        )) {
            throw new PlayerAuthenticationException();
        }
        return participantId;
    }

    private String subject(String version, UUID participantId, String sessionId, ParticipantRole role) {
        return version + ":" + participantId + ":" + sessionId + ":" + role.name();
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Impossibile generare la credenziale di sessione.", ex);
        }
    }
}
