package it.uniupo.boardhub.eventservice.service;

import it.uniupo.boardhub.eventservice.model.grid.GameGrid;
import it.uniupo.boardhub.eventservice.model.grid.GridCell;
import it.uniupo.boardhub.eventservice.model.grid.GridDirection;
import it.uniupo.boardhub.eventservice.model.grid.GridPosition;
import it.uniupo.boardhub.eventservice.model.grid.GridTrap;
import it.uniupo.boardhub.eventservice.model.grid.GridWall;
import it.uniupo.boardhub.eventservice.model.grid.MovementRequest;
import it.uniupo.boardhub.eventservice.model.grid.ReachableCell;
import it.uniupo.boardhub.eventservice.model.grid.TerrainType;
import it.uniupo.boardhub.eventservice.model.grid.TrapVisibility;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovementServiceTest {

    private final MovementService service = new MovementService();

    @Test
    void calcolaCelleRaggiungibiliConMovimentoBase() {
        GameGrid grid = GameGrid.empty(3, 3);
        MovementRequest request = new MovementRequest("adv-01", GridPosition.fromCell("B2"), 1);

        Map<String, ReachableCell> reachable = byCell(service.calculateReachableCells(grid, request));

        assertEquals(9, reachable.size());
        assertTrue(reachable.containsKey("B2"));
        assertTrue(reachable.containsKey("B1"));
        assertTrue(reachable.containsKey("A2"));
        assertTrue(reachable.containsKey("C2"));
        assertTrue(reachable.containsKey("B3"));
        assertTrue(reachable.containsKey("A1"));
        assertTrue(reachable.containsKey("C1"));
        assertTrue(reachable.containsKey("A3"));
        assertTrue(reachable.containsKey("C3"));
    }

    @Test
    void usaCostoMaggiorePerTerrenoDifficile() {
        GameGrid grid = GameGrid.empty(3, 3)
                .withCell(new GridCell(GridPosition.fromCell("B1"), TerrainType.DIFFICULT, false));
        MovementRequest request = new MovementRequest("adv-01", GridPosition.fromCell("B2"), 1);

        Map<String, ReachableCell> reachable = byCell(service.calculateReachableCells(grid, request));

        assertFalse(reachable.containsKey("B1"));
    }

    @Test
    void evitaOstacoliCelleBloccateEOccupate() {
        GameGrid grid = GameGrid.empty(3, 3)
                .withCell(new GridCell(GridPosition.fromCell("A2"), TerrainType.OBSTACLE, false))
                .withCell(new GridCell(GridPosition.fromCell("B1"), TerrainType.BLOCKED, false))
                .withCell(new GridCell(GridPosition.fromCell("C2"), TerrainType.NORMAL, true));
        MovementRequest request = new MovementRequest("adv-01", GridPosition.fromCell("B2"), 1);

        Map<String, ReachableCell> reachable = byCell(service.calculateReachableCells(grid, request));

        assertFalse(reachable.containsKey("A2"));
        assertFalse(reachable.containsKey("B1"));
        assertFalse(reachable.containsKey("C2"));
        assertTrue(reachable.containsKey("B3"));
    }

    @Test
    void rispettaMuriTraCelleAdiacenti() {
        GridPosition b2 = GridPosition.fromCell("B2");
        GameGrid grid = GameGrid.empty(3, 3)
                .withWall(new GridWall(b2, GridDirection.EAST));
        MovementRequest request = new MovementRequest("adv-01", b2, 1);

        Map<String, ReachableCell> reachable = byCell(service.calculateReachableCells(grid, request));

        assertFalse(reachable.containsKey("C2"));
        assertTrue(reachable.containsKey("A2"));
        assertTrue(reachable.containsKey("B1"));
        assertTrue(reachable.containsKey("B3"));
    }

    @Test
    void conservaPercorsoETrappoleAttraversate() {
        GameGrid grid = GameGrid.empty(3, 1)
                .withTrap(new GridTrap("trap-01", GridPosition.fromCell("B1"), TrapVisibility.HIDDEN, true));
        MovementRequest request = new MovementRequest("adv-01", GridPosition.fromCell("A1"), 2);

        Map<String, ReachableCell> reachable = byCell(service.calculateReachableCells(grid, request));
        ReachableCell c1 = reachable.get("C1");

        assertEquals(List.of(
                GridPosition.fromCell("A1"),
                GridPosition.fromCell("B1"),
                GridPosition.fromCell("C1")
        ), c1.path());
        assertEquals(1, c1.trapsOnPath().size());
        assertEquals("trap-01", c1.trapsOnPath().get(0).trapId());
    }

    @Test
    void consenteMovimentoDiagonaleSemplificato() {
        GameGrid grid = GameGrid.empty(3, 3);
        MovementRequest request = new MovementRequest("adv-01", GridPosition.fromCell("B2"), 1);

        Map<String, ReachableCell> reachable = byCell(service.calculateReachableCells(grid, request));

        assertTrue(reachable.containsKey("C3"));
        assertEquals(1, reachable.get("C3").cost());
    }

    @Test
    void bloccaDiagonaleSeTagliaAngoloConCellaNonAttraversabile() {
        GameGrid grid = GameGrid.empty(3, 3)
                .withCell(new GridCell(GridPosition.fromCell("C2"), TerrainType.BLOCKED, false));
        MovementRequest request = new MovementRequest("adv-01", GridPosition.fromCell("B2"), 1);

        Map<String, ReachableCell> reachable = byCell(service.calculateReachableCells(grid, request));

        assertFalse(reachable.containsKey("C3"));
    }

    @Test
    void bloccaDiagonaleSeTagliaAngoloConMuro() {
        GridPosition b2 = GridPosition.fromCell("B2");
        GameGrid grid = GameGrid.empty(3, 3)
                .withWall(new GridWall(b2, GridDirection.EAST));
        MovementRequest request = new MovementRequest("adv-01", b2, 1);

        Map<String, ReachableCell> reachable = byCell(service.calculateReachableCells(grid, request));

        assertFalse(reachable.containsKey("C3"));
    }

    private Map<String, ReachableCell> byCell(List<ReachableCell> reachableCells) {
        return reachableCells.stream()
                .collect(Collectors.toMap(cell -> cell.position().toCell(), Function.identity()));
    }
}
