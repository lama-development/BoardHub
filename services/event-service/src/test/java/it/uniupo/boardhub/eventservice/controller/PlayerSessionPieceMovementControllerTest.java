package it.uniupo.boardhub.eventservice.controller;

import it.uniupo.boardhub.eventservice.model.grid.GridPosition;
import it.uniupo.boardhub.eventservice.model.grid.ReachableCell;
import it.uniupo.boardhub.eventservice.model.piece.PieceMoveResult;
import it.uniupo.boardhub.eventservice.model.piece.PieceReachability;
import it.uniupo.boardhub.eventservice.service.SessionPieceMovementService;
import it.uniupo.boardhub.eventservice.service.exception.StalePieceStateException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class PlayerSessionPieceMovementControllerTest {

    private static final UUID PIECE_ID =
            UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID CHARACTER_ID =
            UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID COMMAND_ID =
            UUID.fromString("40000000-0000-0000-0000-000000000001");

    @Test
    void esponeRaggiungibilitaESpostamentoConContrattiStabili() throws Exception {
        SessionPieceMovementService service = new StubMovementService() {
            @Override
            public PieceReachability reachableCells(
                    String sessionId,
                    UUID sessionPieceId,
                    String authorizationHeader
            ) {
                return new PieceReachability(
                        PIECE_ID,
                        "B2",
                        6,
                        0,
                        List.of(new ReachableCell(
                                GridPosition.fromCell("C2"),
                                1,
                                List.of(
                                        GridPosition.fromCell("B2"),
                                        GridPosition.fromCell("C2")
                                ),
                                List.of()
                        ))
                );
            }

            @Override
            public PieceMoveResult move(
                    String sessionId,
                    UUID sessionPieceId,
                    String authorizationHeader,
                    it.uniupo.boardhub.eventservice.service.command.MoveSessionPieceCommand command
            ) {
                return new PieceMoveResult(
                        COMMAND_ID,
                        "move-" + COMMAND_ID,
                        PIECE_ID,
                        CHARACTER_ID,
                        "B2",
                        "C2",
                        List.of("B2", "C2"),
                        1,
                        1,
                        List.of()
                );
            }
        };
        MockMvc mockMvc = mockMvc(service);

        mockMvc.perform(get(
                        "/api/v1/player/sessions/session-001/pieces/{pieceId}/reachable-cells",
                        PIECE_ID
                ).header("Authorization", "Bearer player-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentCell").value("B2"))
                .andExpect(jsonPath("$.movementPoints").value(6))
                .andExpect(jsonPath("$.reachableCells[0].cell").value("C2"));

        mockMvc.perform(post(
                        "/api/v1/player/sessions/session-001/pieces/{pieceId}/moves",
                        PIECE_ID
                )
                        .header("Authorization", "Bearer player-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "destination": "C2",
                                  "expectedVersion": 0,
                                  "commandId": "40000000-0000-0000-0000-000000000001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.from").value("B2"))
                .andExpect(jsonPath("$.to").value("C2"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void traduceUnaVersioneObsoletaInConflict() throws Exception {
        SessionPieceMovementService service = new StubMovementService() {
            @Override
            public PieceMoveResult move(
                    String sessionId,
                    UUID sessionPieceId,
                    String authorizationHeader,
                    it.uniupo.boardhub.eventservice.service.command.MoveSessionPieceCommand command
            ) {
                throw new StalePieceStateException(2);
            }
        };

        mockMvc(service).perform(post(
                        "/api/v1/player/sessions/session-001/pieces/{pieceId}/moves",
                        PIECE_ID
                )
                        .header("Authorization", "Bearer player-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "destination": "C2",
                                  "expectedVersion": 0,
                                  "commandId": "40000000-0000-0000-0000-000000000001"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STALE_PIECE_STATE"));
    }

    private MockMvc mockMvc(SessionPieceMovementService service) {
        return standaloneSetup(new PlayerSessionPieceMovementController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private abstract static class StubMovementService
            extends SessionPieceMovementService {

        private StubMovementService() {
            super(null, null, null, null, null, null, null, null);
        }
    }
}
