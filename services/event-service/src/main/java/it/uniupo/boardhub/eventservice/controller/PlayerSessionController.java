package it.uniupo.boardhub.eventservice.controller;

import it.uniupo.boardhub.eventservice.controller.dto.CharacterResponse;
import it.uniupo.boardhub.eventservice.controller.dto.CreateCharacterRequest;
import it.uniupo.boardhub.eventservice.controller.dto.ParticipantResponse;
import it.uniupo.boardhub.eventservice.controller.mapper.CharacterDtoMapper;
import it.uniupo.boardhub.eventservice.controller.mapper.JoinDtoMapper;
import it.uniupo.boardhub.eventservice.service.CharacterService;
import it.uniupo.boardhub.eventservice.service.ParticipantAccessService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/api/v1/player/sessions/{sessionId}")
public class PlayerSessionController {

    private final ParticipantAccessService participantAccessService;
    private final CharacterService characterService;

    public PlayerSessionController(
            ParticipantAccessService participantAccessService,
            CharacterService characterService
    ) {
        this.participantAccessService = participantAccessService;
        this.characterService = characterService;
    }

    // Verifica la credenziale e restituisce l'identita attiva del giocatore nella sessione.
    @GetMapping("/me")
    public ParticipantResponse getCurrentParticipant(
            @PathVariable String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return JoinDtoMapper.toResponse(
                participantAccessService.requireActiveParticipant(sessionId, authorization)
        );
    }

    // Crea un personaggio posseduto dal partecipante identificato dal token.
    @PostMapping("/characters")
    @ResponseStatus(HttpStatus.CREATED)
    public CharacterResponse createCharacter(
            @PathVariable String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody CreateCharacterRequest request
    ) {
        return CharacterDtoMapper.toResponse(
                characterService.create(
                        sessionId,
                        authorization,
                        CharacterDtoMapper.toCommand(request)
                )
        );
    }

    // Elenca soltanto i personaggi appartenenti al partecipante autenticato.
    @GetMapping("/characters")
    public List<CharacterResponse> listOwnedCharacters(
            @PathVariable String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        return characterService.listOwned(sessionId, authorization).stream()
                .map(CharacterDtoMapper::toResponse)
                .toList();
    }
}
