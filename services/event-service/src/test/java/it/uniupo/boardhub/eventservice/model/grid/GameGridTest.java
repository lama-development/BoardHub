package it.uniupo.boardhub.eventservice.model.grid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameGridTest {

    @Test
    void restituisceCellaNormaleQuandoNonConfigurata() {
        GameGrid grid = GameGrid.empty(5, 5);
        GridCell cell = grid.cellAt(GridPosition.fromCell("C3"));

        assertEquals(TerrainType.NORMAL, cell.terrainType());
        assertTrue(cell.isWalkable());
        assertEquals(1, cell.movementCost());
    }

    @Test
    void distingueTerrenoDifficileOstacoliECelleInaccessibili() {
        GameGrid grid = GameGrid.empty(5, 5)
                .withCell(new GridCell(GridPosition.fromCell("B2"), TerrainType.DIFFICULT, false))
                .withCell(new GridCell(GridPosition.fromCell("B3"), TerrainType.OBSTACLE, false))
                .withCell(new GridCell(GridPosition.fromCell("B5"), TerrainType.BLOCKED, false))
                .withCell(new GridCell(GridPosition.fromCell("B4"), TerrainType.NORMAL, true));

        assertEquals(2, grid.cellAt(GridPosition.fromCell("B2")).movementCost());
        assertTrue(grid.cellAt(GridPosition.fromCell("B2")).isWalkable());
        assertFalse(grid.cellAt(GridPosition.fromCell("B3")).isWalkable());
        assertFalse(grid.cellAt(GridPosition.fromCell("B4")).isWalkable());
        assertFalse(grid.cellAt(GridPosition.fromCell("B5")).isWalkable());
    }

    @Test
    void rappresentaMuriTraDueCelleAdiacenti() {
        GridPosition a1 = GridPosition.fromCell("A1");
        GridPosition b1 = GridPosition.fromCell("B1");
        GameGrid grid = GameGrid.empty(3, 3)
                .withWall(new GridWall(a1, GridDirection.EAST));

        assertTrue(grid.cellAt(a1).isWalkable());
        assertTrue(grid.cellAt(b1).isWalkable());
        assertTrue(grid.hasWallBetween(a1, b1));
        assertTrue(grid.hasWallBetween(b1, a1));
    }

    @Test
    void distingueTrappoleDaOstacoliECelleBloccate() {
        GridPosition c2 = GridPosition.fromCell("C2");
        GameGrid grid = GameGrid.empty(5, 5)
                .withTrap(new GridTrap("trap-01", c2, TrapVisibility.HIDDEN, true));

        assertTrue(grid.cellAt(c2).isWalkable());
        assertEquals("trap-01", grid.trapAt(c2).trapId());
        assertFalse(grid.trapAt(c2).isVisibleToPlayers());
        assertTrue(grid.trapAt(c2).canBeRevealed());
    }

    @Test
    void supportaTrappoleSempreNascoste() {
        GridPosition d4 = GridPosition.fromCell("D4");
        GameGrid grid = GameGrid.empty(5, 5)
                .withTrap(new GridTrap("trap-02", d4, TrapVisibility.ALWAYS_HIDDEN, true));

        assertTrue(grid.cellAt(d4).isWalkable());
        assertFalse(grid.trapAt(d4).isVisibleToPlayers());
        assertFalse(grid.trapAt(d4).canBeRevealed());
    }

    @Test
    void rifiutaCelleFuoriDallaGriglia() {
        GameGrid grid = GameGrid.empty(2, 2);

        assertThrows(IllegalArgumentException.class, () -> grid.cellAt(GridPosition.fromCell("C1")));
    }
}
