package it.uniupo.boardhub.eventservice.service;

import it.uniupo.boardhub.eventservice.config.CharacterProperties;
import it.uniupo.boardhub.eventservice.model.character.PartyVisibility;
import it.uniupo.boardhub.eventservice.model.character.PlayerCharacter;
import it.uniupo.boardhub.eventservice.model.join.SessionParticipant;
import it.uniupo.boardhub.eventservice.repository.CharacterRepository;
import it.uniupo.boardhub.eventservice.repository.GameSessionRepository;
import it.uniupo.boardhub.eventservice.service.command.CreateCharacterCommand;
import it.uniupo.boardhub.eventservice.service.exception.CharacterCapacityException;
import it.uniupo.boardhub.eventservice.service.exception.GameSessionNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class CharacterService {

    private static final int MAX_TEXT_LENGTH = 80;
    private static final int MAX_SESSION_ID_LENGTH = 100;
    private static final int MAX_SPEED_CELLS = 100;
    private static final int MAX_HIT_POINTS = 1_000_000;
    private static final int MAX_ARMOR_CLASS = 100;

    private final ParticipantAccessService participantAccessService;
    private final CharacterRepository characterRepository;
    private final GameSessionRepository sessionRepository;
    private final CharacterProperties properties;
    private final Clock clock;

    public CharacterService(
            ParticipantAccessService participantAccessService,
            CharacterRepository characterRepository,
            GameSessionRepository sessionRepository,
            CharacterProperties properties,
            Clock clock
    ) {
        this.participantAccessService = participantAccessService;
        this.characterRepository = characterRepository;
        this.sessionRepository = sessionRepository;
        this.properties = properties;
        this.clock = clock;
        if (properties.maxPerParticipant() < 1 || properties.maxPerParticipant() > 64) {
            throw new IllegalStateException(
                    "boardhub.characters.max-per-participant deve essere compreso tra 1 e 64."
            );
        }
    }

    // Crea un personaggio per il partecipante autenticato e serializza il controllo del limite.
    @Transactional
    public PlayerCharacter create(
            String sessionId,
            String authorizationHeader,
            CreateCharacterCommand command
    ) {
        SessionParticipant participant = participantAccessService.requireActiveParticipantForUpdate(
                sessionId,
                authorizationHeader
        );
        if (characterRepository.countByParticipant(sessionId, participant.participantId())
                >= properties.maxPerParticipant()) {
            throw new CharacterCapacityException(properties.maxPerParticipant());
        }

        CharacterData data = validate(command);
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        PlayerCharacter character = new PlayerCharacter(
                UUID.randomUUID(),
                sessionId,
                participant.participantId(),
                data.name(),
                data.species(),
                data.age(),
                data.className(),
                data.level(),
                data.speedCells(),
                data.hpCurrent(),
                data.hpMax(),
                data.armorClass(),
                data.partyVisibility(),
                0,
                now,
                now
        );
        characterRepository.save(character);
        return character;
    }

    // Restituisce al giocatore esclusivamente i personaggi che possiede nella sessione.
    public List<PlayerCharacter> listOwned(String sessionId, String authorizationHeader) {
        SessionParticipant participant = participantAccessService.requireActiveParticipant(
                sessionId,
                authorizationHeader
        );
        return characterRepository.findByParticipant(sessionId, participant.participantId());
    }

    // Restituisce al DM la proiezione completa dei personaggi della sessione.
    public List<PlayerCharacter> listForDm(String sessionId) {
        String normalizedSessionId = requireSessionId(sessionId);
        sessionRepository.findSessionById(normalizedSessionId)
                .orElseThrow(() -> new GameSessionNotFoundException(normalizedSessionId));
        return characterRepository.findBySession(normalizedSessionId);
    }

    private CharacterData validate(CreateCharacterCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Il corpo della richiesta e obbligatorio.");
        }
        String name = requireText(command.name(), "name");
        String species = requireText(command.species(), "species");
        String className = requireText(command.className(), "className");
        Integer age = optionalPositive(command.age(), "age");
        int level = requiredRange(command.level(), "level", 1, 20);
        int speedCells = requiredRange(command.speedCells(), "speedCells", 0, MAX_SPEED_CELLS);
        int hpMax = requiredRange(command.hpMax(), "hpMax", 1, MAX_HIT_POINTS);
        int hpCurrent = command.hpCurrent() == null
                ? hpMax
                : requiredRange(command.hpCurrent(), "hpCurrent", 0, hpMax);
        int armorClass = requiredRange(command.armorClass(), "armorClass", 0, MAX_ARMOR_CLASS);
        PartyVisibility visibility = parseVisibility(command.partyVisibility());
        return new CharacterData(
                name, species, age, className, level, speedCells,
                hpCurrent, hpMax, armorClass, visibility
        );
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " e obbligatorio.");
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException(
                    field + " non puo superare " + MAX_TEXT_LENGTH + " caratteri."
            );
        }
        return normalized;
    }

    private String requireSessionId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("sessionId e obbligatorio.");
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_SESSION_ID_LENGTH) {
            throw new IllegalArgumentException(
                    "sessionId non puo superare " + MAX_SESSION_ID_LENGTH + " caratteri."
            );
        }
        return normalized;
    }

    private int requiredRange(Integer value, String field, int minimum, int maximum) {
        if (value == null) {
            throw new IllegalArgumentException(field + " e obbligatorio.");
        }
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(
                    field + " deve essere compreso tra " + minimum + " e " + maximum + "."
            );
        }
        return value;
    }

    private Integer optionalPositive(Integer value, String field) {
        if (value == null) {
            return null;
        }
        if (value < 1) {
            throw new IllegalArgumentException(field + " deve essere maggiore di zero.");
        }
        return value;
    }

    private PartyVisibility parseVisibility(String value) {
        if (value == null || value.isBlank()) {
            return PartyVisibility.OWNER_ONLY;
        }
        try {
            return PartyVisibility.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "partyVisibility deve essere OWNER_ONLY, PARTY oppure DM_ONLY."
            );
        }
    }

    private record CharacterData(
            String name,
            String species,
            Integer age,
            String className,
            int level,
            int speedCells,
            int hpCurrent,
            int hpMax,
            int armorClass,
            PartyVisibility partyVisibility
    ) {
    }
}
