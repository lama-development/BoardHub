package it.uniupo.boardhub.eventservice.model.session;

import it.uniupo.boardhub.eventservice.model.grid.TerrainType;

// Stato persistito di una singola cella configurata dal Dungeon Master.
public record GridCellState(
        String sessionId,
        String cell,
        TerrainType terrainType,
        String occupiedBy
) {
}
