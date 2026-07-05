package it.uniupo.boardhub.eventservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.boardhub.eventservice.model.GameEvent;
import it.uniupo.boardhub.eventservice.repository.GameEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GameEventControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void restituisceEventiDellaSessione() throws Exception {
        GameEvent event = new GameEvent(
                "evt-000001",
                "MOVE",
                "venue-01",
                "table-04",
                "session-20260703-001",
                "SIMULATOR",
                "2026-07-03T10:00:00Z",
                1,
                objectMapper.readTree("""
                        {
                          "characterId": "adv-01",
                          "from": "A3",
                          "to": "A4"
                        }
                        """)
        );

        GameEventRepository repository = new GameEventRepository(null, null) {
            @Override
            public List<GameEvent> findBySessionId(String sessionId) {
                return List.of(event);
            }
        };
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new GameEventController(repository))
                .build();

        mockMvc.perform(get("/api/v1/sessions/session-20260703-001/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value("evt-000001"))
                .andExpect(jsonPath("$[0].eventType").value("MOVE"))
                .andExpect(jsonPath("$[0].sequenceNumber").value(1))
                .andExpect(jsonPath("$[0].payload.characterId").value("adv-01"));
    }
}
