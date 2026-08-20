package it.uniupo.boardhub.statsservice.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.boardhub.statsservice.config.StatsMqttProperties;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SessionResultSubscriberTest {

    private final StatsMqttProperties properties = new StatsMqttProperties(
            "tcp://localhost:1883",
            "boardhub-stats-service",
            "boardhub/v1/venues/+/session-results",
            1
    );

    private SessionResultSubscriber subscriber() {
        return new SessionResultSubscriber(properties, new ObjectMapper(), null);
    }

    @Test
    void laSessioneMqttNonDeveEssereRipulita() {
        MqttConnectOptions options = subscriber().connectOptions();

        // Con una sessione pulita il broker scarterebbe i risultati pubblicati
        // mentre il servizio e spento, e quelle partite andrebbero perse.
        assertThat(options.isCleanSession()).isFalse();
    }

    @Test
    void laRiconnessioneAutomaticaRestaAttiva() {
        assertThat(subscriber().connectOptions().isAutomaticReconnect()).isTrue();
    }

    @Test
    void ilClientIdEStabile() {
        // Il broker riconosce la sessione persistente dall'identificativo del
        // client: se cambiasse a ogni avvio, la coda non verrebbe ritrovata.
        assertThat(properties.clientId()).isEqualTo("boardhub-stats-service");
    }
}
