package it.uniupo.boardhub.eventservice.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniupo.boardhub.eventservice.config.MqttProperties;
import it.uniupo.boardhub.eventservice.model.GameEvent;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
public class MqttBoardCommandPublisher implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MqttBoardCommandPublisher.class);

    private final MqttProperties properties;
    private final ObjectMapper objectMapper;
    private MqttClient client;

    public MqttBoardCommandPublisher(MqttProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) throws MqttException {
        client = new MqttClient(
                properties.brokerUri(), properties.clientId() + "-commands", new MemoryPersistence()
        );
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        client.connect(options);
        log.info("Publisher comandi plancia BoardHub collegato al broker MQTT.");
    }

    public void publishFor(GameEvent event) {
        Optional<ObjectNode> command = commandPayload(event);
        if (command.isEmpty()) {
            return;
        }
        if (client == null || !client.isConnected()) {
            log.warn("Comando plancia {} non pubblicato: broker MQTT non collegato.", event.eventId());
            return;
        }
        String topic = commandTopic(event);
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(command.get());
            MqttMessage message = new MqttMessage(bytes);
            message.setQos(properties.qos());
            message.setRetained(false);
            client.publish(topic, message);
        } catch (MqttException | JsonProcessingException ex) {
            log.warn("Comando plancia {} non pubblicato su {}: {}", event.eventId(), topic, ex.getMessage());
        }
    }

    Optional<ObjectNode> commandPayload(GameEvent event) {
        ObjectNode command = JsonNodeFactory.instance.objectNode();
        command.put("commandId", event.eventId());
        command.put("sessionId", event.sessionId());
        command.put("sequenceNumber", event.sequenceNumber());
        command.put("issuedAt", event.occurredAt());
        copyText(event, command, "sessionPieceId");

        switch (event.eventType()) {
            case "MOVE_CONFIRMED" -> {
                command.put("commandType", "SHOW_PATH");
                copyText(event, command, "to");
                copyTextArray(event, command, "path");
            }
            case "TRAP_TRIGGERED" -> {
                command.put("commandType", "SHOW_CORRECTION_CELL");
                copyText(event, command, "to");
            }
            case "TRAP_ROLL_RESOLVED", "TRAP_RESOLUTION_COMPLETED" ->
                    command.put("commandType", "CLEAR_EFFECT");
            default -> {
                return Optional.empty();
            }
        }
        return Optional.of(command);
    }

    static String commandTopic(GameEvent event) {
        return "boardhub/v1/venues/%s/tables/%s/commands"
                .formatted(event.venueId(), event.tableId());
    }

    private void copyText(GameEvent event, ObjectNode target, String field) {
        if (event.payload().path(field).isTextual()) {
            target.put(field, event.payload().path(field).asText());
        }
    }

    private void copyTextArray(GameEvent event, ObjectNode target, String field) {
        if (!event.payload().path(field).isArray()) {
            return;
        }
        ArrayNode values = target.putArray(field);
        event.payload().path(field).forEach(value -> {
            if (value.isTextual()) {
                values.add(value.asText());
            }
        });
    }

    @PreDestroy
    public void shutdown() throws MqttException {
        if (client != null && client.isConnected()) {
            client.disconnect();
            client.close();
        }
    }
}
