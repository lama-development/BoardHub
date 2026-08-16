# Contratti di comunicazione - BoardHub

## 1. Scopo del documento

Questo documento definisce i contratti di comunicazione utilizzati dai componenti principali di **BoardHub**.

L'obiettivo e stabilire in modo esplicito:

- le API REST esposte dal backend verso le applicazioni client;
- i topic MQTT usati per la comunicazione asincrona tra simulatore, componente edge, broker e backend;
- il formato standard dei messaggi JSON;
- le regole minime per identificare, ordinare e sincronizzare gli eventi di gioco.

Il documento mantiene un approccio **contract-first**: le interfacce condivise vengono definite e aggiornate insieme all'implementazione dei servizi applicativi, in modo da ridurre ambiguita tra backend, simulatore e futuri client.

Questa versione descrive il contratto iniziale dell'MVP e si concentra sul nucleo rilevante per PISSIR: generazione, trasmissione, ricezione, ordinamento e persistenza degli eventi di gioco prodotti da una plancia D&D fisica o simulata.

## 2. Componenti coinvolti

| Componente | Responsabilita | Protocollo principale |
| :--- | :--- | :--- |
| **Backend BoardHub** | Espone API REST, valida i dati, persiste eventi e stato delle sessioni. | HTTP/REST, MQTT |
| **Broker MQTT Mosquitto** | Smista gli eventi tra simulatore e backend. | MQTT |
| **Simulatore software** | Genera una mini-sessione D&D dimostrativa. | MQTT |
| **PostgreSQL** | Memorizza eventi, sessioni, partecipanti, personaggi e configurazione della griglia. | SQL interno |
| **Dashboard web** | Consulta stato del backend ed eventi di una sessione. | REST |
| **App mobile, plancia fisica ed edge** | Componenti previsti per gli sviluppi successivi. | REST, MQTT |

Il database non viene esposto direttamente ai client. Tutto l'accesso ai dati deve passare dal backend.

### 2.1 Regole operative del tavolo

Il tavolo BoardHub rappresenta una sessione D&D fisica o simulata. Per l'MVP il dominio viene limitato a poche regole operative, sufficienti per generare eventi chiari e verificabili:

| Area | Regola | Effetto sui contratti |
| :--- | :--- | :--- |
| Accesso giocatore | Il QR del tavolo apre la sessione pubblica; il DM accetta o rifiuta la richiesta. | API REST pubbliche e API protette per il DM. |
| Preparazione mappa | La configurazione iniziale della griglia e accettata dall'API di creazione sessione. | Celle, terreno, muri e trappole nel payload REST. |
| Movimento | Il backend calcola le celle raggiungibili su griglia stateless o salvata. | Risposta REST con costo, percorso e trappole visibili. |
| Dadi | Il tiro salvezza richiesto da una trappola e generato dal backend; il lancio libero dell'app resta futuro. | API idempotenti di risoluzione e futuri eventi `DICE_ROLLED` generici. |
| Turni e round | Il simulatore include `ROUND_END`; la gestione completa dei turni e futura. | Eventi MQTT generici, senza motore completo del regolamento D&D. |

I contratti non automatizzano tutto il regolamento D&D. Descrivono solo le informazioni necessarie per dimostrare plancia connessa, comunicazione MQTT, persistenza e consultazione tramite API.

### 2.2 Implementazione attuale

| Componente | Stato | Note |
| :--- | :--- | :--- |
| Broker MQTT | Implementato in ambiente Docker | Disponibile tramite Mosquitto per i test locali. |
| Simulatore software | Implementato | Pubblica una mini-sessione D&D ordinata sul topic `events`. |
| Subscriber backend | Implementato | `event-service` riceve e interpreta gli eventi MQTT. |
| Persistenza eventi | Implementata | `event-service` salva gli eventi in `game_schema.game_events`. |
| Persistenza sessione/plancia | Base implementata | Sono presenti tabelle e repository per sessione, celle configurate, muri e trappole. |
| Tavoli e richieste di ingresso | Implementati nel backend | QR stabile, abilitazione del locale, claim temporaneo, coda richieste, approvazione DM e partecipanti sono persistiti. |
| Personaggi | Implementati nel backend | Il giocatore crea e legge i propri personaggi; il DM legge tutti quelli della sessione. |
| Pedine virtuali | Implementate nel backend | Ogni pedina collega proprietario, personaggio e cella univoca; il DM vede l'intera sessione. |
| Ricostruzione griglia | Implementata come servizio interno | Lo stato persistito, comprese le pedine, viene convertito in `GameGrid` per il calcolo del movimento. |
| Eventi REST e SSE | Implementati | Proiezioni pubblica, giocatore e DM; aggiornamenti live filtrati per ruolo. |
| API REST sessioni | Implementata | `POST /api/v1/sessions` salva sessione e griglia e occupa il tavolo indicato. |
| API REST ingresso | Implementata | Risoluzione QR, richiesta idempotente, decisione DM, partecipanti e chiusura sessione. |
| API REST movimento | Implementata | Le due API `reachable-cells` calcolano il movimento su griglia stateless o persistita. |
| Risoluzione trappole | Implementata | Interruzione del percorso, tiro server-side, danni, HP, prosecuzione e ciclo di vita persistito. |
| Controllo temporaneo DM | Implementato | Il DM puo assumere e restituire il controllo di un personaggio con audit degli eventi. |
| Dashboard web | Base implementata | Consuma health check e storico eventi; l'integrazione con la griglia variabile deve essere completata. |

Per mantenere prevedibile il carico del servizio, una richiesta accetta al massimo **2.500 celle complessive** e **100 punti movimento**. Questi limiti sono protezioni tecniche dell'MVP, non regole del regolamento D&D.

## 3. Convenzioni di naming

Per evitare ambiguita tra documentazione, codice, payload JSON e database, i contratti tecnici usano nomi in inglese.

| Concetto | Nome tecnico | Esempio |
| :--- | :--- | :--- |
| Nodo/installazione dimostrativa | `venueId` | `venue-01` |
| Tavolo | `tableId` | `table-04` |
| QR pubblico del tavolo | `tablePublicId` | `qr-table-04` |
| Sessione di gioco | `sessionId` | `session-20260630-001` |
| Installazione locale giocatore | `playerReference` | `player-device-01` |
| Evento | `eventId` | `evt-000042` |
| Giocatore | `playerId` | `player-01` |
| Avventuriero / personaggio | `characterId` | `adv-01` |
| Pedina fisica | `tokenId` | `token-adv-01` |
| Mostro | `monsterId` | `mon-03` |
| Nodo edge | `edgeId` | `edge-venue-01-table-04` |
| Cella della griglia | `cell` | `A3` |

Nota: in questa fase `venueId` e usato solo come identificativo tecnico per separare una sorgente di eventi da un'altra.

Regole generali:

- gli identificativi sono stringhe stabili e leggibili;
- i campi JSON usano `camelCase`;
- gli endpoint REST usano nomi plurali;
- le date usano formato ISO 8601;
- gli eventi MQTT includono sempre `eventId`, `eventType`, `sessionId`, `occurredAt` e `sequenceNumber`;
- i testi dell'interfaccia possono essere in italiano, ma i contratti tecnici restano in inglese.

## 4. Contratto REST

Le API REST attualmente implementate sono usate per creare e chiudere sessioni,
gestire l'ingresso dei giocatori, leggere eventi persistiti, calcolare le
destinazioni e confermare il movimento autorevole delle pedine.

Base path:

```text
/api/v1
```

La specifica OpenAPI dell'`event-service` e disponibile in:

```text
docs/openapi/event-service.openapi.yml
```

### 4.1 Stato del servizio

| Metodo | Endpoint | Scopo |
| :--- | :--- | :--- |
| `GET` | `/actuator/health` | Verifica lo stato tecnico del backend Spring Boot. |

### 4.2 Sessioni di gioco e movimento

| Metodo | Endpoint | Scopo |
| :--- | :--- | :--- |
| `POST` | `/api/v1/sessions` | Consuma il claim del tavolo, crea la sessione e restituisce il token DM. |
| `GET` | `/api/v1/sessions/{sessionId}/events` | Restituisce la proiezione pubblica e sanitizzata dello storico. |
| `GET` | `/api/v1/player/sessions/{sessionId}/events` | Restituisce gli eventi del giocatore autenticato senza segreti del DM. |
| `GET` | `/api/v1/dm/sessions/{sessionId}/events` | Restituisce al DM la proiezione completa dello storico. |
| `GET` | `/api/v1/player/sessions/{sessionId}/events/stream` | Apre il flusso SSE filtrato per il giocatore autenticato. |
| `GET` | `/api/v1/dm/sessions/{sessionId}/events/stream` | Apre il flusso SSE completo per il DM autenticato. |
| `POST` | `/api/v1/movement/reachable-cells` | Calcola le celle raggiungibili su una griglia fornita nella richiesta. |
| `POST` | `/api/v1/sessions/{sessionId}/movement/reachable-cells` | Calcola le celle raggiungibili usando la griglia salvata della sessione. |
| `GET` | `/api/v1/public/tables/{tablePublicId}` | Restituisce `DISABLED`, `CLAIMABLE` o `IN_SESSION`. |
| `GET` | `/api/v1/public/tables/{tablePublicId}/active-session` | Restituisce la sessione pubblica associata al QR. |
| `POST` | `/api/v1/public/sessions/{sessionId}/join-requests` | Inserisce una richiesta idempotente nella coda del DM. |
| `GET` | `/api/v1/public/sessions/{sessionId}/join-requests/{requestId}` | Restituisce al dispositivo originario lo stato della propria richiesta. |
| `GET` | `/api/v1/player/sessions/{sessionId}/me` | Verifica il token Bearer e restituisce il partecipante giocatore attivo. |
| `POST` | `/api/v1/player/sessions/{sessionId}/characters` | Crea un personaggio posseduto dal giocatore autenticato. |
| `GET` | `/api/v1/player/sessions/{sessionId}/characters` | Elenca soltanto i personaggi posseduti dal giocatore autenticato. |
| `POST` | `/api/v1/player/sessions/{sessionId}/pieces` | Associa un personaggio posseduto a una pedina virtuale e a una cella iniziale. |
| `GET` | `/api/v1/player/sessions/{sessionId}/pieces` | Elenca soltanto le pedine possedute dal giocatore autenticato. |
| `GET` | `/api/v1/player/sessions/{sessionId}/pieces/{sessionPieceId}/reachable-cells` | Calcola le destinazioni usando posizione e velocita persistite della pedina. |
| `POST` | `/api/v1/player/sessions/{sessionId}/pieces/{sessionPieceId}/moves` | Conferma atomicamente lo spostamento e registra `MOVE_CONFIRMED`. |
| `GET` | `/api/v1/player/sessions/{sessionId}/trap-resolutions/{resolutionId}` | Legge lo stato sicuro di una trappola attivata. |
| `POST` | `/api/v1/player/sessions/{sessionId}/trap-resolutions/{resolutionId}/roll` | Esegue una sola volta il tiro salvezza autorevole. |
| `POST` | `/api/v1/player/sessions/{sessionId}/trap-resolutions/{resolutionId}/continue` | Prosegue il movimento residuo quando consentito. |
| `GET` | `/api/v1/dm/sessions/{sessionId}/join-requests` | Elenca le richieste filtrate per stato. |
| `POST` | `/api/v1/dm/sessions/{sessionId}/join-requests/{requestId}/accept` | Accetta la richiesta e crea il partecipante. |
| `POST` | `/api/v1/dm/sessions/{sessionId}/join-requests/{requestId}/reject` | Rifiuta la richiesta. |
| `GET` | `/api/v1/dm/sessions/{sessionId}/participants` | Elenca i partecipanti attivi. |
| `GET` | `/api/v1/dm/sessions/{sessionId}/characters` | Elenca tutti i personaggi della sessione, comprese le schede riservate. |
| `GET` | `/api/v1/dm/sessions/{sessionId}/pieces` | Elenca tutte le pedine virtuali e le loro celle per il DM. |
| `POST/DELETE` | `/api/v1/dm/sessions/{sessionId}/characters/{characterId}/control` | Assume o restituisce il controllo temporaneo del personaggio. |
| `GET` | `/api/v1/dm/sessions/{sessionId}/pieces/{sessionPieceId}/reachable-cells` | Calcola il movimento per una pedina controllata dal DM. |
| `POST` | `/api/v1/dm/sessions/{sessionId}/pieces/{sessionPieceId}/moves` | Conferma il movimento per una pedina controllata dal DM. |
| `GET` | `/api/v1/dm/sessions/{sessionId}/trap-resolutions` | Elenca le risoluzioni ancora pendenti. |
| `GET` | `/api/v1/dm/sessions/{sessionId}/trap-resolutions/{resolutionId}` | Consulta la trappola per un personaggio controllato dal DM. |
| `POST` | `/api/v1/dm/sessions/{sessionId}/trap-resolutions/{resolutionId}/roll` | Esegue il tiro salvezza per un personaggio controllato dal DM. |
| `POST` | `/api/v1/dm/sessions/{sessionId}/trap-resolutions/{resolutionId}/continue` | Prosegue il movimento del personaggio controllato dal DM quando consentito. |
| `POST` | `/api/v1/dm/sessions/{sessionId}/close` | Conclude la sessione e disabilita il tavolo, che dovrà essere riabilitato dal locale. |
| `GET` | `/api/v1/admin/tables` | Elenca tutti i tavoli dalla console privata del locale. |
| `POST` | `/api/v1/admin/tables/{tablePublicId}/enable` | Apre una finestra temporanea di avvio. |
| `POST` | `/api/v1/admin/tables/{tablePublicId}/disable` | Revoca una finestra non ancora consumata. |
| `POST` | `/api/v1/admin/tables/{tablePublicId}/close-session` | Conclude dal locale una sessione rimasta attiva. |

L'endpoint stateless `reachable-cells` e implementato come operazione `POST` perche riceve una griglia completa: dimensioni, terreno, celle occupate, muri e trappole.

L'endpoint storico legato direttamente a `sessionId` ricostruisce la griglia
persistita, ma riceve ancora personaggio, posizione e punti movimento dal
chiamante ed e mantenuto per demo e compatibilita. Il flusso operativo usa
invece la pedina autenticata: il client invia soltanto destinazione, versione
attesa e identificativo idempotente del comando; posizione, velocita,
proprietario e occupazione vengono ricavati dal backend.

Per proteggere le informazioni riservate al Dungeon Master, il campo `trapsOnPath` delle risposte REST contiene solo trappole con visibilita `REVEALED`. Le trappole `HIDDEN` e `ALWAYS_HIDDEN` possono essere rilevate internamente dal backend, ma non vengono comunicate al client giocatore.

Le richieste con griglie oltre 2.500 celle, configurazioni sproporzionate o `movementPoints` fuori dall'intervallo `0..100` vengono rifiutate con `400 Bad Request`.

### 4.3 Accesso tramite QR e approvazione del DM

Il QR contiene l'URL pubblico stabile della pagina del tavolo, per esempio
`http://host:5173/t/qr-table-04`. Non contiene credenziali, dati del giocatore o
identificativi di una singola partita. La pagina estrae il riferimento del tavolo
e ne recupera lo stato:

```http
GET /api/v1/public/tables/qr-table-04
```

Un tavolo configurato parte da `DISABLED`: il QR continua ad aprire la pagina,
ma nessun visitatore puo creare una partita. Il personale abilita il tavolo per
un intervallo limitato e lo stato diventa `CLAIMABLE`, con
`claimExpiresAt`. Il primo dispositivo che crea la sessione consuma
atomicamente il claim e diventa DM; lo stato passa a `IN_SESSION`. Un secondo
tentativo concorrente riceve `409 Conflict`. Alla scadenza, un claim non usato
torna automaticamente `DISABLED`.

Le API del locale richiedono `X-BoardHub-Venue-Key`. Per impostazione
predefinita accettano soltanto richieste provenienti dalla macchina loopback del
backend: la credenziale non viene inserita nel QR, nel frontend pubblico o nei
dispositivi dei giocatori.

I riferimenti standard vanno da `qr-table-01` a `qr-table-08`. Un numero fuori
intervallo restituisce `400 Bad Request` con l'intervallo ammesso. L'endpoint
storico seguente rimane disponibile per i client che vogliono esclusivamente una
sessione attiva:

```http
GET /api/v1/public/tables/qr-table-04/active-session
```

Il giocatore invia quindi una richiesta con un UUID nuovo nell'header `Idempotency-Key`:

```http
POST /api/v1/public/sessions/session-20260705-001/join-requests
Idempotency-Key: 5f31aa70-a48f-46ca-9db3-b7e53169afaf
Content-Type: application/json
```

```json
{
  "playerReference": "player-device-01",
  "displayName": "Andrea"
}
```

Un retry con la stessa chiave e gli stessi dati restituisce la richiesta gia creata. Riutilizzare la chiave con dati differenti produce `409 Conflict`. Le richieste pendenti scadono dopo il tempo configurato e il numero di tentativi per giocatore viene limitato.

Il client conserva localmente la chiave casuale e il `requestId`. In questo
prototipo locale la chiave svolge anche il ruolo di segreto di possesso della
richiesta e non deve essere inserita nel QR, nell'URL o nei log. Dopo un refresh
il dispositivo recupera lo stato con:

```http
GET /api/v1/public/sessions/session-20260705-001/join-requests/{requestId}
X-BoardHub-Join-Claim: 5f31aa70-a48f-46ca-9db3-b7e53169afaf
```

Una chiave diversa restituisce `404` senza rivelare se il `requestId` esiste.
Finche lo stato e `PENDING` il browser ripete periodicamente questa lettura. In
caso di `REJECTED` o `EXPIRED` permette una nuova richiesta; in caso di
`ACCEPTED` riceve il partecipante e la credenziale firmata della sessione.

La creazione della sessione restituisce il token DM versionato
`bhd1.<participantId>.<firma>`. Il browser che ha consumato il claim lo conserva
localmente e lo ripristina dopo un refresh. Le operazioni del DM richiedono
`Authorization: Bearer bhd1...`; la firma lega il DM a una sola sessione e il
backend verifica anche ruolo, partecipante e stato corrente.

Dopo l'accettazione il backend crea un solo partecipante giocatore. La risposta al DM e la
successiva lettura autenticata del giocatore restituiscono la stessa credenziale
firmata `accessToken`; i retry non duplicano il partecipante. Il limite
predefinito e di otto giocatori attivi per sessione.

La credenziale del giocatore usa il formato versionato
`bhp1.<participantId>.<firma>`. La firma HMAC lega il partecipante alla singola
sessione e viene confrontata in tempo costante. Il client la invia come Bearer:

```http
GET /api/v1/player/sessions/session-20260705-001/me
Authorization: Bearer bhp1.550e8400-e29b-41d4-a716-446655440000.firma
```

Il backend restituisce `401 PLAYER_UNAUTHORIZED` se il token e assente,
malformato, alterato o usato per una sessione diversa. Anche un token
crittograficamente valido viene rifiutato quando il partecipante non e piu
`ACTIVE` o la sessione non e piu `ACTIVE`. Nel prototipo locale la validita
temporale coincide quindi con la durata della partecipazione alla sessione:
la chiusura della partita revoca immediatamente l'accesso senza memorizzare la
credenziale nel database.

La chiusura della sessione imposta lo stato `ENDED`, fa scadere le richieste
ancora pendenti, chiude le partecipazioni attive, revoca token DM e giocatori e
porta il tavolo a `DISABLED`. Lo stesso QR resta valido, ma una partita
successiva richiede una nuova abilitazione esplicita del locale.

### 4.4 Personaggi del giocatore

Un partecipante accettato crea un personaggio presentando il token Bearer
ricevuto dopo l'approvazione:

```http
POST /api/v1/player/sessions/session-20260705-001/characters
Authorization: Bearer bhp1.550e8400-e29b-41d4-a716-446655440000.firma
Content-Type: application/json
```

```json
{
  "name": "Elaria",
  "species": "Elfa",
  "age": 120,
  "className": "Maga",
  "level": 3,
  "speedCells": 6,
  "hpMax": 18,
  "armorClass": 12,
  "partyVisibility": "OWNER_ONLY"
}
```

`age`, `hpCurrent` e `partyVisibility` sono facoltativi. Se `hpCurrent` e
assente assume `hpMax`; la visibilita predefinita e `OWNER_ONLY`. I valori
ammessi per la visibilita sono `OWNER_ONLY`, `PARTY` e `DM_ONLY`. In questo
incremento il valore viene persistito per le proiezioni future, ma non amplia
la risposta del giocatore: `GET
/api/v1/player/sessions/{sessionId}/characters` restituisce sempre e soltanto
i personaggi posseduti dal token corrente.

Questa e una scheda tattica minima compatibile con le Free Rules 2024, non una
scheda D&D completa. `level` segue l'intervallo ufficiale 1-20. `speedCells`
registra la velocita effettiva sulla griglia: nelle regole ufficiali ogni
casella rappresenta 5 piedi e la velocita in caselle si ottiene dividendo la
Speed per 5. `hpCurrent` non puo superare `hpMax`.

Eta, specie, classe, HP e Classe Armatura non vengono dedotti
automaticamente: l'eta dipende dalla specie, la Speed puo cambiare per specie
ed effetti, gli HP dipendono almeno da classe, livello e Costituzione, mentre
la CA dipende da Destrezza, equipaggiamento e capacita. Questi dati non sono
ancora tutti presenti nell'MVP, quindi il client invia i valori effettivi
approvati dal DM. I massimi di 100 caselle, 1.000.000 HP e CA 100 sono
protezioni tecniche BoardHub, non limiti del regolamento. Specie e classe
restano testo libero per non escludere materiale ufficiale aggiuntivo o
opzioni della campagna.

Riferimenti ufficiali:
[Creating a Character](https://www.dndbeyond.com/sources/dnd/br-2024/creating-a-character),
[Character Origins](https://www.dndbeyond.com/sources/dnd/br-2024/character-origins)
e [Playing the Game](https://www.dndbeyond.com/sources/dnd/br-2024/playing-the-game).

Il DM autorizzato usa `GET
/api/v1/dm/sessions/{sessionId}/characters` e vede tutte le schede della
sessione, incluse quelle marcate `OWNER_ONLY` o `DM_ONLY`. Il limite
predefinito e otto personaggi per partecipante ed e configurabile tramite
`BOARDHUB_MAX_CHARACTERS_PER_PARTICIPANT`; il superamento restituisce `409
CHARACTER_CAPACITY_REACHED`.

La proprieta non viene accettata dal body: `participantId` e `sessionId`
derivano dalla credenziale e dal path. Questa regola impedisce a un giocatore
di creare o leggere personaggi per conto di un altro partecipante. La chiusura
della sessione revoca anche queste API.

### 4.5 Pedine virtuali della sessione

Dopo aver creato un personaggio, il proprietario puo registrare la sua pedina
virtuale e scegliere una cella iniziale:

```http
POST /api/v1/player/sessions/session-20260705-001/pieces
Authorization: Bearer bhp1.550e8400-e29b-41d4-a716-446655440000.firma
Content-Type: application/json
```

```json
{
  "characterId": "8f65ff63-0c9d-42bd-bdc6-c9400edce91b",
  "representationMode": "VIRTUAL",
  "startCell": "B2"
}
```

Il backend ricava `participantId` dal token, verifica che il personaggio
appartenga a quel partecipante e normalizza la coordinata. Un personaggio puo
avere una sola pedina e una cella puo contenere una sola pedina nella stessa
sessione. Celle fuori griglia, bloccate, ostacolo o gia occupate vengono
rifiutate. Questi vincoli sono applicati sia dal servizio sia dal database.

`GET /api/v1/player/sessions/{sessionId}/pieces` espone soltanto le pedine del
proprietario autenticato. `GET /api/v1/dm/sessions/{sessionId}/pieces` richiede
il token DM e restituisce tutte le pedine della sessione. Non esiste ancora una
proiezione pubblica della plancia: verra introdotta insieme alla credenziale
del nodo edge, evitando di esporre dati tattici tramite il QR pubblico.

La modalita supportata in questo incremento e soltanto `VIRTUAL`. NFC, sensori
e associazione con una pedina fisica richiedono un identificativo hardware e
un contratto di attendibilita dedicato; non vengono simulati dichiarandoli gia
disponibili. Le celle occupate dalle pedine persistite sono gia considerate
non attraversabili dalla ricostruzione `GameGrid`.

### 4.6 Esempio creazione sessione con griglia iniziale

Richiesta:

```http
POST /api/v1/sessions
Content-Type: application/json
```

```json
{
  "sessionId": "session-20260705-001",
  "venueId": "venue-01",
  "tableId": "table-04",
  "tablePublicId": "qr-table-04",
  "tableDisplayName": "Tavolo 4",
  "title": "Cripta del Re Caduto",
  "gameType": "DND",
  "publicSummary": "Avventura per personaggi di livello 3.",
  "acceptingJoinRequests": true,
  "grid": {
    "width": 3,
    "height": 3,
    "difficultCells": ["C1"],
    "blockedCells": ["A2"],
    "obstacleCells": [],
    "occupiedCells": ["A1"],
    "walls": [
      { "cell": "B1", "direction": "SOUTH" }
    ],
    "traps": [
      {
        "trapId": "trap-01",
        "cell": "B1",
        "visibility": "HIDDEN",
        "armed": true,
        "lifecyclePolicy": "PERSISTENT",
        "saveAbility": "DEXTERITY",
        "saveDc": 12,
        "rollMode": "NORMAL",
        "damageExpression": "1d6",
        "successDamage": "NONE",
        "successMovement": "CONTINUE",
        "failureDamage": "FULL",
        "failureMovement": "STOP"
      }
    ]
  }
}
```

Risposta:

```json
{
  "sessionId": "session-20260705-001",
  "venueId": "venue-01",
  "tableId": "table-04",
  "tablePublicId": "qr-table-04",
  "tableDisplayName": "Tavolo 4",
  "title": "Cripta del Re Caduto",
  "gameType": "DND",
  "publicSummary": "Avventura per personaggi di livello 3.",
  "acceptingJoinRequests": true,
  "status": "ACTIVE",
  "gridWidth": 3,
  "gridHeight": 3,
  "createdAt": "2026-07-05T16:00:00Z",
  "dmAccessToken": "bhd1.550e8400-e29b-41d4-a716-446655440000.firma"
}
```

`dmAccessToken` viene mostrato una sola volta nella risposta di creazione. Il
client DM deve conservarlo in modo locale e inviarlo come Bearer nelle
operazioni della sessione.

### 4.7 Esempio calcolo celle raggiungibili

Richiesta:

```http
POST /api/v1/movement/reachable-cells
Content-Type: application/json
```

```json
{
  "characterId": "adv-01",
  "start": "A1",
  "movementPoints": 2,
  "grid": {
    "width": 3,
    "height": 3,
    "difficultCells": ["C1"],
    "blockedCells": ["A2"],
    "obstacleCells": [],
    "occupiedCells": [],
    "walls": [
      { "cell": "B1", "direction": "SOUTH" }
    ],
    "traps": [
      {
        "trapId": "trap-01",
        "cell": "B1",
        "visibility": "HIDDEN",
        "armed": true
      }
    ]
  }
}
```

Risposta:

```json
{
  "characterId": "adv-01",
  "reachableCells": [
    {
      "cell": "A1",
      "cost": 0,
      "path": ["A1"],
      "trapsOnPath": []
    },
    {
      "cell": "B1",
      "cost": 1,
      "path": ["A1", "B1"],
      "trapsOnPath": []
    }
  ]
}
```

### 4.8 Movimento autorevole della pedina

Il client giocatore legge prima le destinazioni consentite:

```http
GET /api/v1/player/sessions/session-demo-001/pieces/7d1da3ea-e98c-41e7-8747-702c9c89f35d/reachable-cells
Authorization: Bearer bhp1...
```

```json
{
  "sessionPieceId": "7d1da3ea-e98c-41e7-8747-702c9c89f35d",
  "currentCell": "A1",
  "movementPoints": 6,
  "version": 0,
  "reachableCells": [
    {
      "cell": "B2",
      "cost": 2,
      "path": ["A1", "B1", "B2"],
      "trapsOnPath": []
    }
  ]
}
```

Per confermare la destinazione usa la versione appena letta e genera un UUID
stabile per l'operazione:

```http
POST /api/v1/player/sessions/session-demo-001/pieces/7d1da3ea-e98c-41e7-8747-702c9c89f35d/moves
Authorization: Bearer bhp1...
Content-Type: application/json
```

```json
{
  "destination": "B2",
  "expectedVersion": 0,
  "commandId": "550e8400-e29b-41d4-a716-446655440000"
}
```

```json
{
  "status": "CONFIRMED",
  "commandId": "550e8400-e29b-41d4-a716-446655440000",
  "eventId": "move-550e8400-e29b-41d4-a716-446655440000",
  "sessionPieceId": "7d1da3ea-e98c-41e7-8747-702c9c89f35d",
  "characterId": "2a0c0ce6-a753-41f0-86a3-6a40bbb80fc1",
  "from": "A1",
  "to": "B2",
  "path": ["A1", "B1", "B2"],
  "cost": 2,
  "version": 1,
  "visibleTrapsOnPath": []
}
```

La posizione e l'evento vengono scritti nella stessa transazione. In caso di
errore nessuno dei due cambia. Ripetere la richiesta con lo stesso `commandId`
restituisce il risultato gia registrato; lo stesso UUID non puo descrivere un
movimento diverso. `expectedVersion` impedisce che due comandi concorrenti
aggiornino entrambi una pedina letta alla stessa versione.

Una destinazione non raggiungibile produce `422 MOVE_REJECTED` senza aggiungere
eventi allo storico. Una versione superata produce
`409 STALE_PIECE_STATE`; il client deve rileggere la pedina. Le trappole
`HIDDEN` e `ALWAYS_HIDDEN` non compaiono in `trapsOnPath` o
`visibleTrapsOnPath`.

### 4.9 Risoluzione autorevole delle trappole

Una trappola attraversabile non viene trattata come un muro. Il backend
calcola il percorso e, quando incontra la prima trappola che deve attivarsi,
sposta atomicamente la pedina sulla sua cella e restituisce `202` con stato
`TRAP_PENDING`, `resolutionId`, destinazione originaria e movimento residuo.
La configurazione segreta (CD, formula dei danni e note del DM) non compare
nella risposta del giocatore.

Il tiro usa `expectedVersion` e `commandId`: una ripetizione identica restituisce
lo stesso risultato, mentre una versione superata viene rifiutata. Il backend
genera il d20, applica vantaggio o svantaggio, somma il bonus della
caratteristica, tira gli eventuali danni, aggiorna HP e stato `DOWNED`, quindi
decide `CONTINUE` o `STOP`. Se il movimento puo proseguire, il comando
`continue` riparte dalla cella di attivazione con il budget residuo e puo
interrompersi di nuovo su un'altra trappola.

Le trappole `ONE_SHOT` diventano `SPENT`; quelle `PERSISTENT` rimangono
`TRIGGERED_ACTIVE` finche il DM non le disarma. Una trappola persistente gia
nota viene evitata dal percorso automatico quando esiste un'alternativa, ma
rimane calpestabile se il giocatore sceglie esplicitamente la sua cella o viene
mosso forzatamente. Alla chiusura della sessione ogni risoluzione pendente
diventa `CANCELLED`.

Il DM puo assumere temporaneamente il controllo del personaggio. Finche il
controllo e attivo i comandi del proprietario vengono rifiutati; il DM usa gli
endpoint equivalenti e poi restituisce il controllo. Assunzione e rilascio sono
idempotenti, persistiti e registrati nello storico.

### 4.10 Esempio lettura eventi sessione

Richiesta:

```http
GET /api/v1/sessions/session-20260630-001/events
```

Risposta:

```json
[
  {
    "eventId": "evt-000042",
    "eventType": "MOVE",
    "sessionId": "session-20260630-001",
    "occurredAt": "2026-06-30T17:45:00Z",
    "sequenceNumber": 2,
    "payload": {
      "characterId": "adv-01",
      "from": "A3",
      "to": "A4"
    }
  }
]
```

La risposta precedente e la proiezione pubblica. Le API autenticate di
giocatore e DM restituiscono proiezioni progressivamente piu complete. Gli
stessi filtri sono applicati ai flussi SSE, che usano l'header Bearer e non
inseriscono credenziali nell'URL. Dopo una disconnessione il client ricarica lo
stato corrente con REST e riapre lo stream.

## 5. Contratto MQTT

MQTT viene usato per la comunicazione asincrona e real-time tra simulatore, componente edge e backend.

Topic MQTT attualmente utilizzato:

| Topic | Direzione | Scopo |
| :--- | :--- | :--- |
| `boardhub/v1/venues/{venueId}/tables/{tableId}/events` | Simulatore/edge -> Backend | Pubblicazione degli eventi osservati dalla plancia. |
| `boardhub/v1/venues/{venueId}/tables/{tableId}/commands` | Backend -> Edge/plancia | Comandi visuali derivati da stato gia confermato nel database. |

Esempio topic reale:

```text
boardhub/v1/venues/venue-01/tables/table-04/events
```

Il topic dell'`event-service` e configurabile tramite `BOARDHUB_MQTT_TOPIC`.
Il valore predefinito usa la wildcard controllata
`boardhub/v1/venues/venue-01/tables/+/events`, quindi una sola istanza gestisce
tutti i tavoli del locale. Prima del salvataggio il backend verifica che
`venueId` e `tableId` del payload corrispondano esattamente al topic ricevuto.
Gli eventi validi ricevuti dal simulatore vengono inoltre inoltrati ai client
SSE dopo il salvataggio. I comandi diretti alla plancia sono pubblicati solo
dopo il commit del backend: una mancata consegna MQTT non annulla lo stato
autorevole, che l'edge puo riallineare tramite una futura snapshot.

### 5.1 Tipi di evento attualmente prodotti dalla demo

| Evento | Descrizione |
| :--- | :--- |
| `SESSION_START` | Avvio di una nuova sessione. |
| `MOVE` | Movimento di un personaggio o mostro sulla griglia. |
| `MOVE_CONFIRMED` | Movimento di una pedina validato e persistito dal backend. |
| `TRAP_TRIGGERED` | Il percorso e stato interrotto sulla cella della trappola. |
| `TRAP_ROLL_RESOLVED` | Tiro salvezza, danni e decisione di movimento sono stati persistiti. |
| `TRAP_RESOLUTION_COMPLETED` | La risoluzione non richiede altre azioni. |
| `CHARACTER_DOWNED` | I punti ferita del personaggio hanno raggiunto zero. |
| `CHARACTER_CONTROL_ASSUMED` | Il DM ha assunto temporaneamente il controllo. |
| `CHARACTER_CONTROL_RELEASED` | Il controllo e tornato al proprietario. |
| `SPAWN_MONSTER` | Creazione di un mostro sulla mappa. |
| `ATTACK` | Attacco tra due entita. |
| `DAMAGE` | Applicazione di danno a un bersaglio. |
| `ROUND_END` | Fine di un round di gioco. |

Il parser Java accetta anche altri valori testuali per `eventType`, ma gli eventi non elencati qui non vengono generati dal simulatore attuale.

### 5.2 Regole di movimento D&D per l'MVP

Per l'MVP BoardHub si assume una griglia D&D semplificata:

- una casella e l'unita logica della plancia, circa 1,5 metri;
- negli endpoint dimostrativi il chiamante fornisce i punti movimento; nel
  flusso autorevole vengono letti dal personaggio persistito;
- una casella normale costa 1 punto movimento;
- una casella di terreno difficile costa 2 punti movimento;
- il movimento puo essere ortogonale o diagonale;
- una diagonale costa quanto la casella di arrivo;
- una diagonale non puo tagliare angoli bloccati da muri o celle non attraversabili;
- il movimento non dipende dal tiro di dado;
- i muri stanno sui bordi tra celle adiacenti e bloccano il passaggio;
- ostacoli, celle inaccessibili e caselle occupate bloccano l'ingresso nella casella;
- le trappole non bloccano necessariamente il movimento e possono restare nascoste ai giocatori;
- una trappola nascosta puo essere rivelata con una prova o rilevata internamente quando il percorso la attraversa;
- la conferma aggiorna la pedina e registra `MOVE_CONFIRMED` atomicamente;
- un rifiuto non modifica lo stato e viene restituito come errore API, senza
  creare un evento nello storico.

Il calcolo interno delle celle raggiungibili usa Dijkstra semplificato sulla griglia. Questo permette alla plancia o all'app di mostrare al giocatore solo le caselle valide anche quando sono presenti terreni con costi diversi.

Quando il movimento non viene eseguito passo per passo, il backend mantiene anche il percorso scelto dall'algoritmo. Questo serve a verificare se il tragitto attraversa una casella con trappola, anche se la casella finale e diversa.

### 5.3 Contratto dei comandi per la plancia

Il backend pubblica un sottoinsieme minimo e privo di segreti:

| Comando | Origine | Campi utili |
| :--- | :--- | :--- |
| `SHOW_PATH` | `MOVE_CONFIRMED` | `sessionPieceId`, destinazione e percorso confermato. |
| `SHOW_CORRECTION_CELL` | `TRAP_TRIGGERED` | Pedina e cella sulla quale correggere la posizione fisica. |
| `CLEAR_EFFECT` | Fine tiro o risoluzione | Rimuove l'effetto temporaneo dalla plancia. |

Il payload non contiene `trapId`, CD, formula dei danni, note del DM o token.
Queste informazioni restano nel backend e nella sola proiezione autorizzata.

### 5.4 Contratto previsto per i dadi liberi

Il tiro salvezza legato alle trappole e gia generato dal backend. Il lancio
libero di dadi fisici o digitali richiesto dal DM e la relativa API generica
non sono ancora implementati nel repository.

| Dado | Uso principale nel progetto |
| :--- | :--- |
| `d20` | Prove, attacchi e tiri salvezza semplificati. |
| `d4`, `d6`, `d8`, `d10`, `d12` | Danni o effetti. |
| `d%` | Valori percentuali opzionali, non prioritari per l'MVP. |

Esempio futuro di evento di tiro dado:

```json
{
  "eventId": "evt-000056",
  "eventType": "DICE_ROLLED",
  "venueId": "venue-01",
  "tableId": "table-04",
  "sessionId": "session-20260630-001",
  "source": "APP",
  "occurredAt": "2026-06-30T17:50:00Z",
  "sequenceNumber": 56,
  "payload": {
    "characterId": "adv-01",
    "dice": "1d20",
    "modifier": 3,
    "result": 17,
    "total": 20,
    "reason": "ATTACK_ROLL"
  }
}
```

Un movimento rifiutato usa il formato uniforme degli errori REST e non viene
salvato nello stream degli eventi, perche non ha prodotto una transizione di
stato.

## 6. Formato standard degli eventi

Tutti gli eventi MQTT pubblicati sul topic `events` devono rispettare questa struttura generale:

```json
{
  "eventId": "evt-000042",
  "eventType": "MOVE",
  "venueId": "venue-01",
  "tableId": "table-04",
  "sessionId": "session-20260630-001",
  "source": "SIMULATOR",
  "occurredAt": "2026-06-30T17:45:00Z",
  "sequenceNumber": 42,
  "payload": {
    "characterId": "adv-01",
    "from": "A3",
    "to": "A4"
  }
}
```

### 6.1 Campi comuni

| Campo | Tipo | Obbligatorio | Descrizione |
| :--- | :--- | :--- | :--- |
| `eventId` | string | Si | Identificativo univoco dell'evento. |
| `eventType` | string | Si | Tipo di evento. |
| `venueId` | string | Si | Identificativo tecnico dell'installazione o sorgente logica. |
| `tableId` | string | Si | Identificativo del tavolo o ambiente di gioco. |
| `sessionId` | string | Si | Sessione di gioco associata. |
| `source` | string | Si | Origine tecnica dell'evento, ad esempio `SIMULATOR`, `EDGE` o `MOBILE_APP`. `BACKEND` e riservato agli eventi creati internamente dall'event-service e viene rifiutato sugli ingressi MQTT. |
| `occurredAt` | string | Si | Timestamp ISO 8601 dell'evento. |
| `sequenceNumber` | number | Si | Numero progressivo per coppia `(sessionId, source)`; sorgenti indipendenti possono usare la stessa sequenza. |
| `payload` | object | Si | Dati specifici del tipo evento. |

### 6.2 Esempio evento di attacco

```json
{
  "eventId": "evt-000043",
  "eventType": "ATTACK",
  "venueId": "venue-01",
  "tableId": "table-04",
  "sessionId": "session-20260630-001",
  "source": "SIMULATOR",
  "occurredAt": "2026-06-30T17:46:10Z",
  "sequenceNumber": 43,
  "payload": {
    "attackerId": "adv-01",
    "targetId": "mon-03",
    "weapon": "longsword",
    "hit": true,
    "damage": 8
  }
}
```

### 6.3 Esempio associazione pedina

Questo evento rappresenta l'associazione tra giocatore, personaggio e pedina fisica tramite QR code o NFC.

```json
{
  "eventId": "evt-000010",
  "eventType": "TOKEN_ASSIGNED",
  "venueId": "venue-01",
  "tableId": "table-04",
  "sessionId": "session-20260630-001",
  "source": "MOBILE_APP",
  "occurredAt": "2026-06-30T17:40:00Z",
  "sequenceNumber": 10,
  "payload": {
    "playerId": "player-01",
    "characterId": "adv-01",
    "tokenId": "token-adv-01",
    "assignmentMethod": "QR"
  }
}
```

### 6.4 Esempio futuro di status del nodo edge

Topic:

```text
boardhub/v1/venues/venue-01/tables/table-04/status
```

Payload:

```json
{
  "edgeId": "edge-venue-01-table-04",
  "venueId": "venue-01",
  "tableId": "table-04",
  "status": "ONLINE",
  "lastEventSequence": 43,
  "timestamp": "2026-06-30T17:46:30Z"
}
```

## 7. Gestione errori

Gli errori REST devono usare un formato uniforme:

```json
{
  "code": "SESSION_NOT_FOUND",
  "message": "La sessione richiesta non esiste."
}
```

Codici principali:

| Codice | Quando viene usato |
| :--- | :--- |
| `BAD_REQUEST` | Il payload JSON o i parametri della richiesta non rispettano il contratto. |
| `SESSION_NOT_FOUND` | La sessione richiesta non esiste. |
| `DUPLICATE_SESSION` | Si prova a creare una sessione con un identificativo gia presente. |
| `RESOURCE_NOT_FOUND` | QR o richiesta di ingresso non esistenti. |
| `JOIN_CONFLICT` | La richiesta non e piu pendente, la sessione e chiusa o il tavolo e occupato. |
| `CAPACITY_REACHED` | E stato raggiunto il limite di tavoli o giocatori. |
| `RATE_LIMITED` | Lo stesso giocatore ha inviato troppe richieste in un minuto. |
| `DM_UNAUTHORIZED` | Il token Bearer della sessione DM e assente, non valido, revocato o riferito a un'altra sessione. |
| `PLAYER_UNAUTHORIZED` | Il token Bearer del giocatore e assente, non valido, revocato o riferito a un'altra sessione. |
| `VENUE_UNAUTHORIZED` | La credenziale amministrativa del locale e assente, non valida o usata da un'origine non consentita. |
| `PIECE_NOT_FOUND` | La pedina non esiste nella sessione o non appartiene al giocatore autenticato. |
| `STALE_PIECE_STATE` | La versione inviata non coincide con lo stato corrente della pedina. |
| `MOVE_COMMAND_CONFLICT` | Un `commandId` gia registrato e stato riutilizzato per un movimento diverso. |
| `MOVE_REJECTED` | La destinazione non e raggiungibile nello stato corrente della griglia. |

Per MQTT non e previsto un messaggio di errore sincrono. Gli errori di validazione devono essere registrati dal backend e, se necessario, pubblicati su un topic di diagnostica in una fase successiva del progetto.

## 8. Regole di sincronizzazione

Il sistema deve gestire anche scenari in cui il componente edge perde temporaneamente la connessione con il broker o con il backend.

Regole minime:

- ogni evento deve avere un `eventId` univoco;
- `sequenceNumber` cresce senza duplicati per ogni coppia `(sessionId, source)`;
- il backend ignora eventi duplicati secondo i vincoli di unicita del database;
- gli eventi letti per sessione vengono ordinati per `occurredAt`, `source`,
  `sequenceNumber` ed `eventId`;
- i comandi REST di movimento usano `commandId` come chiave idempotente e
  `expectedVersion` per il controllo concorrente;
- buffer offline, topic `sync` e replay completo dello stato sono sviluppi futuri e non fanno parte dell'implementazione attuale.

Queste regole permettono di dimostrare concetti rilevanti per PISSIR: comunicazione asincrona, tolleranza a disconnessioni temporanee, idempotenza e consistenza dello stato applicativo.

## 9. Stato del documento

| Campo | Valore |
| :--- | :--- |
| Versione | `0.8` |
| Stato | Contratto allineato a controllo tavoli, accessi, personaggi, pedine, movimento autorevole e risoluzione trappole |
| Data | 2026-08-08 |
| Ambito | MVP BoardHub |

Prossimi passi:

- validare il contratto con il collaboratore;
- mantenere sincronizzata la specifica OpenAPI con gli endpoint implementati;
- aggiungere filtri o paginazione alla lettura eventi se il volume dati cresce;
- progettare e implementare componente edge, buffer offline, acknowledgement
  applicativo e riallineamento;
- introdurre il secondo microservizio minimo per risultati, statistiche e
  torneo richiesto dalla traccia;
- integrare i contratti stabili nel frontend e, solo successivamente, nell'app
  mobile.
