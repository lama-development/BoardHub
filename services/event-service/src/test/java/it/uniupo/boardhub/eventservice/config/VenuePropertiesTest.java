package it.uniupo.boardhub.eventservice.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VenuePropertiesTest {

    @Test
    void acceptsCoherentVenueConfiguration() {
        assertDoesNotThrow(() ->
                new VenueProperties(Duration.ofMinutes(10), 60, "venue-test-key", true)
        );
    }

    @Test
    void rejectsInvalidClaimDurations() {
        assertThrows(IllegalArgumentException.class, () ->
                new VenueProperties(Duration.ZERO, 60, "venue-test-key", true)
        );
        assertThrows(IllegalArgumentException.class, () ->
                new VenueProperties(Duration.ofMinutes(61), 60, "venue-test-key", true)
        );
    }

    @Test
    void rejectsInvalidLimitOrWeakAdminKey() {
        assertThrows(IllegalArgumentException.class, () ->
                new VenueProperties(Duration.ofMinutes(10), 0, "venue-test-key", true)
        );
        assertThrows(IllegalArgumentException.class, () ->
                new VenueProperties(Duration.ofMinutes(10), 60, "short", true)
        );
    }
}
