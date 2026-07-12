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
| **PostgreSQL** | Memorizza eventi, sessioni e configurazione della griglia. | SQL interno |
| **Dashboard web** | Consulta stato del backend ed eventi di una sessione. | REST |
| **App mobile, plancia fisica ed edge** | Componenti previsti per gli sviluppi successivi. | REST, MQTT |

Il database non viene esposto direttamente ai client. Tutto l'accesso ai dati deve passare dal backend.

### 2.1 Regole operative del tavolo

Il tavolo BoardHub rappresenta una sessione D&D fisica o simulata. Per l'MVP il dominio viene limitato a poche regole operative, sufficienti per generare eventi chiari e verificabili:

| Area | Regola | Effetto sui contratti |
| :--- | :--- | :--- |
| Accesso giocatore | Previsto per l'app mobile; non ancora implementato. | Futuri eventi di associazione pedina/personaggio. |
| Preparazione mappa | La configurazione iniziale della griglia e accettata dall'API di creazione sessione. | Celle, terreno, muri e trappole nel payload REST. |
| Movimento | Il backend calcola le celle raggiungibili su griglia stateless o salvata. | Risposta REST con costo, percorso e trappole visibili. |
| Dadi | Previsto per l'app mobile; non ancora implementato. | Futuro evento `DICE_ROLLED`. |
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
| Ricostruzione griglia | Implementata come servizio interno | Lo stato persistito puo essere convertito in `GameGrid` per il calcolo del movimento. |
| API REST eventi | Implementata | `event-service` espone gli eventi persistiti tramite `GET /api/v1/sessions/{sessionId}/events`. |
| API REST sessioni | Implementata | `POST /api/v1/sessions` salva una sessione con la griglia iniziale. |
| API REST movimento | Implementata | Le due API `reachable-cells` calcolano il movimento su griglia stateless o persistita. |
| Dashboard web | Base implementata | Consuma health check e storico eventi; l'integrazione con la griglia variabile deve essere completata. |

Per mantenere prevedibile il carico del servizio, una richiesta accetta al massimo **2.500 celle complessive** e **100 punti movimento**. Questi limiti sono protezioni tecniche dell'MVP, non regole del regolamento D&D.

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

Le API REST attualmente implementate sono usate per creare sessioni, leggere eventi persistiti e calcolare il movimento.

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
| `POST` | `/api/v1/sessions` | Crea una nuova sessione di gioco dimostrativa. |
| `GET` | `/api/v1/sessions/{sessionId}/events` | Restituisce lo storico eventi della sessione, ordinato per `sequenceNumber`. |
| `POST` | `/api/v1/movement/reachable-cells` | Calcola le celle raggiungibili su una griglia fornita nella richiesta. |
| `POST` | `/api/v1/sessions/{sessionId}/movement/reachable-cells` | Calcola le celle raggiungibili usando la griglia salvata della sessione. |

L'endpoint stateless `reachable-cells` e implementato come operazione `POST` perche riceve una griglia completa: dimensioni, terreno, celle occupate, muri e trappole.

L'endpoint legato a `sessionId` rappresenta il flusso piu vicino all'uso reale: app o dashboard inviano solo personaggio, posizione iniziale e punti movimento; il backend ricostruisce la griglia persistita e applica il calcolo del movimento.

Per proteggere le informazioni riservate al Dungeon Master, il campo `trapsOnPath` delle risposte REST contiene solo trappole con visibilita `REVEALED`. Le trappole `HIDDEN` e `ALWAYS_HIDDEN` possono essere rilevate internamente dal backend, ma non vengono comunicate al client giocatore.

Le richieste con griglie oltre 2.500 celle, configurazioni sproporzionate o `movementPoints` fuori dall'intervallo `0..100` vengono rifiutate con `400 Bad Request`.

### 4.3 Esempio creazione sessione con griglia iniziale

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
  "title": "Cripta del Re Caduto",
  "gameType": "DND",
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
        "armed": true
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
  "title": "Cripta del Re Caduto",
  "gameType": "DND",
  "status": "ACTIVE",
  "gridWidth": 3,
  "gridHeight": 3,
  "createdAt": "2026-07-05T16:00:00Z"
}
```

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
      "trapsOnPath": []
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

## 5. Contratto MQTT

MQTT viene usato per la comunicazione asincrona e real-time tra simulatore, componente edge e backend.

Topic MQTT attualmente utilizzato:

| Topic | Direzione | Scopo |
| :--- | :--- | :--- |
| `boardhub/v1/venues/{venueId}/tables/{tableId}/events` | Simulatore -> Backend | Pubblicazione degli eventi della demo. |

Esempio topic reale:

```text
boardhub/v1/venues/venue-01/tables/table-04/events
```

Il topic dell'`event-service` e configurabile tramite `BOARDHUB_MQTT_TOPIC`. Il valore predefinito ascolta un solo tavolo dimostrativo; un'installazione con piu tavoli dovra usare una sottoscrizione wildcard controllata o istanze configurate per gruppi di tavoli.

### 5.1 Tipi di evento attualmente prodotti dalla demo

| Evento | Descrizione |
| :--- | :--- |
| `SESSION_START` | Avvio di una nuova sessione. |
| `MOVE` | Movimento di un personaggio o mostro sulla griglia. |
| `SPAWN_MONSTER` | Creazione di un mostro sulla mappa. |
| `ATTACK` | Attacco tra due entita. |
| `DAMAGE` | Applicazione di danno a un bersaglio. |
| `ROUND_END` | Fine di un round di gioco. |

Il parser Java accetta anche altri valori testuali per `eventType`, ma gli eventi non elencati qui non vengono generati dal simulatore attuale.

### 5.2 Regole di movimento D&D per l'MVP

Per l'MVP BoardHub si assume una griglia D&D semplificata:

- una casella e l'unita logica della plancia, circa 1,5 metri;
- nella demo il chiamante fornisce i punti movimento disponibili;
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
- la conferma o il rifiuto definitivo dello spostamento sono sviluppi successivi: l'API attuale calcola le celle raggiungibili.

Il calcolo interno delle celle raggiungibili usa Dijkstra semplificato sulla griglia. Questo permette alla plancia o all'app di mostrare al giocatore solo le caselle valide anche quando sono presenti terreni con costi diversi.

Quando il movimento non viene eseguito passo per passo, il backend mantiene anche il percorso scelto dall'algoritmo. Questo serve a verificare se il tragitto attraversa una casella con trappola, anche se la casella finale e diversa.

### 5.3 Contratto previsto per i dadi

Il progetto prevede dadi fisici o digitali, ma il tiro digitale e la relativa generazione di eventi non sono ancora implementati nel repository.

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

Esempio futuro di evento di movimento rifiutato:

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
| `source` | string | Si | Origine tecnica dell'evento, ad esempio `SIMULATOR`, `EDGE` o `MOBILE_APP`. Il backend richiede solo un valore non vuoto. |
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

Per MQTT non e previsto un messaggio di errore sincrono. Gli errori di validazione devono essere registrati dal backend e, se necessario, pubblicati su un topic di diagnostica in una fase successiva del progetto.

## 8. Regole di sincronizzazione

Il sistema deve gestire anche scenari in cui il componente edge perde temporaneamente la connessione con il broker o con il backend.

Regole minime:

- ogni evento deve avere un `eventId` univoco;
- `sequenceNumber` cresce in modo progressivo e non duplicato per ogni sessione;
- il backend ignora eventi duplicati secondo i vincoli di unicita del database;
- gli eventi letti per sessione vengono ordinati per `sequenceNumber` e `occurredAt`;
- buffer offline, topic `sync` e replay completo dello stato sono sviluppi futuri e non fanno parte dell'implementazione attuale.

Queste regole permettono di dimostrare concetti rilevanti per PISSIR: comunicazione asincrona, tolleranza a disconnessioni temporanee, idempotenza e consistenza dello stato applicativo.

## 9. Stato del documento

| Campo | Valore |
| :--- | :--- |
| Versione | `0.2` |
| Stato | Contratto allineato a subscriber, persistenza, sessioni e API REST movimento implementati |
| Data | 2026-07-11 |
| Ambito | MVP BoardHub |

Prossimi passi:

- validare il contratto con il collaboratore;
- mantenere sincronizzata la specifica OpenAPI con gli endpoint implementati;
- aggiungere filtri o paginazione alla lettura eventi se il volume dati cresce;
- completare la dashboard DM e implementare app mobile e componente edge;
- introdurre progressivamente dadi, conferma movimento, buffer offline ed event replay.
