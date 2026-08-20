package it.uniupo.boardhub.statsservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import it.uniupo.boardhub.statsservice.model.PlayerResult;
import it.uniupo.boardhub.statsservice.model.PlayerStatistics;
import it.uniupo.boardhub.statsservice.model.SessionResult;
import it.uniupo.boardhub.statsservice.repository.SessionResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Accoglie i risultati pubblicati dal servizio di gioco e li rende consultabili.
@Service
public class SessionResultIngestionService {

    private static final Logger log = LoggerFactory.getLogger(SessionResultIngestionService.class);

    private final SessionResultRepository repository;

    public SessionResultIngestionService(SessionResultRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public boolean ingest(SessionResult result) {
        boolean stored = repository.save(result);
        if (stored) {
            log.info(
                    "Risultato registrato per la sessione {} con {} partecipanti.",
                    result.sessionId(), result.participants().size()
            );
        } else {
            log.info("Risultato gia noto per la sessione {}: consegna ripetuta ignorata.", result.sessionId());
        }
        return stored;
    }

    // Traduce il fatto MQTT nel modello del servizio, assegnando i punteggi.
    public SessionResult parse(JsonNode fact) {
        String sessionId = requireText(fact, "sessionId");
        List<PlayerResult> participants = new ArrayList<>();
        JsonNode nodes = fact.path("participants");
        if (nodes.isArray()) {
            for (JsonNode node : nodes) {
                PlayerResult player = new PlayerResult(
                        sessionId,
                        requireText(node, "playerReference"),
                        node.path("displayName").asText("Giocatore"),
                        node.path("characterName").asText(null),
                        node.path("className").asText(null),
                        node.path("species").asText(null),
                        node.path("level").asInt(0),
                        node.path("survived").asBoolean(true),
                        node.path("movesConfirmed").asInt(0),
                        node.path("cellsTravelled").asInt(0),
                        node.path("trapsTriggered").asInt(0),
                        node.path("savesSucceeded").asInt(0),
                        node.path("savesFailed").asInt(0),
                        node.path("damageTaken").asInt(0),
                        0
                );
                participants.add(withPoints(player));
            }
        }

        return new SessionResult(
                sessionId,
                requireText(fact, "venueId"),
                fact.path("tableId").asText(""),
                fact.path("title").asText("Sessione"),
                fact.path("gameType").asText("DND"),
                OffsetDateTime.parse(requireText(fact, "startedAt")),
                OffsetDateTime.parse(requireText(fact, "endedAt")),
                Math.max(0, fact.path("durationMinutes").asLong(0)),
                participants
        );
    }

    private PlayerResult withPoints(PlayerResult player) {
        return new PlayerResult(
                player.sessionId(), player.playerReference(), player.displayName(),
                player.characterName(), player.className(), player.species(),
                player.level(), player.survived(), player.movesConfirmed(),
                player.cellsTravelled(), player.trapsTriggered(),
                player.savesSucceeded(), player.savesFailed(), player.damageTaken(),
                ScoringRule.pointsFor(player)
        );
    }

    public Optional<SessionResult> findSession(String sessionId) {
        return repository.findBySessionId(sessionId);
    }

    public List<SessionResult> findAllSessions() {
        return repository.findAll();
    }

    public Optional<PlayerStatistics> findPlayer(String playerReference) {
        return repository.findPlayerStatistics(playerReference);
    }

    private String requireText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException("Campo obbligatorio assente nel fatto ricevuto: " + field);
        }
        return value.asText();
    }
}
