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
        String partyVisibility
) {
}
