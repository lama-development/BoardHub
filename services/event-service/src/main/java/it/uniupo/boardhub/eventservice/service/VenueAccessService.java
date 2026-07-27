package it.uniupo.boardhub.eventservice.service;

import it.uniupo.boardhub.eventservice.config.VenueProperties;
import it.uniupo.boardhub.eventservice.service.exception.VenueAuthenticationException;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class VenueAccessService {

    private final byte[] expectedKey;
    private final boolean localOnly;

    public VenueAccessService(VenueProperties properties) {
        if (properties.adminAccessKey() == null || properties.adminAccessKey().isBlank()) {
            throw new IllegalStateException("boardhub.venue.admin-access-key e obbligatoria.");
        }
        this.expectedKey = properties.adminAccessKey().getBytes(StandardCharsets.UTF_8);
        this.localOnly = properties.adminLocalOnly();
    }

    // Protegge le operazioni del locale e, nel prototipo, le limita al computer del backend.
    public void requireAuthorized(String suppliedKey, String remoteAddress) {
        if (localOnly && !isLoopback(remoteAddress)) {
            throw new VenueAuthenticationException();
        }
        byte[] supplied = suppliedKey == null
                ? new byte[0]
                : suppliedKey.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expectedKey, supplied)) {
            throw new VenueAuthenticationException();
        }
    }

    private boolean isLoopback(String remoteAddress) {
        try {
            return remoteAddress != null && InetAddress.getByName(remoteAddress).isLoopbackAddress();
        } catch (Exception ex) {
            return false;
        }
    }
}
