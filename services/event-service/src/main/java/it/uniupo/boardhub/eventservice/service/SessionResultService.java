package it.uniupo.boardhub.eventservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import it.uniupo.boardhub.eventservice.model.GameEvent;
import it.uniupo.boardhub.eventservice.model.character.CharacterTacticalStatus;
import it.uniupo.boardhub.eventservice.model.character.PlayerCharacter;
import it.uniupo.boardhub.eventservice.model.join.ParticipantRole;
import it.uniupo.boardhub.eventservice.model.join.SessionParticipant;
import it.uniupo.boardhub.eventservice.model.result.PlayerResult;
import it.uniupo.boardhub.eventservice.model.result.SessionResult;
import it.uniupo.boardhub.eventservice.model.session.GameSession;
import it.uniupo.boardhub.eventservice.model.trap.TrapResolution;
import it.uniupo.boardhub.eventservice.repository.CharacterRepository;
import it.uniupo.boardhub.eventservice.repository.GameEventRepository;
import it.uniupo.boardhub.eventservice.repository.SessionParticipantRepository;
import it.uniupo.boardhub.eventservice.repository.TrapResolutionRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

// Costruisce il riepilogo di una sessione prima che la chiusura ne alteri i dati.
@Service
public class SessionResultService {

    private static final String MOVE_CONFIRMED = "MOVE_CONFIRMED";

    private final SessionParticipantRepository participantRepository;
    private final CharacterRepository characterRepository;
    private final GameEventRepository eventRepository;
    private final TrapResolutionRepository trapResolutionRepository;

    public SessionResultService(
            SessionParticipantRepository participantRepository,
            CharacterRepository characterRepository,
            GameEventRepository eventRepository,
            TrapResolutionRepository trapResolutionRepository
    ) {
        this.participantRepository = participantRepository;
        this.characterRepository = characterRepository;
        this.eventRepository = eventRepository;
        this.trapResolutionRepository = trapResolutionRepository;
    }

    public SessionResult build(GameSession session, OffsetDateTime endedAt) {
        List<SessionParticipant> participants = participantRepository
                .findActiveBySessionId(session.sessionId());
        List<PlayerCharacter> characters = characterRepository.findBySession(session.sessionId());
        List<GameEvent> events = eventRepository.findBySessionId(session.sessionId());
        List<TrapResolution> resolutions = trapResolutionRepository
                .findAllBySession(session.sessionId());

        Map<UUID, List<PlayerCharacter>> charactersByParticipant = characters.stream()
                .collect(Collectors.groupingBy(PlayerCharacter::participantId));

        List<PlayerResult> results = new ArrayList<>();
        for (SessionParticipant participant : participants) {
            if (participant.role() != ParticipantRole.PLAYER) {
                continue;
            }
            // Un partecipante senza personaggio compare comunque, con misure a zero.
            List<PlayerCharacter> owned = charactersByParticipant
                    .getOrDefault(participant.participantId(), List.of());
            if (owned.isEmpty()) {
                results.add(emptyResult(participant));
                continue;
            }
            for (PlayerCharacter character : owned) {
                results.add(buildFor(participant, character, events, resolutions));
            }
        }

        return new SessionResult(
                session.sessionId(),
                session.venueId(),
                session.tableId(),
                session.title(),
                session.gameType(),
                session.createdAt(),
                endedAt,
                Math.max(0, Duration.between(session.createdAt(), endedAt).toMinutes()),
                results
        );
    }

    private PlayerResult emptyResult(SessionParticipant participant) {
        return new PlayerResult(
                participant.playerReference(), participant.displayName(),
                null, null, null, 0, true, 0, 0, 0, 0, 0, 0
        );
    }

    private PlayerResult buildFor(
            SessionParticipant participant,
            PlayerCharacter character,
            List<GameEvent> events,
            List<TrapResolution> resolutions
    ) {
        String characterId = character.characterId().toString();

        int moves = 0;
        int cells = 0;
        for (GameEvent event : events) {
            if (!MOVE_CONFIRMED.equals(event.eventType())) {
                continue;
            }
            JsonNode payload = event.payload();
            if (!characterId.equals(payload.path("characterId").asText(null))) {
                continue;
            }
            moves++;
            cells += payload.path("cost").asInt(0);
        }

        int traps = 0;
        int succeeded = 0;
        int failed = 0;
        int damage = 0;
        for (TrapResolution resolution : resolutions) {
            if (!character.characterId().equals(resolution.characterId())) {
                continue;
            }
            traps++;
            if (Boolean.TRUE.equals(resolution.saveSuccess())) {
                succeeded++;
            } else if (Boolean.FALSE.equals(resolution.saveSuccess())) {
                failed++;
            }
            if (resolution.damageTotal() != null) {
                damage += resolution.damageTotal();
            }
        }

        return new PlayerResult(
                participant.playerReference(),
                participant.displayName(),
                character.name(),
                character.className(),
                character.species(),
                character.level(),
                character.tacticalStatus() != CharacterTacticalStatus.DOWNED,
                moves,
                cells,
                traps,
                succeeded,
                failed,
                damage
        );
    }
}
