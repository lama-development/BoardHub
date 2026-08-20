package it.uniupo.boardhub.eventservice.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniupo.boardhub.eventservice.config.MqttProperties;
import it.uniupo.boardhub.eventservice.model.result.PlayerResult;
import it.uniupo.boardhub.eventservice.model.result.SessionResult;
import it.uniupo.boardhub.eventservice.model.result.SessionResultOutboxEntry;
import it.uniupo.boardhub.eventservice.repository.SessionResultOutboxRepository;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

// Unico canale verso il servizio statistiche, che non accede allo schema di gioco.
@Component
public class MqttSessionResultPublisher implements ApplicationRunner, MqttCallbackExtended {

    private static final Logger log = LoggerFactory.getLogger(MqttSessionResultPublisher.class);
    private static final int DELIVERY_BATCH_SIZE = 50;
    private static final long PUBLISH_TIMEOUT_MILLIS = 5_000;
    private static final long MAX_RETRY_SECONDS = 60;

    private final MqttProperties properties;
    private final ObjectMapper objectMapper;
    private final SessionResultOutboxRepository outboxRepository;
    private final Clock clock;
    private MqttClient client;

    @Autowired
    public MqttSessionResultPublisher(
            MqttProperties properties,
            ObjectMapper objectMapper,
            SessionResultOutboxRepository outboxRepository,
            Clock clock
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.outboxRepository = outboxRepository;
        this.clock = clock;
    }

    // Costruttore usato dai test del payload, senza accesso all'outbox.
    public MqttSessionResultPublisher(MqttProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, null, Clock.systemUTC());
    }

    @Override
    public void run(ApplicationArguments args) throws MqttException {
        client = new MqttClient(
                properties.brokerUri(), properties.clientId() + "-results", new MemoryPersistence()
        );
        client.setCallback(this);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        client.connect(options);
    }

    static String topicFor(String venueId) {
        return "boardhub/v1/venues/%s/session-results".formatted(venueId);
    }

    // Accoda nella transazione corrente e tenta la consegna soltanto dopo il commit.
    public void publishAfterCommit(SessionResult result) {
        if (outboxRepository == null) {
            return;
        }
        OffsetDateTime now = now();
        outboxRepository.enqueue(
                result.factId(),
                result.sessionId(),
                topicFor(result.venueId()),
                serialize(result),
                now
        );

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deliverPending();
                }
            });
            return;
        }
        deliverPending();
    }

    @Scheduled(fixedDelayString = "${boardhub.mqtt.result-outbox-poll-ms:2000}")
    public synchronized void deliverPending() {
        if (outboxRepository == null || client == null || !client.isConnected()) {
            return;
        }

        for (SessionResultOutboxEntry entry : outboxRepository.findDue(now(), DELIVERY_BATCH_SIZE)) {
            publish(entry);
        }
    }

    private void publish(SessionResultOutboxEntry entry) {
        try {
            MqttMessage message = new MqttMessage(entry.payloadJson().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            message.setQos(properties.qos());
            message.setRetained(false);
            client.getTopic(entry.topic()).publish(message).waitForCompletion(PUBLISH_TIMEOUT_MILLIS);
            outboxRepository.markPublished(entry.factId(), now());
            log.info("Risultato della sessione {} consegnato al broker MQTT.", entry.sessionId());
        } catch (MqttException ex) {
            int attempt = entry.attemptCount() + 1;
            long delay = Math.min(1L << Math.min(attempt - 1, 6), MAX_RETRY_SECONDS);
            outboxRepository.scheduleRetry(
                    entry.factId(),
                    attempt,
                    now().plus(Duration.ofSeconds(delay)),
                    ex.getMessage()
            );
            log.warn(
                    "Consegna del risultato {} non riuscita; nuovo tentativo tra {} secondi: {}",
                    entry.sessionId(), delay, ex.getMessage()
            );
        }
    }

    private String serialize(SessionResult result) {
        if (client == null || !client.isConnected()) {
            log.debug("Il risultato {} viene accodato mentre MQTT non e collegato.", result.sessionId());
        }
        try {
            return objectMapper.writeValueAsString(payload(result));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Risultato di sessione non serializzabile.", ex);
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
    }

    ObjectNode payload(SessionResult result) {
        ObjectNode fact = JsonNodeFactory.instance.objectNode();
        fact.put("factId", result.factId());
        fact.put("factType", "SESSION_COMPLETED");
        fact.put("sessionId", result.sessionId());
        fact.put("venueId", result.venueId());
        fact.put("tableId", result.tableId());
        fact.put("title", result.title());
        fact.put("gameType", result.gameType());
        fact.put("startedAt", result.startedAt().toString());
        fact.put("endedAt", result.endedAt().toString());
        fact.put("durationMinutes", result.durationMinutes());

        ArrayNode participants = fact.putArray("participants");
        for (PlayerResult player : result.participants()) {
            ObjectNode node = participants.addObject();
            node.put("playerReference", player.playerReference());
            node.put("displayName", player.displayName());
            node.put("role", "PLAYER");
            node.put("characterName", player.characterName());
            node.put("className", player.className());
            node.put("species", player.species());
            node.put("level", player.level());
            node.put("survived", player.survived());
            node.put("movesConfirmed", player.movesConfirmed());
            node.put("cellsTravelled", player.cellsTravelled());
            node.put("trapsTriggered", player.trapsTriggered());
            node.put("savesSucceeded", player.savesSucceeded());
            node.put("savesFailed", player.savesFailed());
            node.put("damageTaken", player.damageTaken());
        }
        return fact;
    }

    @Override
    public void connectComplete(boolean reconnect, String serverUri) {
        log.info("Publisher dei risultati collegato al broker MQTT (riconnessione={}).", reconnect);
        deliverPending();
    }

    @Override
    public void connectionLost(Throwable cause) {
        String reason = cause == null ? "motivo non disponibile" : cause.getMessage();
        log.warn("Connessione MQTT del publisher risultati persa: {}", reason);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        // Questo client pubblica soltanto risultati e non sottoscrive topic.
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // La conferma viene attesa in modo sincrono prima di aggiornare l'outbox.
    }

    @PreDestroy
    public void shutdown() throws MqttException {
        if (client != null) {
            if (client.isConnected()) {
                client.disconnect();
            }
            client.close();
        }
    }
}
