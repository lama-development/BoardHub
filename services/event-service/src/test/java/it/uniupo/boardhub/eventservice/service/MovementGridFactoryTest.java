package it.uniupo.boardhub.eventservice.service;

import it.uniupo.boardhub.eventservice.controller.dto.MovementGridRequest;
import it.uniupo.boardhub.eventservice.controller.dto.MovementWallRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MovementGridFactoryTest {

    private final MovementGridFactory factory = new MovementGridFactory();

    @Test
    void rifiutaCellaFuoriDallaGriglia() {
        assertThatThrownBy(() -> factory.create(new MovementGridRequest(
                3, 3, List.of("D1"), List.of(), List.of(), List.of(), List.of(), List.of()
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("D1");
    }

    @Test
    void rifiutaCellaConTerreniInConflitto() {
        assertThatThrownBy(() -> factory.create(new MovementGridRequest(
                3, 3, List.of("B2"), List.of("B2"), List.of(), List.of(), List.of(), List.of()
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("piu categorie");
    }

    @Test
    void rifiutaMuroDiagonale() {
        assertThatThrownBy(() -> factory.create(new MovementGridRequest(
                3, 3, List.of(), List.of(), List.of(), List.of(),
                List.of(new MovementWallRequest("B2", "SOUTH_EAST")), List.of()
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Un muro puo trovarsi solo su un bordo ortogonale.");
    }

    @Test
    void rifiutaGrigliaSproporzionataPerLaDemo() {
        assertThatThrownBy(() -> factory.create(new MovementGridRequest(
                51, 50, List.of(), List.of(), List.of(), List.of(), List.of(), List.of()
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2500 celle");
    }
}
