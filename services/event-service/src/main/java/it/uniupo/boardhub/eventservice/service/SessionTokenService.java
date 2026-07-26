package it.uniupo.boardhub.eventservice.service;

import it.uniupo.boardhub.eventservice.config.JoinProperties;
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
    private static final String TOKEN_VERSION = "bhp1";

    private final byte[] secret;

    public SessionTokenService(JoinProperties properties) {
        if (properties.sessionTokenSecret() == null || properties.sessionTokenSecret().length() < 32) {
            throw new IllegalStateException(
                    "boardhub.join.session-token-secret deve contenere almeno 32 caratteri."
            );
        }
        this.secret = properties.sessionTokenSecret().getBytes(StandardCharsets.UTF_8);
    }

    // Genera una credenziale firmata stabile, riproducibile anche dopo un retry di accept.
    public String issue(SessionParticipant participant) {
        String subject = participant.participantId() + ":" + participant.sessionId();
        return TOKEN_VERSION + "." + participant.participantId() + "." + sign(subject);
    }

    // Verifica versione, struttura e firma del token rispetto alla sessione richiesta.
    public UUID verify(String token, String expectedSessionId) {
        if (token == null || token.isBlank() || expectedSessionId == null || expectedSessionId.isBlank()) {
            throw new PlayerAuthenticationException();
        }

        String[] parts = token.trim().split("\\.", -1);
        if (parts.length != 3 || !TOKEN_VERSION.equals(parts[0])) {
            throw new PlayerAuthenticationException();
        }

        UUID participantId;
        try {
            participantId = UUID.fromString(parts[1]);
        } catch (IllegalArgumentException ex) {
            throw new PlayerAuthenticationException();
        }

        String expectedSignature = sign(participantId + ":" + expectedSessionId.trim());
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.US_ASCII),
                parts[2].getBytes(StandardCharsets.US_ASCII)
        )) {
            throw new PlayerAuthenticationException();
        }
        return participantId;
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
