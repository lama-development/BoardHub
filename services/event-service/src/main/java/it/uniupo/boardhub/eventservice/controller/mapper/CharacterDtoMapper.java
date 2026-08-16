package it.uniupo.boardhub.eventservice.controller.mapper;

import it.uniupo.boardhub.eventservice.controller.dto.CharacterResponse;
import it.uniupo.boardhub.eventservice.controller.dto.CreateCharacterRequest;
import it.uniupo.boardhub.eventservice.model.character.PlayerCharacter;
import it.uniupo.boardhub.eventservice.service.command.CreateCharacterCommand;

public final class CharacterDtoMapper {

    private CharacterDtoMapper() {
    }

    public static CreateCharacterCommand toCommand(CreateCharacterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Il corpo della richiesta e obbligatorio.");
        }
        return new CreateCharacterCommand(
                request.name(),
                request.species(),
                request.age(),
                request.className(),
                request.level(),
                request.speedCells(),
                request.hpCurrent(),
                request.hpMax(),
                request.armorClass(),
                request.strengthSave(),
                request.dexteritySave(),
                request.constitutionSave(),
                request.intelligenceSave(),
                request.wisdomSave(),
                request.charismaSave(),
                request.partyVisibility()
        );
    }

    public static CharacterResponse toResponse(PlayerCharacter character) {
        return new CharacterResponse(
                character.characterId(),
                character.sessionId(),
                character.participantId(),
                character.name(),
                character.species(),
                character.age(),
                character.className(),
                character.level(),
                character.speedCells(),
                character.hpCurrent(),
                character.hpMax(),
                character.armorClass(),
                character.strengthSave(),
                character.dexteritySave(),
                character.constitutionSave(),
                character.intelligenceSave(),
                character.wisdomSave(),
                character.charismaSave(),
                character.tacticalStatus().name(),
                character.partyVisibility().name(),
                character.version(),
                character.createdAt().toString(),
                character.updatedAt().toString()
        );
    }
}
