package it.uniupo.boardhub.eventservice.model.grid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Configurazione applicativa della griglia, indipendente dal trasporto HTTP.
public record GridConfiguration(
        int width,
        int height,
        List<String> difficultCells,
        List<String> blockedCells,
        List<String> obstacleCells,
        List<String> occupiedCells,
        List<WallConfiguration> walls,
        List<TrapConfiguration> traps
) {

    public GridConfiguration {
        difficultCells = immutableCopy(difficultCells);
        blockedCells = immutableCopy(blockedCells);
        obstacleCells = immutableCopy(obstacleCells);
        occupiedCells = immutableCopy(occupiedCells);
        walls = immutableCopy(walls);
        traps = immutableCopy(traps);
    }

    private static <T> List<T> immutableCopy(List<T> values) {
        return values == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(values));
    }

    public record WallConfiguration(String cell, String direction) {
    }

    public record TrapConfiguration(
            String trapId,
            String cell,
            String visibility,
            boolean armed
    ) {
    }
}
