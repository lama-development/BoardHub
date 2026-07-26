package it.uniupo.boardhub.eventservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

// Limiti e credenziali locali del flusso di ingresso alle sessioni.
@ConfigurationProperties(prefix = "boardhub.join")
public record JoinProperties(
        Duration requestTtl,
        int maxPlayersPerSession,
        int maxActiveTables,
        int maxRequestsPerMinute,
        String dmAccessKey,
        String sessionTokenSecret
) {
}
