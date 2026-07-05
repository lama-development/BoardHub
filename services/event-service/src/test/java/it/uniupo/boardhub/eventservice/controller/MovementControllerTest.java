package it.uniupo.boardhub.eventservice.controller;

import it.uniupo.boardhub.eventservice.service.MovementService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MovementControllerTest {

    @Test
    void restituisceCelleRaggiungibiliConPercorsoETrappole() throws Exception {
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new MovementController(new MovementService()))
                .build();

        mockMvc.perform(post("/api/v1/movement/reachable-cells")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "characterId": "adv-01",
                                  "start": "A1",
                                  "movementPoints": 2,
                                  "grid": {
                                    "width": 3,
                                    "height": 3,
                                    "difficultCells": ["C1"],
                                    "blockedCells": ["A2"],
                                    "obstacleCells": [],
                                    "occupiedCells": [],
                                    "walls": [
                                      { "cell": "B1", "direction": "SOUTH" }
                                    ],
                                    "traps": [
                                      {
                                        "trapId": "trap-01",
                                        "cell": "B1",
                                        "visibility": "HIDDEN",
                                        "armed": true
                                      }
                                    ]
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.characterId").value("adv-01"))
                .andExpect(jsonPath("$.reachableCells[?(@.cell == 'A1')]").exists())
                .andExpect(jsonPath("$.reachableCells[?(@.cell == 'B1')]").exists())
                .andExpect(jsonPath("$.reachableCells[?(@.cell == 'C1')]").doesNotExist())
                .andExpect(jsonPath("$.reachableCells[?(@.cell == 'A2')]").doesNotExist())
                .andExpect(jsonPath("$.reachableCells[?(@.cell == 'B2')]").doesNotExist())
                .andExpect(jsonPath("$.reachableCells[1].cell").value("B1"))
                .andExpect(jsonPath("$.reachableCells[1].trapsOnPath[0]").value("trap-01"))
                .andExpect(jsonPath("$.reachableCells[1].path[0]").value("A1"))
                .andExpect(jsonPath("$.reachableCells[1].path[1]").value("B1"));
    }
}
