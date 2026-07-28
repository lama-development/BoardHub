package it.uniupo.boardhub.eventservice.service;

import it.uniupo.boardhub.eventservice.model.grid.GameGrid;
import it.uniupo.boardhub.eventservice.model.grid.GridCell;
import it.uniupo.boardhub.eventservice.model.grid.GridPosition;
import it.uniupo.boardhub.eventservice.model.grid.GridTrap;
import it.uniupo.boardhub.eventservice.model.grid.GridWall;
import it.uniupo.boardhub.eventservice.model.grid.TerrainType;
import it.uniupo.boardhub.eventservice.model.session.GameSession;
import it.uniupo.boardhub.eventservice.model.session.GridCellState;
import it.uniupo.boardhub.eventservice.model.session.GridTrapState;
import it.uniupo.boardhub.eventservice.model.session.GridWallState;
import it.uniupo.boardhub.eventservice.repository.GameSessionRepository;
import it.uniupo.boardhub.eventservice.repository.SessionPieceRepository;
import it.uniupo.boardhub.eventservice.service.exception.GameSessionNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class SessionGridService {

    private final GameSessionRepository repository;
    private final SessionPieceRepository pieceRepository;

    public SessionGridService(
            GameSessionRepository repository,
            SessionPieceRepository pieceRepository
    ) {
        this.repository = repository;
        this.pieceRepository = pieceRepository;
    }

    // Ricostruisce la griglia con una sola copia immutabile finale.
    public GameGrid loadGrid(String sessionId) {
        GameSession session = repository.findSessionById(sessionId)
                .orElseThrow(() -> new GameSessionNotFoundException(sessionId));

        Map<GridPosition, GridCell> cells = new HashMap<>();
        for (GridCellState cell : repository.findCellsBySessionId(sessionId)) {
            GridPosition position = GridPosition.fromCell(cell.cell());
            cells.put(position, new GridCell(position, cell.terrainType(), cell.occupiedBy() != null));
        }
        pieceRepository.findBySession(sessionId).forEach(piece -> {
            GridPosition position = GridPosition.fromCell(piece.currentCell());
            GridCell existing = cells.get(position);
            cells.put(
                    position,
                    new GridCell(
                            position,
                            existing == null ? TerrainType.NORMAL
                                    : existing.terrainType(),
                            true
                    )
            );
        });

        Set<GridWall> walls = new HashSet<>();
        for (GridWallState wall : repository.findWallsBySessionId(sessionId)) {
            GridPosition position = GridPosition.fromCell(wall.cell());
            GridPosition adjacent = position.move(wall.direction());
            walls.add(new GridWall(position, wall.direction()));
            walls.add(new GridWall(adjacent, wall.direction().opposite()));
        }

        Map<GridPosition, GridTrap> traps = new HashMap<>();
        for (GridTrapState trap : repository.findTrapsBySessionId(sessionId)) {
            GridPosition position = GridPosition.fromCell(trap.cell());
            traps.put(position, new GridTrap(
                    trap.trapId(),
                    position,
                    trap.visibility(),
                    trap.armed()
            ));
        }

        return new GameGrid(session.gridWidth(), session.gridHeight(), cells, walls, traps);
    }
}
