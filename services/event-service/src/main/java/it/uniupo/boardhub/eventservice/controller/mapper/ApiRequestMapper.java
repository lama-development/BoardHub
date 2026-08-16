package it.uniupo.boardhub.eventservice.controller.mapper;

import it.uniupo.boardhub.eventservice.controller.dto.CreateGameSessionRequest;
import it.uniupo.boardhub.eventservice.controller.dto.CreateJoinRequestRequest;
import it.uniupo.boardhub.eventservice.controller.dto.MovementGridRequest;
import it.uniupo.boardhub.eventservice.model.grid.GridConfiguration;
import it.uniupo.boardhub.eventservice.service.command.CreateGameSessionCommand;
import it.uniupo.boardhub.eventservice.service.command.CreateJoinRequestCommand;

import java.util.List;

// Converte il contratto HTTP in input applicativi prima di entrare nei service.
public final class ApiRequestMapper {

    private ApiRequestMapper() {
    }

    public static CreateGameSessionCommand toCommand(CreateGameSessionRequest request) {
        if (request == null) {
            return null;
        }
        return new CreateGameSessionCommand(
                request.sessionId(),
                request.venueId(),
                request.tableId(),
                request.tablePublicId(),
                request.tableDisplayName(),
                request.title(),
                request.gameType(),
                request.publicSummary(),
                request.acceptingJoinRequests(),
                toGridConfiguration(request.grid())
        );
    }

    public static CreateJoinRequestCommand toCommand(CreateJoinRequestRequest request) {
        if (request == null) {
            return null;
        }
        return new CreateJoinRequestCommand(request.playerReference(), request.displayName());
    }

    public static GridConfiguration toGridConfiguration(MovementGridRequest request) {
        if (request == null) {
            return null;
        }
        return new GridConfiguration(
                request.width(),
                request.height(),
                request.difficultCells(),
                request.blockedCells(),
                request.obstacleCells(),
                request.occupiedCells(),
                mapWalls(request),
                mapTraps(request)
        );
    }

    private static List<GridConfiguration.WallConfiguration> mapWalls(MovementGridRequest request) {
        if (request.walls() == null) {
            return List.of();
        }
        return request.walls().stream()
                .map(wall -> wall == null
                        ? null
                        : new GridConfiguration.WallConfiguration(wall.cell(), wall.direction()))
                .toList();
    }

    private static List<GridConfiguration.TrapConfiguration> mapTraps(MovementGridRequest request) {
        if (request.traps() == null) {
            return List.of();
        }
        return request.traps().stream()
                .map(trap -> trap == null
                        ? null
                        : new GridConfiguration.TrapConfiguration(
                                trap.trapId(), trap.cell(), trap.visibility(), trap.armed(),
                                defaultText(trap.lifecyclePolicy(), "ONE_SHOT"),
                                defaultText(trap.saveAbility(), "DEXTERITY"),
                                trap.saveDc() == null ? 10 : trap.saveDc(),
                                defaultText(trap.rollMode(), "NORMAL"),
                                defaultText(trap.damageExpression(), "1d6"),
                                defaultText(trap.successDamage(), "NONE"),
                                defaultText(trap.successMovement(), "CONTINUE"),
                                defaultText(trap.failureDamage(), "FULL"),
                                defaultText(trap.failureMovement(), "STOP")
                        ))
                .toList();
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
