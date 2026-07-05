package it.uniupo.boardhub.eventservice.controller.dto;

import java.util.List;

public record ReachableCellResponse(
        String cell,
        int cost,
        List<String> path,
        List<String> trapsOnPath
) {
}
