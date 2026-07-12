package it.uniupo.boardhub.eventservice.model.grid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MovementRequestTest {

    @Test
    void creaRichiestaDiMovimentoValida() {
        MovementRequest request = new MovementRequest("adv-01", GridPosition.fromCell("A3"), 6);

        assertEquals("adv-01", request.characterId());
        assertEquals("A3", request.start().toCell());
        assertEquals(6, request.movementPoints());
    }

    @Test
    void rifiutaRichiestaSenzaPersonaggioOPuntiNegativi() {
        assertThrows(IllegalArgumentException.class,
                () -> new MovementRequest("", GridPosition.fromCell("A3"), 6));
        assertThrows(IllegalArgumentException.class,
                () -> new MovementRequest("adv-01", GridPosition.fromCell("A3"), -1));
    }

    @Test
    void rifiutaPuntiMovimentoSproporzionati() {
        assertThrows(IllegalArgumentException.class,
                () -> new MovementRequest(
                        "adv-01",
                        GridPosition.fromCell("A3"),
                        MovementRequest.MAX_MOVEMENT_POINTS + 1
                ));
    }
}
