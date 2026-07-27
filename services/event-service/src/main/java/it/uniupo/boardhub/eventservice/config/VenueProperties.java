package it.uniupo.boardhub.eventservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

// Configurazione della console privata con cui il locale abilita i tavoli.
@ConfigurationProperties(prefix = "boardhub.venue")
public record VenueProperties(
        Duration tableClaimTtl,
        int maxClaimMinutes,
        String adminAccessKey,
        boolean adminLocalOnly
) {
    public VenueProperties {
        if (tableClaimTtl == null || tableClaimTtl.isZero() || tableClaimTtl.isNegative()) {
            throw new IllegalArgumentException(
                    "boardhub.venue.table-claim-ttl deve essere una durata positiva."
            );
        }
        if (maxClaimMinutes < 1) {
            throw new IllegalArgumentException(
                    "boardhub.venue.max-claim-minutes deve essere almeno 1."
            );
        }
        if (tableClaimTtl.compareTo(Duration.ofMinutes(maxClaimMinutes)) > 0) {
            throw new IllegalArgumentException(
                    "boardhub.venue.table-claim-ttl non puo superare max-claim-minutes."
            );
        }
        if (adminAccessKey == null || adminAccessKey.isBlank() || adminAccessKey.length() < 12) {
            throw new IllegalArgumentException(
                    "boardhub.venue.admin-access-key deve contenere almeno 12 caratteri."
            );
        }
    }
}
