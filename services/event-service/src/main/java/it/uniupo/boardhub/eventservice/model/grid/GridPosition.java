package it.uniupo.boardhub.eventservice.model.grid;

import java.util.Locale;

public record GridPosition(int row, int column) {

    public GridPosition {
        if (row < 1) {
            throw new IllegalArgumentException("La riga deve essere maggiore di zero.");
        }
        if (column < 1) {
            throw new IllegalArgumentException("La colonna deve essere maggiore di zero.");
        }
    }

    public static GridPosition fromCell(String cell) {
        if (cell == null || !cell.matches("[A-Za-z]+[1-9][0-9]*")) {
            throw new IllegalArgumentException("Formato cella non valido: " + cell);
        }

        String normalized = cell.toUpperCase(Locale.ROOT);
        int splitIndex = 0;
        while (splitIndex < normalized.length() && Character.isLetter(normalized.charAt(splitIndex))) {
            splitIndex++;
        }

        String columnLabel = normalized.substring(0, splitIndex);
        int row = Integer.parseInt(normalized.substring(splitIndex));
        return new GridPosition(row, columnFromLabel(columnLabel));
    }

    public String toCell() {
        return labelFromColumn(column) + row;
    }

    public GridPosition move(GridDirection direction) {
        return new GridPosition(row + direction.rowDelta(), column + direction.columnDelta());
    }

    private static int columnFromLabel(String label) {
        int value = 0;
        for (int i = 0; i < label.length(); i++) {
            value = value * 26 + (label.charAt(i) - 'A' + 1);
        }
        return value;
    }

    private static String labelFromColumn(int column) {
        StringBuilder label = new StringBuilder();
        int current = column;
        while (current > 0) {
            current--;
            label.append((char) ('A' + current % 26));
            current /= 26;
        }
        return label.reverse().toString();
    }
}
