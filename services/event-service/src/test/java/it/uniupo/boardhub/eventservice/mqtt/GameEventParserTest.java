package it.uniupo.boardhub.eventservice.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.boardhub.eventservice.model.GameEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameEventParserTest {

    private final GameEventParser parser = new GameEventParser(new ObjectMapper());

    @Test
    void interpretaPayloadDiMovimento() throws Exception {
        String json = """
                {
                  "eventId": "evt-000001",
                  "eventType": "MOVE",
                  "venueId": "venue-01",
                  "tableId": "table-04",
                  "sessionId": "session-20260702-001",
                  "source": "SIMULATOR",
                  "occurredAt": "2026-07-02T10:00:00Z",
                  "sequenceNumber": 2,
                  "payload": {
                    "characterId": "adv-01",
                    "from": "A3",
                    "to": "A4"
                  }
                }
                """;

        GameEvent event = parser.parse(json);

        assertThat(event.eventType()).isEqualTo("MOVE");
        assertThat(event.sequenceNumber()).isEqualTo(2);
        assertThat(event.payload().get("characterId").asText()).isEqualTo("adv-01");
        assertThat(event.payload().get("from").asText()).isEqualTo("A3");
        assertThat(event.payload().get("to").asText()).isEqualTo("A4");
    }
}
