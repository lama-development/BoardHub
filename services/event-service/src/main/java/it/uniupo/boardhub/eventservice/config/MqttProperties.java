package it.uniupo.boardhub.eventservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "boardhub.mqtt")
// Parametri MQTT letti da application.yml.
public record MqttProperties(
        String brokerUri,
        String clientId,
        String topic,
        int qos
) {
}
