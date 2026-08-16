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
        Integer strengthSave,
        Integer dexteritySave,
        Integer constitutionSave,
        Integer intelligenceSave,
        Integer wisdomSave,
        Integer charismaSave,
        String partyVisibility
) {
    public CreateCharacterCommand(
            String name, String species, Integer age, String className, Integer level,
            Integer speedCells, Integer hpCurrent, Integer hpMax, Integer armorClass,
            String partyVisibility
    ) {
        this(
                name, species, age, className, level, speedCells, hpCurrent, hpMax,
                armorClass, 0, 0, 0, 0, 0, 0, partyVisibility
        );
    }
}
