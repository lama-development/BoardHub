# BoardHub

**Connected Games Platform for Smart Venues**

BoardHub e una piattaforma sperimentale per collegare tavoli D&D/fantasy fisici a un sistema digitale tramite eventi MQTT, backend REST, database e interfaccia web.

Lo stato attuale del progetto copre la prima pipeline applicativa:

```text
simulatore Python -> broker MQTT Mosquitto -> event-service Java/Spring Boot -> PostgreSQL
```

## Struttura

| Cartella | Scopo |
| :--- | :--- |
| `docker/` | Infrastruttura locale con PostgreSQL e Mosquitto. |
| `docs/` | Documentazione condivisa: contratti e changelog. |
| `services/event-service/` | Primo microservizio Spring Boot che ascolta e salva gli eventi MQTT di gioco. |
| `simulator/` | Script Python per pubblicare e leggere eventi MQTT demo. |

## Avvio rapido

Avviare i servizi locali:

```bash
docker compose -f docker/docker-compose.yml up -d
```

Controllare lo stato:

```bash
docker compose -f docker/docker-compose.yml ps
```

Avviare il subscriber leggibile:

```bash
cd simulator
source .venv/bin/activate
python subscribe_events.py
```

In un secondo terminale pubblicare la mini-sessione D&D:

```bash
cd simulator
source .venv/bin/activate
python publish_event.py
```

Avviare il backend che riceve gli eventi MQTT:

```bash
cd services/event-service
mvn spring-boot:run
```

Verificare che il service sia attivo:

```bash
curl http://localhost:8082/actuator/health
```

Eseguire i test automatici del service:

```bash
cd services/event-service
mvn test
```

Nota: se il volume Docker di PostgreSQL esisteva gia prima dell'aggiunta della tabella `game_schema.game_events`, lo script `docker/postgres/init.sql` potrebbe non essere rieseguito automaticamente. In quel caso occorre ricreare il volume o applicare manualmente lo schema aggiornato.

## Documentazione

- [Contratti di comunicazione](docs/CONTRATTI_DI_COMUNICAZIONE.md)
- [Changelog](docs/CHANGELOG.md)

## Stato

- Docker locale configurato.
- Contratti REST/MQTT definiti.
- Simulatore MQTT funzionante.
- Subscriber MQTT leggibile funzionante.
- Primo backend Java/Spring Boot implementato come subscriber MQTT.
- Repository JDBC per salvare eventi su PostgreSQL implementato e testato con H2.
- Web app ancora da implementare.
