package it.uniupo.boardhub.eventservice.controller.mapper;

import it.uniupo.boardhub.eventservice.controller.dto.ReachableCellResponse;
import it.uniupo.boardhub.eventservice.model.grid.GridPosition;
import it.uniupo.boardhub.eventservice.model.grid.GridTrap;
import it.uniupo.boardhub.eventservice.model.grid.ReachableCell;

public final class MovementDtoMapper {

    private MovementDtoMapper() {
    }

    public static ReachableCellResponse toResponse(ReachableCell reachableCell) {
        return new ReachableCellResponse(
                reachableCell.position().toCell(),
                reachableCell.cost(),
                reachableCell.path().stream().map(GridPosition::toCell).toList(),
                reachableCell.trapsOnPath().stream()
                        .filter(GridTrap::isVisibleToPlayers)
                        .map(GridTrap::trapId)
                        .toList()
        );
    }
}
