# Contratti di comunicazione - BoardHub

## 1. Scopo del documento

Questo documento definisce i contratti di comunicazione utilizzati dai componenti principali di **BoardHub**.

L'obiettivo e stabilire in modo esplicito:

- le API REST esposte dal backend verso le applicazioni client;
- i topic MQTT usati per la comunicazione asincrona tra simulatore, componente edge, broker e backend;
- il formato standard dei messaggi JSON;
- le regole minime per identificare, ordinare e sincronizzare gli eventi di gioco.

Il documento segue un approccio **contract-first**: le interfacce vengono definite prima dell'implementazione dei servizi applicativi, in modo da ridurre ambiguita tra frontend, backend, simulatore e componente edge.

Questa versione descrive il contratto iniziale dell'MVP e si concentra sul nucleo rilevante per PISSIR: generazione, trasmissione, ricezione, ordinamento e persistenza degli eventi di gioco prodotti da una plancia D&D fisica o simulata.

## 2. Componenti coinvolti

| Componente | Responsabilita | Protocollo principale |
| :--- | :--- | :--- |
| **App Mobile Giocatore** | Associa pedina e personaggio, mostra turno, azioni, dadi e registro combattimento. | HTTP/REST |
| **Dashboard/App Dungeon Master** | Avvia sessione, imposta mappa, mostri, muri, trappole, turni e round. | HTTP/REST |
| **Plancia fisica / simulata** | Rappresenta la griglia D&D, rileva o simula posizione pedine e feedback visivo. | MQTT |
| **Backend BoardHub** | Espone API REST, valida i dati, persiste eventi e stato delle sessioni. | HTTP/REST, MQTT |
| **Broker MQTT Mosquitto** | Smista i messaggi asincroni tra simulatori, edge e backend. | MQTT |
| **Simulatore software** | Genera eventi di gioco in sostituzione temporanea della plancia fisica. | MQTT |
| **Componente Edge** | Nodo associato al tavolo: bufferizza eventi, valida dati locali e comunica con il broker. | MQTT |
| **PostgreSQL** | Memorizza eventi, sessioni e informazioni utili a ricostruire lo stato della partita. | SQL interno |

Il database non viene esposto direttamente ai client. Tutto l'accesso ai dati deve passare dal backend.

### 2.1 Regole operative del tavolo

Il tavolo BoardHub rappresenta una sessione D&D fisica o simulata. Per l'MVP il dominio viene limitato a poche regole operative, sufficienti per generare eventi chiari e verificabili:

| Area | Regola | Effetto sui contratti |
| :--- | :--- | :--- |
| Accesso giocatore | Il giocatore usa l'app mobile e associa una pedina tramite QR/NFC. | Eventi `PLAYER_JOINED`, `TOKEN_ASSIGNED`, `CHARACTER_CREATED`. |
| Preparazione mappa | Il Dungeon Master posiziona personaggi, creature, muri, ostacoli, terreno difficile e trappole visibili o nascoste. | Eventi `TOKEN_PLACED`, `SPAWN_MONSTER`, `WALL`, `OBSTACLE`, `TERRAIN_UPDATED`, `TRAP_REVEALED`, `TRAP_TRIGGERED`. |
| Movimento | Il movimento dipende da velocita, casella iniziale e costo del terreno. | Eventi `REACHABLE_CELLS_CALCULATED`, `MOVE_CONFIRMED`, `MOVE_REJECTED`. |
| Dadi | Il set previsto e `d4`, `d6`, `d8`, `d10`, `d%`, `d12`, `d20`. | Eventi `DICE_ROLLED`; il `d20` e usato per prove/attacchi, gli altri per danni/effetti. |
| Turni e round | Il Dungeon Master controlla l'avanzamento logico del combattimento. | Eventi `TURN_STARTED`, `ROUND_END`, `SESSION_END`. |

I contratti non automatizzano tutto il regolamento D&D. Descrivono solo le informazioni necessarie per dimostrare plancia connessa, comunicazione MQTT, persistenza e consultazione tramite API.

### 2.2 Implementazione attuale

| Componente | Stato | Note |
| :--- | :--- | :--- |
| Broker MQTT | Implementato in ambiente Docker | Disponibile tramite Mosquitto per i test locali. |
| Simulatore software | Implementato | Pubblica una mini-sessione D&D ordinata sul topic `events`. |
| Subscriber backend | Implementato | `event-service` riceve e interpreta gli eventi MQTT. |
| Persistenza eventi | Implementata e verificata | `event-service` salva gli eventi in `game_schema.game_events`; test end-to-end eseguito con PostgreSQL Docker. |
| API REST eventi | Implementata | `event-service` espone gli eventi persistiti tramite `GET /api/v1/sessions/{sessionId}/events`. |

## 3. Convenzioni di naming

Per evitare ambiguita tra documentazione, codice, payload JSON e database, i contratti tecnici usano nomi in inglese.

| Concetto | Nome tecnico | Esempio |
| :--- | :--- | :--- |
| Nodo/installazione dimostrativa | `venueId` | `venue-01` |
| Tavolo | `tableId` | `table-04` |
| Sessione di gioco | `sessionId` | `session-20260630-001` |
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

Le API REST sono usate per operazioni sincrone: consultazione dati, creazione sessioni, lettura eventi persistiti e statistiche.

Base path:

```text
/api/v1
```

La specifica OpenAPI dell'`event-service` e disponibile in:

```text
docs/openapi/event-service.openapi.yml
```

### 4.1 Endpoint infrastrutturali

| Metodo | Endpoint | Scopo |
| :--- | :--- | :--- |
| `GET` | `/api/v1/health` | Verifica lo stato del backend. |

### 4.2 Sorgenti di gioco

| Metodo | Endpoint | Scopo |
| :--- | :--- | :--- |
| `GET` | `/api/v1/sources` | Restituisce le sorgenti tecniche note al sistema, ad esempio simulatori o nodi edge. |
| `GET` | `/api/v1/sources/{sourceId}/status` | Restituisce lo stato tecnico di una sorgente di eventi. |

Questi endpoint sono opzionali per l'MVP. La parte prioritaria resta la lettura delle sessioni e degli eventi persistiti.

### 4.3 Sessioni di gioco e stato

| Metodo | Endpoint | Scopo |
| :--- | :--- | :--- |
| `POST` | `/api/v1/sessions` | Crea una nuova sessione di gioco dimostrativa. |
| `GET` | `/api/v1/sessions/{sessionId}` | Restituisce lo stato corrente della sessione. |
| `GET` | `/api/v1/sessions/{sessionId}/events` | Restituisce lo storico eventi della sessione, ordinato per `sequenceNumber`. |
| `GET` | `/api/v1/sessions/{sessionId}/stats` | Restituisce statistiche aggregate della sessione. |
| `POST` | `/api/v1/movement/reachable-cells` | Calcola le celle raggiungibili su una griglia fornita nella richiesta. |

L'endpoint `reachable-cells` e implementato come operazione `POST` perche riceve una griglia completa: dimensioni, terreno difficile, celle bloccate, celle occupate, muri e trappole. In una fase successiva potra essere collegato direttamente allo stato persistito di una sessione.

### 4.4 Esempio calcolo celle raggiungibili

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
      "trapsOnPath": ["trap-01"]
    }
  ]
}
```

### 4.5 Esempio lettura eventi sessione

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
    "venueId": "venue-01",
    "tableId": "table-04",
    "sessionId": "session-20260630-001",
    "source": "SIMULATOR",
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

### 4.6 Esempio creazione sessione

Richiesta:

```http
POST /api/v1/sessions
Content-Type: application/json
```

```json
{
  "venueId": "venue-01",
  "tableId": "table-04",
  "gameType": "DND",
  "title": "Cripta del Re Caduto"
}
```

Risposta:

```json
{
  "sessionId": "session-20260630-001",
  "venueId": "venue-01",
  "tableId": "table-04",
  "status": "ACTIVE",
  "createdAt": "2026-06-30T17:45:00Z"
}
```

## 5. Contratto MQTT

MQTT viene usato per la comunicazione asincrona e real-time tra simulatore, componente edge e backend.

Topic principali:

| Topic | Direzione | Scopo |
| :--- | :--- | :--- |
| `boardhub/v1/venues/{venueId}/tables/{tableId}/events` | Edge/Simulatore -> Backend | Pubblicazione eventi di gioco. |
| `boardhub/v1/venues/{venueId}/tables/{tableId}/status` | Edge -> Backend | Stato del nodo edge o del tavolo. |
| `boardhub/v1/venues/{venueId}/tables/{tableId}/commands` | Backend -> Edge | Comandi inviati dal backend verso il tavolo. |
| `boardhub/v1/venues/{venueId}/tables/{tableId}/sync` | Edge -> Backend | Invio di eventi accumulati offline. |

Esempio topic reale:

```text
boardhub/v1/venues/venue-01/tables/table-04/events
```

### 5.1 Tipi di evento supportati

| Evento | Descrizione |
| :--- | :--- |
| `SESSION_START` | Avvio di una nuova sessione. |
| `SESSION_END` | Chiusura di una sessione. |
| `PLAYER_JOINED` | Un giocatore entra nella sessione. |
| `TOKEN_ASSIGNED` | Una pedina fisica viene associata a giocatore e personaggio. |
| `CHARACTER_CREATED` | Creazione o configurazione del personaggio. |
| `ENCOUNTER_START` | Avvio di un combattimento da parte del Dungeon Master. |
| `TOKEN_PLACED` | Posizionamento iniziale di una pedina sulla griglia. |
| `TURN_STARTED` | Inizio turno di un personaggio o mostro. |
| `REACHABLE_CELLS_CALCULATED` | Calcolo delle celle raggiungibili per il movimento. |
| `MOVE` | Movimento di un personaggio o mostro sulla griglia. |
| `MOVE_CONFIRMED` | Movimento accettato dal sistema. |
| `MOVE_REJECTED` | Movimento rifiutato per distanza, muro tra celle, ostacolo o casella non valida. |
| `SPAWN_MONSTER` | Creazione di un mostro sulla mappa. |
| `DICE_ROLLED` | Tiro di dado digitale registrato dall'app. |
| `ATTACK` | Attacco tra due entita. |
| `DAMAGE` | Applicazione di danno a un bersaglio. |
| `OBSTACLE` | Inserimento di un ostacolo sulla mappa. |
| `WALL` | Inserimento o modifica di una parete sul bordo tra due celle. |
| `TERRAIN_UPDATED` | Modifica del tipo di terreno di una o piu caselle. |
| `TRAP_REVEALED` | Rivelazione di una trappola. |
| `TRAP_TRIGGERED` | Attivazione di una trappola attraversata o raggiunta da un personaggio. |
| `ROUND_END` | Fine di un round di gioco. |

### 5.2 Regole di movimento D&D per l'MVP

Per l'MVP BoardHub si assume una griglia D&D semplificata:

- una casella e l'unita logica della plancia, circa 1,5 metri;
- un personaggio standard puo avere 6 punti movimento per turno;
- una casella normale costa 1 punto movimento;
- una casella di terreno difficile costa 2 punti movimento;
- il movimento puo essere ortogonale o diagonale;
- una diagonale costa quanto la casella di arrivo;
- una diagonale non puo tagliare angoli bloccati da muri o celle non attraversabili;
- il movimento non dipende dal tiro di dado;
- i muri stanno sui bordi tra celle adiacenti e bloccano il passaggio;
- ostacoli, celle inaccessibili e caselle occupate bloccano l'ingresso nella casella;
- le trappole non bloccano necessariamente il movimento e possono restare nascoste ai giocatori;
- una trappola nascosta puo essere rivelata con una prova o attivata quando il percorso la attraversa;
- il sistema deve distinguere movimento valido e movimento non valido.

Il calcolo interno delle celle raggiungibili usa Dijkstra semplificato sulla griglia. Questo permette alla plancia o all'app di mostrare al giocatore solo le caselle valide anche quando sono presenti terreni con costi diversi.

Quando il movimento non viene eseguito passo per passo, il backend mantiene anche il percorso scelto dall'algoritmo. Questo serve a verificare se il tragitto attraversa una casella con trappola, anche se la casella finale e diversa.

### 5.3 Regole dei dadi per l'MVP

I dadi possono essere tirati fisicamente e registrati dal Dungeon Master oppure tirati digitalmente dall'app. Per la demo e preferibile il tiro digitale, perche produce un evento verificabile e sincronizzato.

| Dado | Uso principale nel progetto |
| :--- | :--- |
| `d20` | Prove, attacchi e tiri salvezza semplificati. |
| `d4`, `d6`, `d8`, `d10`, `d12` | Danni o effetti. |
| `d%` | Valori percentuali opzionali, non prioritari per l'MVP. |

Esempio evento di tiro dado:

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

Esempio evento di movimento rifiutato:

```json
{
  "eventId": "evt-000055",
  "eventType": "MOVE_REJECTED",
  "venueId": "venue-01",
  "tableId": "table-04",
  "sessionId": "session-20260630-001",
  "source": "EDGE",
  "occurredAt": "2026-06-30T17:49:00Z",
  "sequenceNumber": 55,
  "payload": {
    "characterId": "adv-01",
    "from": "A3",
    "to": "A10",
    "reason": "OUT_OF_RANGE",
    "maxCells": 6
  }
}
```

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
| `source` | string | Si | Origine dell'evento: `SIMULATOR`, `EDGE`, `BACKEND`. |
| `occurredAt` | string | Si | Timestamp ISO 8601 dell'evento. |
| `sequenceNumber` | number | Si | Numero progressivo dell'evento nella sessione. |
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

### 6.4 Esempio status del nodo edge

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
  "message": "La sessione richiesta non esiste.",
  "traceId": "trace-20260630-001"
}
```

Codici principali:

| Codice | Quando viene usato |
| :--- | :--- |
| `VALIDATION_ERROR` | Il payload della richiesta non rispetta il contratto. |
| `SOURCE_NOT_FOUND` | La sorgente tecnica richiesta non esiste. |
| `TABLE_NOT_FOUND` | Il tavolo o ambiente di gioco richiesto non esiste. |
| `SESSION_NOT_FOUND` | La sessione richiesta non esiste. |
| `DUPLICATE_EVENT` | Il backend ha gia ricevuto un evento con lo stesso `eventId`. |
| `INTERNAL_ERROR` | Errore interno non previsto. |

Per MQTT non e previsto un messaggio di errore sincrono. Gli errori di validazione devono essere registrati dal backend e, se necessario, pubblicati su un topic di diagnostica in una fase successiva del progetto.

## 8. Regole di sincronizzazione

Il sistema deve gestire anche scenari in cui il componente edge perde temporaneamente la connessione con il broker o con il backend.

Regole minime:

- ogni evento deve avere un `eventId` univoco;
- `sequenceNumber` cresce in modo progressivo per ogni sessione;
- il backend deve ignorare eventi duplicati con lo stesso `eventId`;
- se arrivano eventi fuori ordine, il backend deve usare `sequenceNumber` e `occurredAt` per ricostruire l'ordine logico;
- durante una disconnessione, il componente edge puo accumulare eventi sul nodo;
- alla riconnessione, gli eventi accumulati vengono pubblicati sul topic `sync`;
- lo stato del tavolo viene ricostruito applicando gli eventi validi in ordine.

Queste regole permettono di dimostrare concetti rilevanti per PISSIR: comunicazione asincrona, tolleranza a disconnessioni temporanee, idempotenza e consistenza dello stato applicativo.

## 9. Stato del documento

| Campo | Valore |
| :--- | :--- |
| Versione | `0.1` |
| Stato | Contratto iniziale con subscriber, persistenza e prima API REST eventi implementati |
| Data | 2026-07-03 |
| Ambito | MVP BoardHub |

Prossimi passi:

- validare il contratto con il collaboratore;
- trasformare gli endpoint REST in una specifica OpenAPI;
- aggiungere filtri o paginazione alla lettura eventi se il volume dati cresce;
- verificare i payload con MQTT Explorer;
- usare questi contratti come base per l'implementazione backend.
