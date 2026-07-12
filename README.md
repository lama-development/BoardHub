# BoardHub

**Connected D&D Board Platform**

BoardHub è una piattaforma distribuita per sessioni di **Dungeons & Dragons** giocate su una plancia fisica connessa o simulata.

Il progetto collega un tavolo di gioco a componenti software di rete: una plancia o un simulatore genera eventi, un nodo edge li raccoglie, MQTT li trasporta verso il backend, PostgreSQL conserva lo storico e le API REST rendono i dati disponibili ad applicazioni client.

Il diagramma seguente rappresenta l'architettura completa. Nell'MVP corrente il simulatore pubblica direttamente sul broker; il nodo edge con buffer offline e sincronizzazione e il principale componente ancora da realizzare.

```text
plancia fisica / simulatore
-> nodo edge
-> broker MQTT
-> event-service Java/Spring Boot
-> PostgreSQL
-> API REST
-> app mobile / dashboard
```

I dettagli di dominio, regole operative, dadi, movimento su griglia, ruolo del Dungeon Master e algoritmi previsti sono descritti in [PROJECT_SCOPE.md](PROJECT_SCOPE.md).

## Stato attuale

| Area | Stato |
| :--- | :--- |
| Infrastruttura Docker | Implementata con Mosquitto MQTT e PostgreSQL. |
| Contratti REST/MQTT | Definiti in `docs/CONTRATTI_DI_COMUNICAZIONE.md`. |
| Simulatore Python | Implementato per pubblicare una mini-sessione D&D su MQTT. |
| Subscriber Python | Implementato per leggere gli eventi MQTT in forma comprensibile. |
| `event-service` | Implementato come microservizio Spring Boot che riceve eventi MQTT. |
| Persistenza eventi | Implementata su PostgreSQL tramite repository JDBC. |
| Persistenza sessione/plancia | Base dati iniziale implementata per sessioni, celle, muri e trappole. |
| API REST eventi | Implementata con `GET /api/v1/sessions/{sessionId}/events`. |
| API REST sessioni | Implementata con `POST /api/v1/sessions`. |
| OpenAPI | Specifica iniziale disponibile in `docs/openapi/event-service.openapi.yml`. |
| Comandi rapidi | Disponibili tramite `just help`. |
| Modello griglia | Implementato per posizioni, celle, terreno e richieste di movimento. |
| Algoritmo movimento | Implementato con Dijkstra semplificato. |
| API celle raggiungibili | Implementata con `POST /api/v1/movement/reachable-cells`. |
| Ricostruzione griglia da sessione | Implementata come servizio interno da stato persistito. |
| API movimento da sessione | Implementata con `POST /api/v1/sessions/{sessionId}/movement/reachable-cells`. |
| Dashboard web | Implementata per monitorare stato del backend ed eventi di una sessione. |
| App mobile | Da implementare. |

## Struttura del repository

| Percorso | Contenuto |
| :--- | :--- |
| `docker/` | Configurazione locale di Mosquitto e PostgreSQL. |
| `docs/` | Changelog pubblico, contratti di comunicazione e specifica OpenAPI. |
| `justfile` | Comandi rapidi per avvio, test e demo locale. |
| `services/event-service/` | Microservizio Java/Spring Boot per ricezione, salvataggio e lettura degli eventi. |
| `frontend/` | Dashboard React per monitorare eventi e stato minimo di una sessione. |
| `simulator/` | Script Python per pubblicare e leggere eventi MQTT dimostrativi. |

## Avvio rapido

Per visualizzare i comandi rapidi disponibili:

```bash
just help
```

I comandi principali sono:

```bash
just up
just backend
just test
just create-session session-demo-001
just move-session session-demo-001
just publish-event session-demo-001
just events session-demo-001
just down
```

I comandi manuali equivalenti sono riportati sotto.

Avviare Mosquitto e PostgreSQL:

```bash
docker compose -f docker/docker-compose.yml up -d
```

Controllare lo stato dei container:

```bash
docker compose -f docker/docker-compose.yml ps
```

Avviare il backend:

```bash
cd services/event-service
mvn spring-boot:run
```

Verificare lo stato del servizio:

```bash
curl http://localhost:8082/actuator/health
```

In un altro terminale, pubblicare una mini-sessione D&D:

```bash
cd simulator
source .venv/bin/activate
python publish_event.py
```

Leggere gli eventi salvati:

```bash
curl http://localhost:8082/api/v1/sessions/session-20260630-001/events
```

Avviare la dashboard in un terminale separato:

```bash
cd frontend
npm ci
npm run dev
```

Aprire `http://localhost:5173` e indicare l'identificativo della sessione da monitorare. Durante lo sviluppo Vite inoltra al backend locale le richieste `/api` e `/actuator`.

Creare una sessione con griglia iniziale:

```bash
curl -X POST http://localhost:8082/api/v1/sessions \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"session-20260705-001","venueId":"venue-01","tableId":"table-04","title":"Cripta del Re Caduto","gameType":"DND","grid":{"width":3,"height":3,"difficultCells":["C1"],"blockedCells":["A2"],"obstacleCells":[],"occupiedCells":["A1"],"walls":[{"cell":"B1","direction":"SOUTH"}],"traps":[{"trapId":"trap-01","cell":"B1","visibility":"HIDDEN","armed":true}]}}'
```

Calcolare le celle raggiungibili:

```bash
curl -X POST http://localhost:8082/api/v1/movement/reachable-cells \
  -H "Content-Type: application/json" \
  -d '{"characterId":"adv-01","start":"A1","movementPoints":2,"grid":{"width":3,"height":3,"traps":[{"trapId":"trap-01","cell":"B1","visibility":"HIDDEN","armed":true}]}}'
```

Calcolare le celle raggiungibili usando una sessione salvata:

```bash
curl -X POST http://localhost:8082/api/v1/sessions/session-20260705-001/movement/reachable-cells \
  -H "Content-Type: application/json" \
  -d '{"characterId":"adv-01","start":"A1","movementPoints":2}'
```

Eseguire i test automatici:

```bash
cd services/event-service
mvn test
```

Nella risposta REST, `trapsOnPath` contiene solo le trappole gia rivelate ai giocatori. Le trappole nascoste restano gestite internamente dal backend e non vengono esposte al client.

## Documentazione

- [Project Scope](PROJECT_SCOPE.md)
- [Contratti di comunicazione](docs/CONTRATTI_DI_COMUNICAZIONE.md)
- [Specifica OpenAPI event-service](docs/openapi/event-service.openapi.yml)
- [Changelog](docs/CHANGELOG.md)

## Note operative

Se il volume Docker di PostgreSQL esisteva gia prima di modifiche allo schema, lo script `docker/postgres/init.sql` potrebbe non essere rieseguito automaticamente. Per applicare gli aggiornamenti senza cancellare il volume:

```bash
docker exec -i boardhub_db psql -U boardhub_user -d boardhub_db < docker/postgres/init.sql
```

Il comando puo fallire se nel database esistono gia eventi con lo stesso `sequenceNumber` nella stessa sessione: questi duplicati vanno prima analizzati.
