package it.uniupo.boardhub.eventservice.service.command;

// Dati applicativi richiesti per creare la scheda tattica minima.
public record CreateCharacterCommand(
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
