package it.uniupo.boardhub.eventservice.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.boardhub.eventservice.model.GameEvent;
import org.springframework.stereotype.Component;

@Component
public class GameEventParser {

    private final ObjectMapper objectMapper;

    public GameEventParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // Converte il JSON MQTT nel modello interno del servizio.
    public GameEvent parse(String payload) throws JsonProcessingException {
        return objectMapper.readValue(payload, GameEvent.class);
    }
}
