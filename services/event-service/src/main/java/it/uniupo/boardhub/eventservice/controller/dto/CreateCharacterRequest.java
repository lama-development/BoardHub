package it.uniupo.boardhub.eventservice.controller.dto;

public record CreateCharacterRequest(
        String name,
        String species,
        Integer age,
        String className,
        Integer level,
        Integer speedCells,
        Integer hpCurrent,
        Integer hpMax,
        Integer armorClass,
        Integer strengthSave,
        Integer dexteritySave,
        Integer constitutionSave,
        Integer intelligenceSave,
        Integer wisdomSave,
        Integer charismaSave,
        String partyVisibility
) {
}
