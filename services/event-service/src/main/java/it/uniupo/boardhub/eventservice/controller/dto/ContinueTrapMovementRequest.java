package it.uniupo.boardhub.eventservice.controller.dto;

import java.util.UUID;

public record ContinueTrapMovementRequest(Long expectedVersion, UUID commandId) {
}
