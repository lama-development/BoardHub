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
| Tavoli e QR | Implementati con QR stabile, abilitazione temporanea del locale e sessione attiva associata. |
| Ingresso giocatori | Implementato con richiesta idempotente, approvazione DM e partecipante persistito. |
| Accesso giocatore | Implementato per l'MVP locale con claim privato, token HMAC e endpoint Bearer `/me`. |
| Accesso DM | Implementato con token HMAC limitato alla sessione e ripristino dopo il refresh. |
| Controllo locale | Implementato con API private locali per elencare, abilitare, disabilitare e chiudere i tavoli. |
| Personaggi | Implementati con proprietario autenticato, limiti, validazione e vista completa riservata al DM. |
| API REST eventi | Implementata con `GET /api/v1/sessions/{sessionId}/events`. |
| API REST sessioni | Implementata con `POST /api/v1/sessions`. |
| OpenAPI | Specifica iniziale disponibile in `docs/openapi/event-service.openapi.yml`. |
| Comandi rapidi | Disponibili tramite `just help`. |
| Modello griglia | Implementato per posizioni, celle, terreno e richieste di movimento. |
| Algoritmo movimento | Implementato con Dijkstra semplificato. |
| API celle raggiungibili | Implementata con `POST /api/v1/movement/reachable-cells`. |
| Ricostruzione griglia da sessione | Implementata come servizio interno da stato persistito. |
| API movimento da sessione | Implementata con `POST /api/v1/sessions/{sessionId}/movement/reachable-cells`. |
| Dashboard web | Base implementata per monitor, pagina QR, avvio DM, richieste e partecipanti; i personaggi non sono ancora mostrati. |
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
just enable-table 4
just create-session session-demo-001
export BOARDHUB_DM_TOKEN='bhd1...'
just move-session session-demo-001
just publish-event session-demo-001
just events session-demo-001
just close-session session-demo-001
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

Avviare il sito in un terminale separato:

```bash
just frontend
```

Aprire `http://localhost:5173` per il monitor delle sessioni. Il percorso
`/t/qr-table-XX`, aperto dal QR fisico, mostra invece lo stato del tavolo: se il
locale lo ha abilitato permette al primo dispositivo di avviare una sessione
come DM; se e occupato permette al giocatore di chiedere l'ingresso; se e
disabilitato resta in attesa del personale. Durante lo sviluppo Vite inoltra al
backend locale le richieste `/api` e `/actuator`.

Abilitare per dieci minuti il Tavolo 4 dalla macchina del locale:

```bash
just enable-table 4 10
```

Creare quindi una sessione con griglia iniziale:

```bash
curl -X POST http://localhost:8082/api/v1/sessions \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"session-20260705-001","venueId":"venue-01","tableId":"table-04","tablePublicId":"qr-table-04","tableDisplayName":"Tavolo 4","title":"Cripta del Re Caduto","gameType":"DND","publicSummary":"Avventura per personaggi di livello 3.","acceptingJoinRequests":true,"grid":{"width":3,"height":3,"difficultCells":["C1"],"blockedCells":["A2"],"obstacleCells":[],"occupiedCells":["A1"],"walls":[{"cell":"B1","direction":"SOUTH"}],"traps":[{"trapId":"trap-01","cell":"B1","visibility":"HIDDEN","armed":true}]}}'
```

La risposta contiene `dmAccessToken` nel formato `bhd1...`. Il sito lo conserva
nel browser che ha creato la sessione; da terminale va esportato in
`BOARDHUB_DM_TOKEN` per usare i comandi riservati al DM. Non e una password
globale del locale e viene revocato alla chiusura della partita.

Il flusso di ingresso parte dal QR stabile del tavolo. Con frontend e backend
attivi, `just qr 4` mostra nel terminale il QR del Tavolo 4 da scansionare con
un telefono sulla stessa rete. `just table-status qr-table-04` controlla lo
stato REST; i comandi `just request-join`, `just pending-joins`,
`just accept-join`, `just participants` e `just close-session` permettono di
provare il resto del flusso senza riscrivere le chiamate REST. `just help`
mostra sintassi e significato.

I comandi REST mostrano per impostazione predefinita un riepilogo pensato per
l'uso operativo: tabelle, stati tradotti, conteggi e identificativi utili. Per
ispezionare il contratto tecnico completo si puo anteporre
`BOARDHUB_OUTPUT=json`, per esempio:

```bash
BOARDHUB_OUTPUT=json just venue-tables
BOARDHUB_OUTPUT=json just events session-demo-001
```

Dopo `just accept-join`, il campo `accessToken` della risposta identifica il
giocatore accettato. Per provare i personaggi senza lasciare il token nella
cronologia dei comandi:

```bash
export BOARDHUB_PLAYER_TOKEN='bhp1...'
just create-character session-demo-001
just my-characters session-demo-001
just dm-characters session-demo-001
```

Il primo comando crea la scheda demo `Elaria`; il secondo mostra soltanto i
personaggi del giocatore autenticato; il terzo usa il token della sessione DM e
mostra tutti i personaggi della sessione.

Il personaggio persistito e una scheda tattica minima, non la riproduzione
completa della scheda D&D. Livello, HP, CA e velocita seguono la semantica
delle [Free Rules 2024](https://www.dndbeyond.com/sources/dnd/br-2024/creating-a-character);
le soglie elevate su HP, CA e caselle sono protezioni tecniche del servizio.
Il sistema non calcola ancora automaticamente questi valori da
caratteristiche, classe, equipaggiamento o capacita.

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

Lo schema PostgreSQL e gestito da Flyway. All'avvio dell'event-service vengono applicate, in ordine, le migrazioni presenti in `services/event-service/src/main/resources/db/migration`.

Il primo avvio con un volume PostgreSQL gia esistente registra una baseline
alla versione `0` e applica in ordine le migrazioni versionate. `V1` crea lo
schema iniziale; `V2` aggiunge tavoli, richieste di ingresso e partecipanti;
`V3` aggiunge i personaggi posseduti dai partecipanti; `V4` introduce il
controllo del locale sullo stato dei tavoli e sulle finestre temporanee di
avvio. I dati esistenti non vengono cancellati e non e piu necessario eseguire
manualmente `init.sql`.

Per controllare le migrazioni applicate mentre PostgreSQL e attivo:

```bash
docker exec boardhub_db psql -U boardhub_user -d boardhub_db \
  -c "SELECT installed_rank, version, description, success FROM game_schema.flyway_schema_history ORDER BY installed_rank;"
```

Le nuove modifiche strutturali devono essere aggiunte con una nuova migrazione versionata. Una migrazione gia applicata non deve essere riscritta, perche Flyway ne verifica l'integrita tramite checksum.
