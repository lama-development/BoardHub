# BoardHub

**Connected D&D Board Platform**

BoardHub è una piattaforma distribuita per sessioni di **Dungeons & Dragons** giocate su una plancia fisica connessa o simulata.

Il progetto collega un tavolo di gioco a componenti software di rete: una plancia o un simulatore genera eventi, un nodo edge li raccoglie, MQTT li trasporta verso il backend, PostgreSQL conserva lo storico e le API REST rendono i dati disponibili ad applicazioni client.

Il diagramma seguente rappresenta l'architettura completa. Il nodo edge con
coda offline e sincronizzazione e implementato in `edge/`: conserva localmente
le osservazioni della plancia e le riconsegna al backend dopo una
disconnessione. Il simulatore storico resta disponibile per la demo minima e
pubblica ancora direttamente sul broker.

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
| Pedine virtuali | Implementate con proprietario, personaggio, cella univoca e vista completa riservata al DM. |
| Eventi REST e live | Implementati con proiezioni separate pubblica, giocatore e DM; aggiornamento live tramite SSE senza segreti negli URL. |
| API REST sessioni | Implementata con `POST /api/v1/sessions`. |
| OpenAPI | Specifiche versionate disponibili per `event-service` e `stats-service` in `docs/openapi/`. |
| Comandi rapidi | Disponibili tramite `just help`. |
| Modello griglia | Implementato per posizioni, celle, terreno e richieste di movimento. |
| Algoritmo movimento | Implementato con Dijkstra semplificato. |
| API celle raggiungibili | Implementata con `POST /api/v1/movement/reachable-cells`. |
| Ricostruzione griglia da sessione | Implementata come servizio interno da stato persistito. |
| API movimento da sessione | Implementata con `POST /api/v1/sessions/{sessionId}/movement/reachable-cells`. |
| Movimento autorevole pedina | Implementato con posizione e velocita persistite, controllo versione, idempotenza ed evento `MOVE_CONFIRMED`. |
| Trappole autorevoli | Implementate con interruzione del percorso, tiro salvezza server-side, danni, HP, prosecuzione e ciclo di vita one-shot o persistente. |
| Controllo temporaneo DM | Implementato per assumere, muovere e restituire un personaggio quando il giocatore non puo operare. |
| Comandi plancia | Implementata la pubblicazione MQTT backend-edge di percorso, correzione e cancellazione effetti senza dettagli riservati della trappola. |
| Nodo edge offline | Implementato in `edge/` con coda SQLite persistente, backoff, deduplica end-to-end e riallineamento dopo la disconnessione. |
| Secondo microservizio | Implementato in `services/stats-service/` su porta 8083, con schema proprio e comunicazione solo via MQTT. |
| Statistiche e torneo | Implementati con risultato di sessione, statistiche per giocatore, torneo e classifica finale. |
| Acknowledgement applicativi | Implementati: il backend conferma la persistenza di ogni evento MQTT, cosi l'edge chiude un elemento solo a salvataggio avvenuto. |
| Validazione prestazionale | Implementato un comando riproducibile per misurare latenza online, recupero offline e acknowledgement applicativi, con campioni CSV e report Markdown. |
| Dashboard web | Implementata con pagina QR, flussi DM/giocatore persistenti, personaggi, pedine, raggiungibilita, movimento e viste operative della sessione. La risoluzione delle trappole e gli stream SSE non sono ancora collegati all'interfaccia. |
| App mobile | Esclusa dall'MVP d'esame; resta una possibile estensione futura. |

## Struttura del repository

| Percorso | Contenuto |
| :--- | :--- |
| `docker/` | Configurazione locale di Mosquitto e PostgreSQL. |
| `docs/` | Changelog pubblico, contratti di comunicazione e specifiche OpenAPI. |
| `justfile` | Comandi rapidi per avvio, test e demo locale. |
| `services/event-service/` | Microservizio Java/Spring Boot per ricezione, salvataggio e lettura degli eventi. |
| `services/stats-service/` | Secondo microservizio: risultati di sessione, statistiche e tornei. |
| `edge/` | Nodo edge Python con coda offline persistente. |
| `frontend/` | Client React per QR del tavolo, console DM e giocatore, personaggi, pedine e movimento. |
| `simulator/` | Script Python per pubblicare e leggere eventi MQTT dimostrativi. |

## Handoff frontend

Questa sezione riassume il contratto operativo che il frontend deve seguire.
Le specifiche complete dei payload restano in
[`docs/openapi/event-service.openapi.yml`](docs/openapi/event-service.openapi.yml)
e [`docs/openapi/stats-service.openapi.yml`](docs/openapi/stats-service.openapi.yml).

### Avvio dell'ambiente

Dalla radice del repository servono tre terminali:

```bash
# Terminale 1: infrastruttura
just up

# Terminale 2: backend, da lasciare in esecuzione
just backend

# Terminale 3: frontend, da lasciare in esecuzione
just frontend
```

In un quarto terminale si possono controllare i tavoli e generare il QR:

```bash
just venue-tables
just enable-table 1 10
just qr 1
```

`just qr 1` mostra l'URL stabile `/t/qr-table-01` e non abilita il tavolo.
`just enable-table 1 10` apre invece per dieci minuti la finestra nella quale
il primo dispositivo puo creare la sessione e diventare DM. I dieci minuti non
sono la durata della partita: una sessione avviata resta attiva finche il DM o
il personale del locale non la conclude.

### Percorso aperto dal QR

La pagina pubblica e sempre `/t/qr-table-XX`. Al caricamento deve chiamare:

```http
GET /api/v1/public/tables/{tablePublicId}
```

Il comportamento dipende dallo stato restituito:

| Stato | Comportamento dell'interfaccia |
| :--- | :--- |
| `DISABLED` | Mostrare che il tavolo non e stato abilitato dal locale. Non permettere di creare una sessione. |
| `CLAIMABLE` | Permettere al primo dispositivo di creare la sessione. La risposta contiene il token DM della nuova sessione. |
| `IN_SESSION` | Mostrare titolo e riepilogo pubblico della partita e permettere a un giocatore di inviare la richiesta di ingresso. |

La creazione della sessione usa `POST /api/v1/sessions`. Il backend consuma
atomicamente l'abilitazione: due dispositivi non possono diventare DM dello
stesso tavolo. Il browser vincitore deve conservare il `dmAccessToken` associato
alla sessione e ripristinare la console DM dopo un refresh.

### Percorso del giocatore

1. Il giocatore apre il QR di un tavolo `IN_SESSION`.
2. Inserisce il nome e invia `POST /api/v1/public/sessions/{sessionId}/join-requests`.
3. Il browser conserva almeno `requestId`, `sessionId` e riferimento del
   dispositivo, in modo da ripristinare lo stato `PENDING` dopo un refresh.
4. Lo stato della richiesta si legge con
   `GET /api/v1/public/sessions/{sessionId}/join-requests/{requestId}`.
5. Dopo l'accettazione, il browser conserva il token `bhp1...` restituito dal
   backend e verifica l'identita con
   `GET /api/v1/player/sessions/{sessionId}/me`.
6. Il giocatore puo creare e consultare i propri personaggi tramite
   `/api/v1/player/sessions/{sessionId}/characters`.
7. Puo associare un proprio personaggio a una pedina virtuale e consultarla
   tramite `/api/v1/player/sessions/{sessionId}/pieces`.

Le chiamate del giocatore dal punto 5 in avanti richiedono:

```http
Authorization: Bearer bhp1...
```

### Percorso del Dungeon Master

La console DM usa il token `bhd1...` come Bearer e deve permettere di:

- elencare, accettare o rifiutare le richieste con
  `/api/v1/dm/sessions/{sessionId}/join-requests`;
- vedere i partecipanti con
  `/api/v1/dm/sessions/{sessionId}/participants`;
- vedere tutti i personaggi con
  `/api/v1/dm/sessions/{sessionId}/characters`;
- vedere tutte le pedine persistite e la loro cella con
  `/api/v1/dm/sessions/{sessionId}/pieces`;
- concludere la sessione con
  `POST /api/v1/dm/sessions/{sessionId}/close`.

Se il dispositivo DM non e piu disponibile, il personale puo chiudere la
sessione dalla macchina del locale:

```bash
just venue-close-table 1
```

### Limite del frontend attuale

Il monitor generico `http://localhost:5173` legge soltanto
`GET /api/v1/sessions/{sessionId}/events` e ricostruisce graficamente le pedine
dagli eventi `MOVE`. Un `MOVE` MQTT grezzo descrive un'osservazione della
plancia e alimenta lo storico, ma non modifica la posizione autorevole di una
pedina. Non e quindi la fonte autorevole delle nuove pedine persistite: una
pedina creata con `POST .../pieces` puo esistere correttamente nel database
senza comparire in quel monitor.

Le console dedicate usano invece le API autorevoli: il giocatore puo creare e
leggere personaggi e pedine, calcolare le destinazioni e richiedere un
movimento; il DM consulta partecipanti, personaggi e pedine della sessione.
Gli eventi restano lo storico delle azioni, non lo stato corrente della
plancia. Restano da integrare nel frontend gli stati `TRAP_PENDING`, il tiro e
la prosecuzione, il takeover del DM dopo refresh e gli stream SSE.

### Verifica equivalente da terminale

Questo flusso permette di provare il backend anche prima del completamento
dell'interfaccia:

```bash
just enable-table 2 10
just create-session session-piece-demo-001 table-02 qr-table-02 "Tavolo 2"
export BOARDHUB_DM_TOKEN='bhd1...'

just request-join session-piece-demo-001 player-device-01 Andrea
just pending-joins session-piece-demo-001
just accept-join session-piece-demo-001 REQUEST_ID
export BOARDHUB_PLAYER_TOKEN='bhp1...'

just create-character session-piece-demo-001
just create-piece session-piece-demo-001 CHARACTER_ID B2
just my-characters session-piece-demo-001
just my-pieces session-piece-demo-001
just dm-characters session-piece-demo-001
just dm-pieces session-piece-demo-001
```

`REQUEST_ID`, `CHARACTER_ID`, `bhd1...` e `bhp1...` devono essere sostituiti
con i valori mostrati dalle risposte. I token sono credenziali temporanee:
non devono essere inseriti nel repository, negli screenshot pubblici o nei log
del frontend.

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
just create-piece session-demo-001 CHARACTER_ID B2
just my-pieces session-demo-001
just dm-pieces session-demo-001
just piece-reachable session-demo-001 PIECE_ID
just move-piece session-demo-001 PIECE_ID C2 0
just trap-status session-demo-001 RESOLUTION_ID
just roll-trap session-demo-001 RESOLUTION_ID VERSIONE_RISOLUZIONE
just continue-trap session-demo-001 RESOLUTION_ID NUOVA_VERSIONE
```

I primi tre comandi creano e ispezionano le schede dei personaggi. I tre
successivi associano un personaggio alla sua pedina virtuale, la posizionano
in `B2` e mostrano rispettivamente la proiezione del proprietario e quella
completa del DM. `CHARACTER_ID` e l'identificativo mostrato da
`just create-character`; `PIECE_ID` e quello restituito da
`just create-piece`. Gli ultimi due comandi calcolano le destinazioni usando
posizione e velocita salvate e confermano uno spostamento reale. Il quarto
argomento di `move-piece` e la versione corrente della pedina: dopo ogni
movimento usare la nuova versione mostrata nella risposta.

`just move-piece` genera automaticamente un `commandId`. Per riprovare in modo
idempotente lo stesso comando, passare esplicitamente come quinto argomento lo
stesso UUID: il backend restituira il risultato gia salvato senza applicare
un secondo spostamento.

Se il percorso incontra una trappola attiva, `move-piece` restituisce
`TRAP_PENDING` e un `RESOLUTION_ID`: la pedina si ferma sulla cella di
attivazione. `trap-status` legge lo stato, `roll-trap` esegue sul server il d20
con bonus e danni configurati, mentre `continue-trap` usa il movimento residuo
solo quando l'esito lo consente. Ogni comando richiede la versione mostrata
dalla risposta precedente e puo essere ritentato con lo stesso `commandId`
senza duplicare tiro, danni o spostamento.

Il DM consulta tutte le risoluzioni con `just pending-traps ID`. In caso di
telefono scarico o client indisponibile puo usare `just assume-character ID
CHARACTER_ID`, operare con `just dm-move-piece`, quindi restituire il controllo
con `just release-character`. Il controllo e persistito e compare nel campo
`controlMode` delle pedine anche dopo un refresh.

Lo storico pubblico (`just events`) e intenzionalmente ridotto. Le viste
autenticate `just player-events` e `just dm-events` applicano proiezioni
diverse: il giocatore vede solo i propri dati, il DM vede lo stato completo.

### Demo completa della trappola

La ricetta `create-trap-session` crea una griglia `4 x 3` con partenza prevista
in `B2`, destinazione `D2`, ostacoli in `C1` e `C3` e una trappola persistente
nascosta in `C2`. Il percorso e quindi univoco e permette di verificare
l'interruzione senza dipendere dalla scelta di uno fra piu cammini equivalenti.

Con infrastruttura e backend gia attivi, usare un tavolo libero:

```bash
just enable-table 8 15
just create-trap-session session-trap-demo-001
export BOARDHUB_DM_TOKEN='TOKEN_DM_RESTITUITO'

just request-join session-trap-demo-001 player-device-01 Andrea
just pending-joins session-trap-demo-001
just accept-join session-trap-demo-001 REQUEST_ID
export BOARDHUB_PLAYER_TOKEN='TOKEN_GIOCATORE_RESTITUITO'

just create-character session-trap-demo-001
just create-piece session-trap-demo-001 CHARACTER_ID B2
just move-piece session-trap-demo-001 PIECE_ID D2 0
```

L'ultimo comando deve rispondere `TRAP_PENDING`: la pedina e salvata in `C2`
e la risposta fornisce `RESOLUTION_ID`, movimento residuo e nuova versione.
Proseguire sempre usando la versione mostrata dalla risposta precedente:

```bash
just trap-status session-trap-demo-001 RESOLUTION_ID
just roll-trap session-trap-demo-001 RESOLUTION_ID VERSIONE_RISOLUZIONE
just continue-trap session-trap-demo-001 RESOLUTION_ID NUOVA_VERSIONE
just player-events session-trap-demo-001
just dm-events session-trap-demo-001
```

Se il tiro impone `STOP`, `continue-trap` viene correttamente rifiutato. Se
consente `CONTINUE`, la pedina termina in `D2`. Per simulare un telefono non
disponibile, il DM puo assumere il controllo e usare le API equivalenti:

```bash
just assume-character session-trap-demo-001 CHARACTER_ID
just dm-piece-reachable session-trap-demo-001 PIECE_ID
just dm-roll-trap session-trap-demo-001 RESOLUTION_ID VERSIONE_RISOLUZIONE
just dm-continue-trap session-trap-demo-001 RESOLUTION_ID NUOVA_VERSIONE
just release-character session-trap-demo-001 CHARACTER_ID
```

Il tiro e casuale e va eseguito una sola volta per quella risoluzione. I retry
devono riutilizzare lo stesso `commandId`; in tal caso il backend restituisce
lo stesso risultato senza ritirare i dadi o applicare nuovamente il danno.

Il personaggio persistito e una scheda tattica minima, non la riproduzione
completa della scheda D&D. Livello, HP, CA e velocita seguono la semantica
delle [Free Rules 2024](https://www.dndbeyond.com/sources/dnd/br-2024/creating-a-character);
le soglie elevate su HP, CA e caselle sono protezioni tecniche del servizio.
Il sistema non calcola automaticamente questi valori da caratteristiche,
classe, equipaggiamento o capacita: salva anche i sei bonus ai tiri salvezza e
li usa come valori effettivi durante la risoluzione delle trappole.

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

Nella risposta REST, `trapsOnPath` contiene solo le trappole gia rivelate ai giocatori. Le trappole nascoste restano gestite internamente dal backend e non vengono esposte al client; se vengono attraversate, il backend interrompe comunque il movimento in modo autorevole.

## Validazione prestazionale

Con infrastruttura e `event-service` attivi, e con un tavolo disabilitato,
la baseline locale si esegue dalla radice del repository:

```bash
just edge-setup
just measure 10 7
```

Il comando raccoglie dieci campioni utili piu un warm-up per due scenari:
pubblicazione online fino alla persistenza PostgreSQL e recupero della coda
SQLite dopo una disconnessione del broker. Produce `measurements.csv` e
`report.md` sotto `artifacts/measurements/`; questi risultati locali sono
ignorati da Git. Il tavolo scelto deve essere nello stato `DISABLED` e viene
ripristinato automaticamente al termine della prova.

## Documentazione

- [Project Scope](PROJECT_SCOPE.md)
- [Contratti di comunicazione](docs/CONTRATTI_DI_COMUNICAZIONE.md)
- [Specifica OpenAPI event-service](docs/openapi/event-service.openapi.yml)
- [Specifica OpenAPI stats-service](docs/openapi/stats-service.openapi.yml)
- [Changelog](docs/CHANGELOG.md)

## Note operative

Lo schema PostgreSQL e gestito da Flyway. All'avvio dell'event-service vengono applicate, in ordine, le migrazioni presenti in `services/event-service/src/main/resources/db/migration`.

Il primo avvio con un volume PostgreSQL gia esistente registra una baseline
alla versione `0` e applica in ordine le migrazioni versionate. `V1` crea lo
schema iniziale; `V2` aggiunge tavoli, richieste di ingresso e partecipanti;
`V3` aggiunge i personaggi posseduti dai partecipanti; `V4` introduce il
controllo del locale sullo stato dei tavoli e sulle finestre temporanee di
avvio; `V5` collega ogni pedina virtuale al proprietario, al personaggio e a
una cella univoca della sessione; `V6` aggiunge il contatore degli eventi
generati dal backend e separa la progressione degli eventi per sorgente; `V7`
aggiunge definizioni complete delle trappole, risoluzioni persistite, tiri,
danni, stato tattico e controllo temporaneo del DM; `V8` aggiunge l'outbox
transazionale che conserva i risultati di sessione finche il broker MQTT non ne
conferma la pubblicazione. I dati esistenti non vengono cancellati e non e piu
necessario eseguire manualmente `init.sql`. Lo `stats-service` gestisce invece
il proprio schema `stats_schema` con migrazioni indipendenti.

Per controllare le migrazioni applicate mentre PostgreSQL e attivo:

```bash
docker exec boardhub_db psql -U boardhub_user -d boardhub_db \
  -c "SELECT installed_rank, version, description, success FROM game_schema.flyway_schema_history ORDER BY installed_rank;"
```

Le nuove modifiche strutturali devono essere aggiunte con una nuova migrazione versionata. Una migrazione gia applicata non deve essere riscritta, perche Flyway ne verifica l'integrita tramite checksum.
