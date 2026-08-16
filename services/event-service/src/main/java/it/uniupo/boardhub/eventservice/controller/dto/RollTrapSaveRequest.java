package it.uniupo.boardhub.eventservice.controller.dto;

import java.util.UUID;

public record RollTrapSaveRequest(Long expectedVersion, UUID commandId) {
}
