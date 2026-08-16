package it.uniupo.boardhub.eventservice.service.command;

import java.util.UUID;

public record RollTrapSaveCommand(Long expectedVersion, UUID commandId) {
}
