package it.uniupo.boardhub.eventservice.controller.mapper;

import it.uniupo.boardhub.eventservice.controller.dto.CreateSessionPieceRequest;
import it.uniupo.boardhub.eventservice.controller.dto.SessionPieceResponse;
import it.uniupo.boardhub.eventservice.model.piece.SessionPiece;
import it.uniupo.boardhub.eventservice.service.command.CreateSessionPieceCommand;

public final class SessionPieceDtoMapper {

    private SessionPieceDtoMapper() {
    }

    public static CreateSessionPieceCommand toCommand(CreateSessionPieceRequest request) {
        if (request == null) {
            return null;
        }
        return new CreateSessionPieceCommand(
                request.characterId(),
                request.representationMode(),
                request.startCell()
        );
    }

    public static SessionPieceResponse toResponse(SessionPiece piece) {
        return new SessionPieceResponse(
                piece.sessionPieceId(),
                piece.sessionId(),
                piece.characterId(),
                piece.participantId(),
                piece.representationMode().name(),
                piece.currentCell(),
                piece.version(),
                piece.createdAt(),
                piece.updatedAt()
        );
    }
}
