package it.uniupo.boardhub.statsservice.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Tournament(
        UUID tournamentId,
        String name,
        String gameType,
        String venueId,
        OffsetDateTime createdAt
) {
}
