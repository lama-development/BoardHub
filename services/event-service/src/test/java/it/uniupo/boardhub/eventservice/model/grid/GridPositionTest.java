package it.uniupo.boardhub.eventservice.model.grid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GridPositionTest {

    @Test
    void converteCoordinateDellaGriglia() {
        GridPosition position = GridPosition.fromCell("B12");

        assertEquals(12, position.row());
        assertEquals(2, position.column());
        assertEquals("B12", position.toCell());
    }

    @Test
    void supportaColonneConPiuLettere() {
        GridPosition position = GridPosition.fromCell("AA3");

        assertEquals(3, position.row());
        assertEquals(27, position.column());
        assertEquals("AA3", position.toCell());
    }

    @Test
    void rifiutaCoordinateNonValide() {
        assertThrows(IllegalArgumentException.class, () -> GridPosition.fromCell("A0"));
        assertThrows(IllegalArgumentException.class, () -> GridPosition.fromCell("12A"));
    }
}
