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
            boolean armed,
            String lifecyclePolicy,
            String saveAbility,
            Integer saveDc,
            String rollMode,
            String damageExpression,
            String successDamage,
            String successMovement,
            String failureDamage,
            String failureMovement
    ) {
        public TrapConfiguration(String trapId, String cell, String visibility, boolean armed) {
            this(
                    trapId, cell, visibility, armed, "ONE_SHOT", "DEXTERITY", 10,
                    "NORMAL", "1d6", "NONE", "CONTINUE", "FULL", "STOP"
            );
        }
    }
}
