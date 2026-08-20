package it.uniupo.boardhub.statsservice.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.boardhub.statsservice.config.StatsMqttProperties;
import it.uniupo.boardhub.statsservice.service.SessionResultIngestionService;
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

// Unico ingresso dei dati del servizio: nessun accesso allo schema di gioco.
@Component
public class SessionResultSubscriber implements ApplicationRunner, MqttCallbackExtended {

    private static final Logger log = LoggerFactory.getLogger(SessionResultSubscriber.class);

    private final StatsMqttProperties properties;
    private final ObjectMapper objectMapper;
    private final SessionResultIngestionService ingestionService;
    private MqttClient client;

    public SessionResultSubscriber(
            StatsMqttProperties properties,
            ObjectMapper objectMapper,
            SessionResultIngestionService ingestionService
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.ingestionService = ingestionService;
    }

    @Override
    public void run(ApplicationArguments args) throws MqttException {
        client = new MqttClient(properties.brokerUri(), properties.clientId(), new MemoryPersistence());
        client.setCallback(this);
        client.connect(connectOptions());
    }

    // La sessione persistente conserva sul broker i risultati ricevuti mentre il
    // servizio e offline.
    MqttConnectOptions connectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(false);
        return options;
    }

    // Rinnova la sottoscrizione dopo ogni connessione senza dipendere dallo stato
    // conservato dal broker.
    @Override
    public void connectComplete(boolean reconnect, String serverUri) {
        try {
            client.subscribe(properties.topic(), properties.qos());
            log.info(
                    "Stats-service in ascolto sui risultati di sessione: {} (riconnessione={})",
                    properties.topic(), reconnect
            );
        } catch (MqttException ex) {
            log.error("Sottoscrizione a {} non riuscita: {}", properties.topic(), ex.getMessage());
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        try {
            JsonNode fact = objectMapper.readTree(payload);
            String factType = fact.path("factType").asText("");
            if (!"SESSION_COMPLETED".equals(factType)) {
                log.debug("Fatto ignorato di tipo {} sul topic {}.", factType, topic);
                return;
            }
            ingestionService.ingest(ingestionService.parse(fact));
        } catch (Exception ex) {
            log.warn("Risultato non valido sul topic {}: {}", topic, ex.getMessage());
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("Connessione MQTT persa: {}", cause == null ? "motivo non disponibile" : cause.getMessage());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Il servizio statistiche riceve soltanto: non pubblica nulla sul broker.
    }

    @PreDestroy
    public void shutdown() throws MqttException {
        if (client != null && client.isConnected()) {
            client.disconnect();
            client.close();
        }
    }
}
