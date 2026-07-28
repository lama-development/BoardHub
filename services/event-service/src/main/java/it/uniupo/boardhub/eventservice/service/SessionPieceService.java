package it.uniupo.boardhub.eventservice.service;

import it.uniupo.boardhub.eventservice.model.character.PlayerCharacter;
import it.uniupo.boardhub.eventservice.model.grid.GameGrid;
import it.uniupo.boardhub.eventservice.model.grid.GridPosition;
import it.uniupo.boardhub.eventservice.model.join.SessionParticipant;
import it.uniupo.boardhub.eventservice.model.piece.RepresentationMode;
import it.uniupo.boardhub.eventservice.model.piece.SessionPiece;
import it.uniupo.boardhub.eventservice.repository.CharacterRepository;
import it.uniupo.boardhub.eventservice.repository.GameSessionRepository;
import it.uniupo.boardhub.eventservice.repository.SessionPieceRepository;
import it.uniupo.boardhub.eventservice.service.command.CreateSessionPieceCommand;
import it.uniupo.boardhub.eventservice.service.exception.CharacterNotFoundException;
import it.uniupo.boardhub.eventservice.service.exception.GameSessionNotFoundException;
import it.uniupo.boardhub.eventservice.service.exception.SessionPieceConflictException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class SessionPieceService {

    private static final int MAX_SESSION_ID_LENGTH = 100;

    private final ParticipantAccessService participantAccessService;
    private final CharacterRepository characterRepository;
    private final SessionPieceRepository pieceRepository;
    private final GameSessionRepository sessionRepository;
    private final SessionGridService gridService;
    private final Clock clock;

    public SessionPieceService(
            ParticipantAccessService participantAccessService,
            CharacterRepository characterRepository,
            SessionPieceRepository pieceRepository,
            GameSessionRepository sessionRepository,
            SessionGridService gridService,
            Clock clock
    ) {
        this.participantAccessService = participantAccessService;
        this.characterRepository = characterRepository;
        this.pieceRepository = pieceRepository;
        this.sessionRepository = sessionRepository;
        this.gridService = gridService;
        this.clock = clock;
    }

    // Registra una sola pedina virtuale per personaggio e serializza l'occupazione della cella.
    @Transactional
    public SessionPiece create(
            String sessionId,
            String authorizationHeader,
            CreateSessionPieceCommand command
    ) {
        String normalizedSessionId = requireSessionId(sessionId);
        SessionParticipant participant =
                participantAccessService.requireActiveParticipantForUpdate(
                        normalizedSessionId,
                        authorizationHeader
                );
        PieceData data = validate(command);
        PlayerCharacter character = characterRepository.findByIdAndSession(
                        data.characterId(),
                        normalizedSessionId
                )
                .filter(item -> item.participantId().equals(participant.participantId()))
                .orElseThrow(CharacterNotFoundException::new);

        if (pieceRepository.existsByCharacter(normalizedSessionId, character.characterId())) {
            throw new SessionPieceConflictException(
                    "Il personaggio possiede gia una pedina nella sessione."
            );
        }

        GameGrid grid = gridService.loadGrid(normalizedSessionId);
        if (!grid.contains(data.position())) {
            throw new IllegalArgumentException(
                    "startCell non appartiene alla griglia della sessione."
            );
        }
        if (!grid.cellAt(data.position()).isWalkable()) {
            throw new SessionPieceConflictException(
                    "La cella iniziale non e disponibile: " + data.cell()
            );
        }

        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        SessionPiece piece = new SessionPiece(
                UUID.randomUUID(),
                normalizedSessionId,
                character.characterId(),
                participant.participantId(),
                data.representationMode(),
                data.cell(),
                0,
                now,
                now
        );
        try {
            pieceRepository.save(piece);
        } catch (DuplicateKeyException ex) {
            throw new SessionPieceConflictException(
                    "Il personaggio o la cella iniziale sono gia associati a una pedina."
            );
        }
        return piece;
    }

    public List<SessionPiece> listOwned(String sessionId, String authorizationHeader) {
        String normalizedSessionId = requireSessionId(sessionId);
        SessionParticipant participant = participantAccessService.requireActiveParticipant(
                normalizedSessionId,
                authorizationHeader
        );
        return pieceRepository.findByParticipant(
                normalizedSessionId,
                participant.participantId()
        );
    }

    public List<SessionPiece> listForDm(String sessionId) {
        String normalizedSessionId = requireSessionId(sessionId);
        sessionRepository.findSessionById(normalizedSessionId)
                .orElseThrow(() -> new GameSessionNotFoundException(normalizedSessionId));
        return pieceRepository.findBySession(normalizedSessionId);
    }

    private PieceData validate(CreateSessionPieceCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Il corpo della richiesta e obbligatorio.");
        }
        if (command.characterId() == null) {
            throw new IllegalArgumentException("characterId e obbligatorio.");
        }
        RepresentationMode mode = parseRepresentationMode(command.representationMode());
        if (command.startCell() == null || command.startCell().isBlank()) {
            throw new IllegalArgumentException("startCell e obbligatorio.");
        }
        GridPosition position = GridPosition.fromCell(command.startCell().trim());
        return new PieceData(command.characterId(), mode, position, position.toCell());
    }

    private RepresentationMode parseRepresentationMode(String value) {
        if (value == null || value.isBlank()) {
            return RepresentationMode.VIRTUAL;
        }
        try {
            return RepresentationMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "representationMode supporta attualmente soltanto VIRTUAL."
            );
        }
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

    private record PieceData(
            UUID characterId,
            RepresentationMode representationMode,
            GridPosition position,
            String cell
    ) {
    }
}
