package it.uniupo.boardhub.eventservice.model.grid;

import java.util.Objects;

public record MovementRequest(String characterId, GridPosition start, int movementPoints) {

    public static final int MAX_MOVEMENT_POINTS = 100;

    public MovementRequest {
        if (characterId == null || characterId.isBlank()) {
            throw new IllegalArgumentException("L'identificativo del personaggio e obbligatorio.");
        }
        Objects.requireNonNull(start, "La posizione iniziale e obbligatoria.");
        if (movementPoints < 0) {
            throw new IllegalArgumentException("I punti movimento non possono essere negativi.");
        }
        if (movementPoints > MAX_MOVEMENT_POINTS) {
            throw new IllegalArgumentException(
                    "I punti movimento non possono superare " + MAX_MOVEMENT_POINTS + "."
            );
        }
    }
}
