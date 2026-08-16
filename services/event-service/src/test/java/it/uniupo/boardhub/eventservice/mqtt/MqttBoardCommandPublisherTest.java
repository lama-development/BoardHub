package it.uniupo.boardhub.eventservice.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.boardhub.eventservice.config.MqttProperties;
import it.uniupo.boardhub.eventservice.model.GameEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MqttBoardCommandPublisherTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MqttBoardCommandPublisher publisher = new MqttBoardCommandPublisher(
            new MqttProperties("tcp://localhost:1883", "test", "events", 1), objectMapper
    );

    @Test
    void proiettaSoloIlComandoTecnicoSenzaSegretiDellaTrappola() throws Exception {
        GameEvent event = new GameEvent(
                "trap-command-1", "TRAP_TRIGGERED", "venue-01", "table-04",
                "session-001", "BACKEND", "2026-08-08T10:00:00Z", 12,
                objectMapper.readTree("""
                        {
                          "sessionPieceId":"00000000-0000-0000-0000-000000000001",
                          "to":"C2",
                          "trapId":"trap-secret",
                          "saveDc":18,
                          "damageExpression":"4d10"
                        }
                        """)
        );

        var command = publisher.commandPayload(event).orElseThrow();

        assertThat(command.path("commandType").asText()).isEqualTo("SHOW_CORRECTION_CELL");
        assertThat(command.path("to").asText()).isEqualTo("C2");
        assertThat(command.has("trapId")).isFalse();
        assertThat(command.has("saveDc")).isFalse();
        assertThat(command.has("damageExpression")).isFalse();
        assertThat(MqttBoardCommandPublisher.commandTopic(event))
                .isEqualTo("boardhub/v1/venues/venue-01/tables/table-04/commands");
    }
}
