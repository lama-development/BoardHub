package it.uniupo.boardhub.eventservice.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniupo.boardhub.eventservice.config.MqttProperties;
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

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

// Conferma all'edge se un evento ricevuto via MQTT e stato davvero persistito.
// Il PUBACK del broker non basta a chiudere l'elemento in coda.
@Component
public class MqttEventAckPublisher implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MqttEventAckPublisher.class);

    public static final String PERSISTED = "PERSISTED";
    public static final String DUPLICATE = "DUPLICATE";
    public static final String REJECTED = "REJECTED";
    public static final String CONFLICT = "CONFLICT";

    private static final int MAX_DETAIL_LENGTH = 200;

    private final MqttProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private MqttClient client;

    public MqttEventAckPublisher(MqttProperties properties, ObjectMapper objectMapper, Clock clock) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) throws MqttException {
        client = new MqttClient(
                properties.brokerUri(), properties.clientId() + "-acks", new MemoryPersistence()
        );
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        client.connect(options);
        log.info("Publisher degli acknowledgement BoardHub collegato al broker MQTT.");
    }

    // Deriva il topic di risposta da quello di ingresso: un payload malformato non
    // deve poter dirottare la conferma sul canale di un altro tavolo.
    static String ackTopicFor(String eventsTopic) {
        if (eventsTopic == null || !eventsTopic.endsWith("/events")) {
            return null;
        }
        return eventsTopic.substring(0, eventsTopic.length() - "/events".length()) + "/event-acks";
    }

    public void publish(String eventsTopic, String eventId, String sessionId, String status, String detail) {
        String topic = ackTopicFor(eventsTopic);
        if (topic == null || eventId == null || eventId.isBlank()) {
            return;
        }
        if (client == null || !client.isConnected()) {
            log.warn("Acknowledgement per {} non pubblicato: broker MQTT non collegato.", eventId);
            return;
        }

        ObjectNode ack = JsonNodeFactory.instance.objectNode();
        ack.put("ackId", "ack-" + eventId);
        ack.put("eventId", eventId);
        if (sessionId != null) {
            ack.put("sessionId", sessionId);
        }
        ack.put("status", status);
        ack.put("receivedAt", OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).toString());
        if (detail == null || detail.isBlank()) {
            ack.putNull("detail");
        } else {
            ack.put("detail", sanitize(detail));
        }

        try {
            MqttMessage message = new MqttMessage(objectMapper.writeValueAsBytes(ack));
            message.setQos(properties.qos());
            message.setRetained(false);
            client.publish(topic, message);
        } catch (MqttException | JsonProcessingException ex) {
            log.warn("Acknowledgement {} non pubblicato su {}: {}", eventId, topic, ex.getMessage());
        }
    }

    // La diagnostica torna all'edge: deve restare breve e priva di dati riservati.
    private String sanitize(String detail) {
        String trimmed = detail.strip();
        return trimmed.length() <= MAX_DETAIL_LENGTH
                ? trimmed
                : trimmed.substring(0, MAX_DETAIL_LENGTH);
    }

    @PreDestroy
    public void shutdown() throws MqttException {
        if (client != null && client.isConnected()) {
            client.disconnect();
            client.close();
        }
    }
}
