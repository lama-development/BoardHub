package it.uniupo.boardhub.eventservice.mqtt;

import it.uniupo.boardhub.eventservice.config.MqttProperties;
import it.uniupo.boardhub.eventservice.model.GameEvent;
import it.uniupo.boardhub.eventservice.repository.GameEventRepository;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
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

@Component
public class MqttEventSubscriber implements ApplicationRunner, MqttCallback {

    private static final Logger log = LoggerFactory.getLogger(MqttEventSubscriber.class);

    private final MqttProperties properties;
    private final GameEventParser parser;
    private final GameEventRepository repository;
    private MqttClient client;

    public MqttEventSubscriber(
            MqttProperties properties,
            GameEventParser parser,
            GameEventRepository repository
    ) {
        this.properties = properties;
        this.parser = parser;
        this.repository = repository;
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
        client.subscribe(properties.topic(), properties.qos());

        log.info("Event-service BoardHub in ascolto sul topic MQTT: {}", properties.topic());
    }

    @Override
    public void connectionLost(Throwable cause) {
        String reason = cause == null ? "motivo non disponibile" : cause.getMessage();
        log.warn("Connessione MQTT persa: {}", reason);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        // Ogni messaggio valido viene trasformato in evento e salvato su database.
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);

        try {
            GameEvent event = parser.parse(payload);
            boolean inserted = repository.save(event);
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
            log.warn("Evento MQTT non valido sul topic {}: {}", topic, ex.getMessage());
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
