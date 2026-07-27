package it.uniupo.boardhub.eventservice.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.boardhub.eventservice.model.GameEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MqttEventSubscriberTest {

    private final GameEventParser parser = new GameEventParser(new ObjectMapper());

    @Test
    void salvaEventoQuandoTopicETavoloCoincidono() throws Exception {
        GameEvent event = parser.parse(eventPayload("table-01"));

        assertThatCode(() -> MqttEventSubscriber.requireMatchingTopic(
                "boardhub/v1/venues/venue-01/tables/table-01/events",
                event
        )).doesNotThrowAnyException();
    }

    @Test
    void rifiutaEventoQuandoTopicETavoloNonCoincidono() throws Exception {
        GameEvent event = parser.parse(eventPayload("table-04"));

        assertThatThrownBy(() -> MqttEventSubscriber.requireMatchingTopic(
                "boardhub/v1/venues/venue-01/tables/table-01/events",
                event
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("topic MQTT incoerente");
    }

    private String eventPayload(String tableId) {
        return """
                {
                  "eventId": "evt-mqtt-routing-001",
                  "eventType": "MOVE",
                  "venueId": "venue-01",
                  "tableId": "%s",
                  "sessionId": "session-mqtt-routing-001",
                  "source": "SIMULATOR",
                  "occurredAt": "2026-07-27T10:00:00Z",
                  "sequenceNumber": 1,
                  "payload": {
                    "characterId": "adv-01",
                    "from": "A1",
                    "to": "B1"
                  }
                }
                """.formatted(tableId);
    }
}
