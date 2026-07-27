package it.uniupo.boardhub.eventservice.model.character;

import java.time.OffsetDateTime;
import java.util.UUID;

// Scheda tattica minima posseduta da un partecipante nella singola sessione.
public record PlayerCharacter(
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
        PartyVisibility partyVisibility,
        long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
