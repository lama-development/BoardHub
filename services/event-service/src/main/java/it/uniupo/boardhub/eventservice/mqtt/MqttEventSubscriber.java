package it.uniupo.boardhub.eventservice.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.boardhub.eventservice.config.MqttProperties;
import it.uniupo.boardhub.eventservice.model.GameEvent;
import it.uniupo.boardhub.eventservice.model.session.GameSession;
import it.uniupo.boardhub.eventservice.model.session.GameSessionStatus;
import it.uniupo.boardhub.eventservice.repository.GameEventRepository;
import it.uniupo.boardhub.eventservice.repository.GameSessionRepository;
import it.uniupo.boardhub.eventservice.service.SessionEventStreamService;
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
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
public class MqttEventSubscriber implements ApplicationRunner, MqttCallbackExtended {

    private static final Logger log = LoggerFactory.getLogger(MqttEventSubscriber.class);

    private final MqttProperties properties;
    private final GameEventParser parser;
    private final GameEventRepository repository;
    private final GameSessionRepository sessionRepository;
    private final SessionEventStreamService eventStreamService;
    private final MqttEventAckPublisher ackPublisher;
    private final ObjectMapper objectMapper;
    private MqttClient client;

    public MqttEventSubscriber(
            MqttProperties properties,
            GameEventParser parser,
            GameEventRepository repository,
            GameSessionRepository sessionRepository,
            SessionEventStreamService eventStreamService,
            MqttEventAckPublisher ackPublisher,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.parser = parser;
        this.repository = repository;
        this.sessionRepository = sessionRepository;
        this.eventStreamService = eventStreamService;
        this.ackPublisher = ackPublisher;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) throws MqttException {
        // All'avvio si collega al broker e si iscrive al topic degli eventi.
        client = new MqttClient(
                properties.brokerUri(),
                properties.clientId(),
                new MemoryPersistence()
        );
        client.setCallback(this);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);

        client.connect(options);
        // La sottoscrizione non viene fatta qui: viene eseguita da connectComplete,
        // che scatta sia alla prima connessione sia dopo ogni riconnessione.
    }

    // Ripristina la sottoscrizione a ogni connessione riuscita. Con cleanSession il
    // broker la scarta alla disconnessione e la sola riconnessione automatica
    // ristabilisce il socket ma non l'iscrizione.
    @Override
    public void connectComplete(boolean reconnect, String serverUri) {
        try {
            client.subscribe(properties.topic(), properties.qos());
            log.info(
                    "Event-service BoardHub in ascolto sul topic MQTT: {} (riconnessione={})",
                    properties.topic(),
                    reconnect
            );
        } catch (MqttException ex) {
            log.error("Sottoscrizione al topic {} non riuscita: {}", properties.topic(), ex.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        String reason = cause == null ? "motivo non disponibile" : cause.getMessage();
        log.warn("Connessione MQTT persa: {}", reason);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        // Ogni messaggio valido viene trasformato in evento, salvato e confermato.
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);

        GameEvent event;
        try {
            event = parser.parse(payload);
            requireMatchingTopic(topic, event);
            requireExternalSource(event);
        } catch (Exception ex) {
            // Payload non interpretabile: l'edge non deve ritentarlo all'infinito.
            log.warn("Evento MQTT non valido sul topic {}: {}", topic, ex.getMessage());
            ackPublisher.publish(
                    topic, extractEventId(payload), null,
                    MqttEventAckPublisher.REJECTED, ex.getMessage()
            );
            return;
        }

        if (isEndedSession(event.sessionId())) {
            // Partita conclusa: il fatto non viene accettato, ma nemmeno scartato in silenzio.
            log.warn("Evento {} rifiutato: la sessione {} e conclusa.", event.eventId(), event.sessionId());
            ackPublisher.publish(
                    topic, event.eventId(), event.sessionId(),
                    MqttEventAckPublisher.CONFLICT, "La sessione e gia conclusa."
            );
            return;
        }

        try {
            // Gli eventi MQTT descrivono osservazioni della plancia. Il movimento
            // autorevole passa dalle API che verificano proprietario e versione.
            boolean inserted = repository.save(event);
            if (inserted) {
                eventStreamService.publishAfterCommit(event);
            }
            ackPublisher.publish(
                    topic, event.eventId(), event.sessionId(),
                    inserted ? MqttEventAckPublisher.PERSISTED : MqttEventAckPublisher.DUPLICATE,
                    null
            );
            log.info(
                    "Evento MQTT ricevuto tipo={} seq={} sessione={} sorgente={} topic={} salvato={}",
                    event.eventType(),
                    event.sequenceNumber(),
                    event.sessionId(),
                    event.source(),
                    topic,
                    inserted
            );
        } catch (Exception ex) {
            // Senza acknowledgement l'edge conserva l'elemento in coda e ritenta.
            log.error("Salvataggio dell'evento {} non riuscito: {}", event.eventId(), ex.getMessage());
        }
    }

    // Una sessione mai creata resta ammessa: il simulatore storico non la crea.
    private boolean isEndedSession(String sessionId) {
        try {
            Optional<GameSession> session = sessionRepository.findSessionById(sessionId);
            return session.isPresent() && session.get().status() == GameSessionStatus.ENDED;
        } catch (Exception ex) {
            log.debug("Stato della sessione {} non verificabile: {}", sessionId, ex.getMessage());
            return false;
        }
    }

    // Legge l'identificativo anche da un payload non valido, per poter comunque rispondere.
    private String extractEventId(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            JsonNode eventId = node.path("eventId");
            return eventId.isTextual() ? eventId.asText() : null;
        } catch (Exception ex) {
            return null;
        }
    }

    // Impedisce che un payload attribuisca l'evento a un tavolo diverso dal topic fisico.
    static void requireMatchingTopic(String topic, GameEvent event) {
        String expectedTopic = "boardhub/v1/venues/%s/tables/%s/events"
                .formatted(event.venueId(), event.tableId());
        if (!expectedTopic.equals(topic)) {
            throw new IllegalArgumentException(
                    "topic MQTT incoerente con venueId e tableId del payload."
            );
        }
    }

    // BACKEND identifica esclusivamente eventi creati nella transazione applicativa.
    static void requireExternalSource(GameEvent event) {
        if ("BACKEND".equalsIgnoreCase(event.source())) {
            throw new IllegalArgumentException(
                    "La sorgente BACKEND e riservata all'event-service."
            );
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Il servizio riceve soltanto eventi, quindi non attende conferme di pubblicazione.
    }

    @PreDestroy
    public void shutdown() throws MqttException {
        if (client != null && client.isConnected()) {
            client.disconnect();
            client.close();
        }
    }
}
