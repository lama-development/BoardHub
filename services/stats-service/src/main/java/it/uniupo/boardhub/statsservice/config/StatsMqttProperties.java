package it.uniupo.boardhub.statsservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "boardhub.stats.mqtt")
public record StatsMqttProperties(
        String brokerUri,
        String clientId,
        String topic,
        int qos
) {
}
