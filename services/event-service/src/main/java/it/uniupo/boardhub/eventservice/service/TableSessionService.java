package it.uniupo.boardhub.eventservice.service;

import it.uniupo.boardhub.eventservice.config.JoinProperties;
import it.uniupo.boardhub.eventservice.config.VenueProperties;
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
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TableSessionService {

    private static final Pattern PUBLIC_ID_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{2,99}");
    private static final Pattern CONFIGURED_TABLE_PATTERN = Pattern.compile("qr-table-(\\d{2})");

    private final GameTableRepository repository;
    private final JoinProperties properties;
    private final VenueProperties venueProperties;
    private final Clock clock;

    public TableSessionService(
            GameTableRepository repository,
            JoinProperties properties,
            VenueProperties venueProperties,
            Clock clock
    ) {
        this.repository = repository;
        this.properties = properties;
        this.venueProperties = venueProperties;
        this.clock = clock;
    }

    // Consuma il claim temporaneo e collega il tavolo alla sessione nella stessa transazione.
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
        GameTable table = repository.findByPublicId(publicId)
                .orElseThrow(() -> new TableConflictException(
                        "Il locale non ha abilitato questo tavolo."
                ));
        if (!table.tableId().equals(tableId)) {
            throw new TableConflictException("Il QR pubblico appartiene a un altro tavolo.");
        }
        if (table.status() != GameTableStatus.CLAIMABLE
                || table.claimExpiresAt() == null
                || !table.claimExpiresAt().isAfter(now)) {
            repository.expireClaim(publicId, Timestamp.from(now.toInstant()));
            throw new TableConflictException(
                    "Il tavolo non e abilitato o la finestra di avvio e scaduta."
            );
        }
        if (repository.countActiveTables() >= properties.maxActiveTables()) {
            throw new SessionCapacityException("E stato raggiunto il limite di tavoli attivi.");
        }
        if (!repository.claimSession(tableId, sessionId, Timestamp.from(now.toInstant()))) {
            throw new TableConflictException(
                    "Il tavolo e stato reclamato da un'altra sessione o l'abilitazione e scaduta."
            );
        }
        return new GameTable(
                table.tableId(),
                table.tablePublicId(),
                table.displayName(),
                GameTableStatus.IN_SESSION,
                sessionId,
                null,
                table.createdAt(),
                now
        );
    }

    // Abilita per un tempo limitato un solo tavolo configurato del locale.
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public GameTable enableTable(String tablePublicId, Integer requestedMinutes) {
        ConfiguredTable configured = requireConfiguredTable(tablePublicId);
        Duration ttl = claimDuration(requestedMinutes);
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        OffsetDateTime expiresAt = now.plus(ttl);
        GameTable existing = repository.findByPublicId(configured.publicId()).orElse(null);
        if (existing != null && existing.status() == GameTableStatus.IN_SESSION) {
            throw new TableConflictException("Il tavolo ospita gia una sessione attiva.");
        }

        if (existing == null) {
            GameTable created = new GameTable(
                    configured.tableId(),
                    configured.publicId(),
                    configured.displayName(),
                    GameTableStatus.CLAIMABLE,
                    null,
                    expiresAt,
                    now,
                    now
            );
            try {
                repository.save(created);
                return created;
            } catch (DuplicateKeyException ex) {
                throw new TableConflictException("Il tavolo e stato modificato da un'altra operazione.");
            }
        }

        if (!repository.enableClaim(
                configured.publicId(),
                Timestamp.from(expiresAt.toInstant()),
                Timestamp.from(now.toInstant())
        )) {
            throw new TableConflictException("Il tavolo non puo essere abilitato nello stato corrente.");
        }
        return new GameTable(
                existing.tableId(),
                existing.tablePublicId(),
                existing.displayName(),
                GameTableStatus.CLAIMABLE,
                null,
                expiresAt,
                existing.createdAt(),
                now
        );
    }

    // Revoca una finestra di avvio; una sessione attiva deve essere conclusa esplicitamente.
    @Transactional
    public GameTable disableTable(String tablePublicId) {
        ConfiguredTable configured = requireConfiguredTable(tablePublicId);
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        GameTable existing = repository.findByPublicId(configured.publicId()).orElse(null);
        if (existing == null) {
            return disabledTable(configured, now);
        }
        if (existing.status() == GameTableStatus.IN_SESSION) {
            throw new TableConflictException(
                    "Il tavolo ospita una sessione attiva: concludila prima di disabilitarlo."
            );
        }
        repository.disableClaim(configured.publicId(), Timestamp.from(now.toInstant()));
        return new GameTable(
                existing.tableId(),
                existing.tablePublicId(),
                existing.displayName(),
                GameTableStatus.DISABLED,
                null,
                null,
                existing.createdAt(),
                now
        );
    }

    // Elenca sempre l'intero inventario configurato, inclusi i tavoli mai usati.
    public List<PublicTableOverview> listConfiguredTables() {
        return java.util.stream.IntStream.rangeClosed(1, properties.maxActiveTables())
                .mapToObj(number -> resolvePublicTable("qr-table-%02d".formatted(number)))
                .toList();
    }

    // Restituisce solo le informazioni che possono essere mostrate dopo la scansione del QR.
    public PublicSessionInfo resolveActiveSession(String tablePublicId) {
        String normalized = requirePublicId(tablePublicId);
        return repository.findActivePublicSession(normalized)
                .orElseThrow(() -> new TableSessionNotFoundException(normalized));
    }

    // Distingue un tavolo disabilitato, reclamabile o occupato e rifiuta QR fuori inventario.
    public PublicTableOverview resolvePublicTable(String tablePublicId) {
        ConfiguredTable configured = requireConfiguredTable(tablePublicId);
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        GameTable table = repository.findByPublicId(configured.publicId()).orElse(null);
        if (table != null
                && table.status() == GameTableStatus.CLAIMABLE
                && (table.claimExpiresAt() == null || !table.claimExpiresAt().isAfter(now))) {
            repository.expireClaim(configured.publicId(), Timestamp.from(now.toInstant()));
            table = new GameTable(
                    table.tableId(), table.tablePublicId(), table.displayName(),
                    GameTableStatus.DISABLED, null, null, table.createdAt(), now
            );
        }
        PublicSessionInfo activeSession = repository.findActivePublicSession(configured.publicId())
                .orElse(null);
        String displayName = table == null ? configured.displayName() : table.displayName();
        PublicTableAvailability availability = activeSession != null
                ? PublicTableAvailability.IN_SESSION
                : table != null && table.status() == GameTableStatus.CLAIMABLE
                        ? PublicTableAvailability.CLAIMABLE
                        : PublicTableAvailability.DISABLED;

        return new PublicTableOverview(
                configured.publicId(),
                configured.number(),
                displayName,
                availability,
                availability == PublicTableAvailability.CLAIMABLE ? table.claimExpiresAt() : null,
                activeSession
        );
    }

    private Duration claimDuration(Integer requestedMinutes) {
        if (requestedMinutes == null) {
            return venueProperties.tableClaimTtl();
        }
        if (requestedMinutes < 1 || requestedMinutes > venueProperties.maxClaimMinutes()) {
            throw new IllegalArgumentException(
                    "La durata deve essere compresa tra 1 e "
                            + venueProperties.maxClaimMinutes() + " minuti."
            );
        }
        return Duration.ofMinutes(requestedMinutes);
    }

    private GameTable disabledTable(ConfiguredTable configured, OffsetDateTime now) {
        return new GameTable(
                configured.tableId(),
                configured.publicId(),
                configured.displayName(),
                GameTableStatus.DISABLED,
                null,
                null,
                now,
                now
        );
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
        return new ConfiguredTable(
                number,
                "table-%02d".formatted(number),
                "qr-table-%02d".formatted(number),
                "Tavolo " + number
        );
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

    private record ConfiguredTable(
            int number,
            String tableId,
            String publicId,
            String displayName
    ) {
    }
}
