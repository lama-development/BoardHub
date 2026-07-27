package it.uniupo.boardhub.eventservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Limiti tecnici applicati ai personaggi creati durante una sessione.
@ConfigurationProperties(prefix = "boardhub.characters")
public record CharacterProperties(
        int maxPerParticipant
) {
}
