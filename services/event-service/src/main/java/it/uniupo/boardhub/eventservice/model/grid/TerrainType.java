package it.uniupo.boardhub.eventservice.model.grid;

public enum TerrainType {
    NORMAL(1, true),
    DIFFICULT(2, true),
    OBSTACLE(Integer.MAX_VALUE, false),
    BLOCKED(Integer.MAX_VALUE, false);

    private final int movementCost;
    private final boolean walkable;

    TerrainType(int movementCost, boolean walkable) {
        this.movementCost = movementCost;
        this.walkable = walkable;
    }

    public int movementCost() {
        return movementCost;
    }

    public boolean isWalkable() {
        return walkable;
    }
}
