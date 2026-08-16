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
        int strengthSave,
        int dexteritySave,
        int constitutionSave,
        int intelligenceSave,
        int wisdomSave,
        int charismaSave,
        CharacterTacticalStatus tacticalStatus,
        PartyVisibility partyVisibility,
        long version,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public int saveBonus(it.uniupo.boardhub.eventservice.model.trap.TrapDefinition.SaveAbility ability) {
        return switch (ability) {
            case STRENGTH -> strengthSave;
            case DEXTERITY -> dexteritySave;
            case CONSTITUTION -> constitutionSave;
            case INTELLIGENCE -> intelligenceSave;
            case WISDOM -> wisdomSave;
            case CHARISMA -> charismaSave;
        };
    }

    public PlayerCharacter(
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
        this(
                characterId, sessionId, participantId, name, species, age, className,
                level, speedCells, hpCurrent, hpMax, armorClass,
                0, 0, 0, 0, 0, 0, CharacterTacticalStatus.ACTIVE,
                partyVisibility, version, createdAt, updatedAt
        );
    }
}
