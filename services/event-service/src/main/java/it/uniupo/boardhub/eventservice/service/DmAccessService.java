package it.uniupo.boardhub.eventservice.service;

import it.uniupo.boardhub.eventservice.config.JoinProperties;
import it.uniupo.boardhub.eventservice.service.exception.DmAuthenticationException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class DmAccessService {

    private final byte[] expectedKey;

    public DmAccessService(JoinProperties properties) {
        if (properties.dmAccessKey() == null || properties.dmAccessKey().isBlank()) {
            throw new IllegalStateException("boardhub.join.dm-access-key e obbligatoria.");
        }
        this.expectedKey = properties.dmAccessKey().getBytes(StandardCharsets.UTF_8);
    }

    // Confronta la chiave in tempo costante prima di eseguire operazioni riservate al DM.
    public void requireAuthorized(String suppliedKey) {
        byte[] supplied = suppliedKey == null
                ? new byte[0]
                : suppliedKey.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expectedKey, supplied)) {
            throw new DmAuthenticationException();
        }
    }
}
