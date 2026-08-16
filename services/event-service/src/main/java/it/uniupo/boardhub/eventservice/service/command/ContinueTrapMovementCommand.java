package it.uniupo.boardhub.eventservice.service.command;

import java.util.UUID;

public record ContinueTrapMovementCommand(Long expectedVersion, UUID commandId) {
}
