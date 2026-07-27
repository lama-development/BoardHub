package it.uniupo.boardhub.eventservice.controller.dto;

import java.util.UUID;

public record CharacterResponse(
        UUID characterId,
        String sessionId,
        UUID participantId,
        String name,
        String species,
        Integer age,
        String className,
        int level,
        int speedCells,
        int hpCurrent,
        int hpMax,
        int armorClass,
        String partyVisibility,
        long version,
        String createdAt,
        String updatedAt
) {
}
