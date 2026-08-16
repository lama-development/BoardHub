package it.uniupo.boardhub.eventservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.uniupo.boardhub.eventservice.model.GameEvent;
import it.uniupo.boardhub.eventservice.model.trap.CharacterControl;
import it.uniupo.boardhub.eventservice.repository.CharacterControlRepository;
import it.uniupo.boardhub.eventservice.repository.CharacterRepository;
import it.uniupo.boardhub.eventservice.repository.GameEventRepository;
import it.uniupo.boardhub.eventservice.repository.GameSessionRepository;
import it.uniupo.boardhub.eventservice.service.exception.CharacterControlConflictException;
import it.uniupo.boardhub.eventservice.service.exception.CharacterNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class CharacterControlService {

    private final DmAccessService dmAccessService;
    private final CharacterRepository characterRepository;
    private final CharacterControlRepository controlRepository;
    private final GameSessionRepository sessionRepository;
    private final GameEventRepository eventRepository;
    private final SessionEventStreamService eventStreamService;
    private final Clock clock;

    public CharacterControlService(
            DmAccessService dmAccessService,
            CharacterRepository characterRepository,
            CharacterControlRepository controlRepository,
            GameSessionRepository sessionRepository,
            GameEventRepository eventRepository,
            SessionEventStreamService eventStreamService,
            Clock clock
    ) {
        this.dmAccessService = dmAccessService;
        this.characterRepository = characterRepository;
        this.controlRepository = controlRepository;
        this.sessionRepository = sessionRepository;
        this.eventRepository = eventRepository;
        this.eventStreamService = eventStreamService;
        this.clock = clock;
    }

    @Transactional
    public CharacterControl assume(String sessionId, UUID characterId, String authorization) {
        var dm = dmAccessService.requireAuthorized(sessionId, authorization);
        characterRepository.findByIdAndSessionForUpdate(characterId, sessionId)
                .orElseThrow(CharacterNotFoundException::new);
        CharacterControl existing = controlRepository.findForUpdate(sessionId, characterId)
                .orElse(null);
        if (existing != null && existing.dmParticipantId().equals(dm.participantId())) {
            return existing;
        }
        CharacterControl control = new CharacterControl(
                sessionId, characterId, dm.participantId(), 0, now()
        );
        controlRepository.saveOrReplace(control);
        CharacterControl saved = controlRepository.find(sessionId, characterId).orElseThrow();
        saveControlEvent("CHARACTER_CONTROL_ASSUMED", saved, dm.participantId());
        return saved;
    }

    @Transactional
    public void release(String sessionId, UUID characterId, String authorization) {
        var dm = dmAccessService.requireAuthorized(sessionId, authorization);
        characterRepository.findByIdAndSessionForUpdate(characterId, sessionId)
                .orElseThrow(CharacterNotFoundException::new);
        CharacterControl existing = controlRepository.findForUpdate(sessionId, characterId)
                .orElse(null);
        if (existing == null) {
            return;
        }
        if (!existing.dmParticipantId().equals(dm.participantId())) {
            throw new CharacterControlConflictException(
                    "Il personaggio e controllato da un altro dispositivo Dungeon Master."
            );
        }
        if (!controlRepository.release(sessionId, characterId, dm.participantId())) {
            throw new CharacterControlConflictException(
                    "Il Dungeon Master non controlla questo personaggio."
            );
        }
        saveControlEvent("CHARACTER_CONTROL_RELEASED", existing, dm.participantId());
    }

    public void requirePlayerControlAvailable(String sessionId, UUID characterId) {
        if (controlRepository.find(sessionId, characterId).isPresent()) {
            throw new CharacterControlConflictException(
                    "Il personaggio e temporaneamente controllato dal Dungeon Master."
            );
        }
    }

    public void requireControlledBy(String sessionId, UUID characterId, UUID dmParticipantId) {
        CharacterControl control = controlRepository.find(sessionId, characterId)
                .orElseThrow(() -> new CharacterControlConflictException(
                        "Il Dungeon Master deve prima assumere il controllo del personaggio."
                ));
        if (!control.dmParticipantId().equals(dmParticipantId)) {
            throw new CharacterControlConflictException(
                    "Il personaggio e controllato da un altro dispositivo Dungeon Master."
            );
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
    }

    private void saveControlEvent(
            String eventType,
            CharacterControl control,
            UUID dmParticipantId
    ) {
        var session = sessionRepository.findSessionById(control.sessionId())
                .orElseThrow(() -> new IllegalStateException(
                        "Sessione scomparsa durante il cambio di controllo."
                ));
        OffsetDateTime occurredAt = now();
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("characterId", control.characterId().toString());
        payload.put("actorRole", "DM");
        payload.put("actorId", dmParticipantId.toString());
        payload.put("controlVersion", control.version());
        GameEvent event = new GameEvent(
                "control-" + UUID.randomUUID(), eventType, session.venueId(), session.tableId(),
                control.sessionId(), "BACKEND", occurredAt.toString(),
                sessionRepository.nextServerEventSequence(control.sessionId()), payload
        );
        try {
            if (!eventRepository.save(event)) {
                throw new CharacterControlConflictException(
                        "Impossibile registrare il cambio di controllo."
                );
            }
            eventStreamService.publishAfterCommit(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                    "Impossibile serializzare il cambio di controllo.", ex
            );
        }
    }
}
