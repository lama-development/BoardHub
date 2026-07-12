package it.uniupo.boardhub.eventservice.service;

import it.uniupo.boardhub.eventservice.controller.dto.CreateGameSessionRequest;
import it.uniupo.boardhub.eventservice.controller.dto.MovementGridRequest;
import it.uniupo.boardhub.eventservice.controller.dto.MovementTrapRequest;
import it.uniupo.boardhub.eventservice.controller.dto.MovementWallRequest;
import it.uniupo.boardhub.eventservice.model.grid.GridDirection;
import it.uniupo.boardhub.eventservice.model.grid.TerrainType;
import it.uniupo.boardhub.eventservice.model.grid.TrapVisibility;
import it.uniupo.boardhub.eventservice.model.session.GameSession;
import it.uniupo.boardhub.eventservice.model.session.GameSessionStatus;
import it.uniupo.boardhub.eventservice.model.session.GridCellState;
import it.uniupo.boardhub.eventservice.model.session.GridTrapState;
import it.uniupo.boardhub.eventservice.model.session.GridWallState;
import it.uniupo.boardhub.eventservice.repository.GameSessionRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class GameSessionCreationService {

    private static final String DEFAULT_GAME_TYPE = "DND";
    private static final String OCCUPIED_MARKER = "OCCUPIED";

    private final GameSessionRepository repository;
    private final MovementGridFactory gridFactory;

    public GameSessionCreationService(GameSessionRepository repository, MovementGridFactory gridFactory) {
        this.repository = repository;
        this.gridFactory = gridFactory;
    }

    // Crea sessione e configurazione iniziale della griglia in un'unica operazione.
    @Transactional
    public GameSession createSession(CreateGameSessionRequest request) {
        validate(request);

        MovementGridRequest grid = request.grid();
        GameSession session = new GameSession(
                sessionIdOrGenerated(request.sessionId()),
                request.venueId(),
                request.tableId(),
                request.title(),
                valueOrDefault(request.gameType(), DEFAULT_GAME_TYPE).toUpperCase(Locale.ROOT),
                GameSessionStatus.ACTIVE,
                grid.width(),
                grid.height(),
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        saveSessionOrFail(session);
        saveCells(session.sessionId(), grid);
        saveWalls(session.sessionId(), grid.walls());
        saveTraps(session.sessionId(), grid.traps());
        return session;
    }

    // Trasforma il vincolo di chiave duplicata del database in errore applicativo REST.
    private void saveSessionOrFail(GameSession session) {
        try {
            repository.saveSession(session);
        } catch (DuplicateKeyException ex) {
            throw new DuplicateGameSessionException(session.sessionId());
        }
    }

    // Verifica i campi minimi necessari per aprire una sessione giocabile.
    private void validate(CreateGameSessionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La richiesta di creazione sessione e obbligatoria.");
        }
        if (isBlank(request.venueId())) {
            throw new IllegalArgumentException("venueId e obbligatorio.");
        }
        if (isBlank(request.tableId())) {
            throw new IllegalArgumentException("tableId e obbligatorio.");
        }
        if (isBlank(request.title())) {
            throw new IllegalArgumentException("title e obbligatorio.");
        }
        if (request.grid() == null) {
            throw new IllegalArgumentException("La griglia iniziale e obbligatoria.");
        }
        if (request.grid().width() < 1 || request.grid().height() < 1) {
            throw new IllegalArgumentException("Le dimensioni della griglia devono essere maggiori di zero.");
        }
        gridFactory.create(request.grid());
    }

    // Salva solo le celle non standard: terreno speciale o occupazione.
    private void saveCells(String sessionId, MovementGridRequest grid) {
        Map<String, GridCellState> cells = new LinkedHashMap<>();
        putTerrainCells(cells, sessionId, grid.difficultCells(), TerrainType.DIFFICULT);
        putTerrainCells(cells, sessionId, grid.blockedCells(), TerrainType.BLOCKED);
        putTerrainCells(cells, sessionId, grid.obstacleCells(), TerrainType.OBSTACLE);
        putOccupiedCells(cells, sessionId, grid.occupiedCells());
        cells.values().forEach(repository::saveCell);
    }

    private void putTerrainCells(
            Map<String, GridCellState> cells,
            String sessionId,
            List<String> positions,
            TerrainType terrainType
    ) {
        // Usa una mappa per fondere terreno speciale e occupazione sulla stessa cella.
        for (String cell : safeList(positions)) {
            GridCellState existing = cells.get(cell);
            cells.put(cell, new GridCellState(
                    sessionId,
                    cell,
                    terrainType,
                    existing == null ? null : existing.occupiedBy()
            ));
        }
    }

    // Registra le celle gia occupate da personaggi o entita della sessione.
    private void putOccupiedCells(Map<String, GridCellState> cells, String sessionId, List<String> positions) {
        for (String cell : safeList(positions)) {
            GridCellState existing = cells.get(cell);
            cells.put(cell, new GridCellState(
                    sessionId,
                    cell,
                    existing == null ? TerrainType.NORMAL : existing.terrainType(),
                    OCCUPIED_MARKER
            ));
        }
    }

    // Salva i muri come bordi direzionati; la reciprocita viene gestita nel modello griglia.
    private void saveWalls(String sessionId, List<MovementWallRequest> walls) {
        for (MovementWallRequest wall : safeList(walls)) {
            repository.saveWall(new GridWallState(
                    sessionId,
                    wall.cell(),
                    GridDirection.valueOf(wall.direction().toUpperCase(Locale.ROOT))
            ));
        }
    }

    // Salva trappole e visibilita decise dal Dungeon Master.
    private void saveTraps(String sessionId, List<MovementTrapRequest> traps) {
        for (MovementTrapRequest trap : safeList(traps)) {
            repository.saveTrap(new GridTrapState(
                    sessionId,
                    trap.trapId(),
                    trap.cell(),
                    TrapVisibility.valueOf(trap.visibility().toUpperCase(Locale.ROOT)),
                    trap.armed()
            ));
        }
    }

    private String sessionIdOrGenerated(String sessionId) {
        return isBlank(sessionId) ? "session-" + UUID.randomUUID() : sessionId;
    }

    private String valueOrDefault(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
