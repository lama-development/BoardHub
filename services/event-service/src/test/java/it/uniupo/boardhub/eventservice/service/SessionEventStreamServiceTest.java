package it.uniupo.boardhub.eventservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.uniupo.boardhub.eventservice.model.GameEvent;
import it.uniupo.boardhub.eventservice.model.piece.RepresentationMode;
import it.uniupo.boardhub.eventservice.model.piece.SessionPiece;
import it.uniupo.boardhub.eventservice.repository.SessionPieceRepository;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SessionEventStreamServiceTest {

    private static final String SESSION_ID = "session-events-001";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void separaProiezioniPubblicaGiocatoreEDm() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID characterId = UUID.randomUUID();
        UUID pieceId = UUID.randomUUID();
        SessionPiece piece = new SessionPiece(
                pieceId, SESSION_ID, characterId, ownerId, RepresentationMode.VIRTUAL,
                "B2", 0, OffsetDateTime.parse("2026-08-08T10:00:00Z"),
                OffsetDateTime.parse("2026-08-08T10:00:00Z")
        );
        SessionPieceRepository pieces = new SessionPieceRepository(null) {
            @Override
            public List<SessionPiece> findByParticipant(String sessionId, UUID participantId) {
                return ownerId.equals(participantId) ? List.of(piece) : List.of();
            }
        };
        SessionEventStreamService service = new SessionEventStreamService(pieces);
        GameEvent trap = event(
                "trap-1", "TRAP_TRIGGERED", 1,
                """
                        {"sessionPieceId":"%s","characterId":"%s","to":"C2","trapId":"secret"}
                        """.formatted(pieceId, characterId)
        );
        GameEvent move = event(
                "move-1", "MOVE_CONFIRMED", 2,
                """
                        {"sessionPieceId":"%s","characterId":"%s","from":"B2","to":"C2",
                         "actorRole":"PLAYER","actorId":"%s","saveDc":18}
                        """.formatted(pieceId, characterId, ownerId)
        );

        assertThat(service.projectPublic(List.of(trap, move)))
                .singleElement()
                .satisfies(projected -> {
                    assertThat(projected.eventType()).isEqualTo("MOVE_CONFIRMED");
                    assertThat(projected.payload().has("actorId")).isFalse();
                    assertThat(projected.payload().has("saveDc")).isFalse();
                });
        assertThat(service.projectPlayer(List.of(trap, move), ownerId))
                .hasSize(2)
                .allSatisfy(projected -> {
                    assertThat(projected.payload().has("trapId")).isFalse();
                    assertThat(projected.payload().has("saveDc")).isFalse();
                    assertThat(projected.payload().has("actorId")).isFalse();
                });
        assertThat(service.projectPlayer(List.of(trap, move), UUID.randomUUID())).isEmpty();
        assertThat(service.projectDm(List.of(trap, move)).get(0).payload().path("trapId").asText())
                .isEqualTo("secret");
    }

    private GameEvent event(String eventId, String eventType, long sequence, String payload)
            throws Exception {
        return new GameEvent(
                eventId, eventType, "venue-01", "table-01", SESSION_ID, "BACKEND",
                "2026-08-08T10:00:00Z", sequence, objectMapper.readTree(payload)
        );
    }
}
