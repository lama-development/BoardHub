package it.uniupo.boardhub.eventservice.service;

import it.uniupo.boardhub.eventservice.config.JoinProperties;
import it.uniupo.boardhub.eventservice.model.join.GameTable;
import it.uniupo.boardhub.eventservice.model.join.GameTableStatus;
import it.uniupo.boardhub.eventservice.model.join.PublicSessionInfo;
import it.uniupo.boardhub.eventservice.model.join.PublicTableAvailability;
import it.uniupo.boardhub.eventservice.model.join.PublicTableOverview;
import it.uniupo.boardhub.eventservice.repository.GameTableRepository;
import it.uniupo.boardhub.eventservice.service.exception.SessionCapacityException;
import it.uniupo.boardhub.eventservice.service.exception.TableConflictException;
import it.uniupo.boardhub.eventservice.service.exception.TableSessionNotFoundException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.regex.Pattern;

@Service
public class TableSessionService {

    private static final Pattern PUBLIC_ID_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{2,99}");
    private static final Pattern CONFIGURED_TABLE_PATTERN = Pattern.compile("qr-table-(\\d{2})");

    private final GameTableRepository repository;
    private final JoinProperties properties;
    private final Clock clock;

    public TableSessionService(GameTableRepository repository, JoinProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    // Registra il tavolo e lo collega alla sessione creata nella stessa transazione.
    public GameTable registerAndActivate(
            String requestedTableId,
            String requestedPublicId,
            String requestedDisplayName,
            String sessionId
    ) {
        String tableId = requireTableId(requestedTableId);
        String publicId = valueOrDefault(requestedPublicId, tableId).trim();
        String displayName = valueOrDefault(requestedDisplayName, tableId).trim();
        validateIdentifiers(tableId, publicId, displayName);

        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        GameTable table = repository.findById(tableId).map(existing -> {
            if (!existing.tablePublicId().equals(publicId)) {
                throw new TableConflictException("Il tavolo esiste gia con un QR pubblico differente.");
            }
            if (existing.activeSessionId() != null && !existing.activeSessionId().equals(sessionId)) {
                throw new TableConflictException("Il tavolo e gia occupato da un'altra sessione attiva.");
            }
            return new GameTable(
                    existing.tableId(), existing.tablePublicId(), existing.displayName(),
                    existing.status(), sessionId, existing.createdAt(), now
            );
        }).orElseGet(() -> createTable(tableId, publicId, displayName, sessionId, now));

        if (!repository.activateSession(tableId, sessionId, Timestamp.from(now.toInstant()))) {
            throw new TableConflictException("Il tavolo non e disponibile per la nuova sessione.");
        }
        return table;
    }

    // Restituisce solo le informazioni che possono essere mostrate dopo la scansione del QR.
    public PublicSessionInfo resolveActiveSession(String tablePublicId) {
        String normalized = requirePublicId(tablePublicId);
        return repository.findActivePublicSession(normalized)
                .orElseThrow(() -> new TableSessionNotFoundException(normalized));
    }

    // Distingue un tavolo libero da uno occupato e rifiuta QR fuori dall'inventario configurato.
    public PublicTableOverview resolvePublicTable(String tablePublicId) {
        ConfiguredTable configured = requireConfiguredTable(tablePublicId);
        PublicSessionInfo activeSession = repository.findActivePublicSession(configured.publicId())
                .orElse(null);
        String displayName = activeSession != null
                ? activeSession.tableDisplayName()
                : repository.findByPublicId(configured.publicId())
                        .map(GameTable::displayName)
                        .orElse("Tavolo " + configured.number());

        return new PublicTableOverview(
                configured.publicId(),
                configured.number(),
                displayName,
                activeSession == null
                        ? PublicTableAvailability.AVAILABLE
                        : PublicTableAvailability.IN_SESSION,
                activeSession
        );
    }

    private GameTable createTable(
            String tableId,
            String publicId,
            String displayName,
            String sessionId,
            OffsetDateTime now
    ) {
        if (repository.countActiveTables() >= properties.maxActiveTables()) {
            throw new SessionCapacityException("E stato raggiunto il limite di tavoli attivi.");
        }
        GameTable table = new GameTable(
                tableId, publicId, displayName, GameTableStatus.ACTIVE,
                sessionId, now, now
        );
        try {
            repository.save(table);
        } catch (DuplicateKeyException ex) {
            throw new TableConflictException("Il QR pubblico e gia associato a un altro tavolo.");
        }
        return table;
    }

    private void validateIdentifiers(String tableId, String publicId, String displayName) {
        if (tableId.length() > 80) {
            throw new IllegalArgumentException("tableId non puo superare 80 caratteri.");
        }
        requirePublicId(publicId);
        if (displayName.isBlank() || displayName.length() > 100) {
            throw new IllegalArgumentException("tableDisplayName deve contenere da 1 a 100 caratteri.");
        }
    }

    private String requireTableId(String tableId) {
        if (tableId == null || tableId.isBlank()) {
            throw new IllegalArgumentException("tableId e obbligatorio.");
        }
        return tableId.trim();
    }

    private String requirePublicId(String publicId) {
        if (publicId == null || !PUBLIC_ID_PATTERN.matcher(publicId.trim()).matches()) {
            throw new IllegalArgumentException(
                    "tablePublicId deve contenere da 3 a 100 caratteri alfanumerici, punto, trattino o underscore."
            );
        }
        return publicId.trim();
    }

    private ConfiguredTable requireConfiguredTable(String publicId) {
        String normalized = requirePublicId(publicId);
        var matcher = CONFIGURED_TABLE_PATTERN.matcher(normalized);
        int maxTables = properties.maxActiveTables();
        if (!matcher.matches()) {
            throw invalidTable(maxTables);
        }

        int number = Integer.parseInt(matcher.group(1));
        if (number < 1 || number > maxTables) {
            throw invalidTable(maxTables);
        }
        return new ConfiguredTable(number, "qr-table-%02d".formatted(number));
    }

    private IllegalArgumentException invalidTable(int maxTables) {
        return new IllegalArgumentException(
                "Tavolo non valido. I tavoli disponibili sono da 1 a " + maxTables
                        + ". Scansiona il QR presente su uno di questi tavoli."
        );
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private record ConfiguredTable(int number, String publicId) {
    }
}
