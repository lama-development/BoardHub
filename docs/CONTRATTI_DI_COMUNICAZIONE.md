# Contratti di comunicazione - BoardHub

## 1. Scopo del documento

Questo documento definisce i contratti di comunicazione utilizzati dai componenti principali di **BoardHub**.

L'obiettivo e stabilire in modo esplicito:

- le API REST esposte dal backend verso le applicazioni client;
- i topic MQTT usati per la comunicazione asincrona tra simulatore, componente edge, broker e backend;
- il formato standard dei messaggi JSON;
- le regole minime per identificare, ordinare e sincronizzare gli eventi di gioco.

Il documento segue un approccio **contract-first**: le interfacce vengono definite prima dell'implementazione dei servizi applicativi, in modo da ridurre ambiguita tra frontend, backend, simulatore e componente edge.

Questa versione descrive il contratto iniziale dell'MVP e si concentra sul nucleo rilevante per PISSIR: generazione, trasmissione, ricezione, ordinamento e persistenza degli eventi di gioco.

## 2. Componenti coinvolti

| Componente | Responsabilita | Protocollo principale |
| :--- | :--- | :--- |
| **Web App / Dashboard** | Visualizza sessioni live, eventi ricevuti, stato della partita e statistiche tecniche. | HTTP/REST |
| **Backend BoardHub** | Espone API REST, valida i dati, persiste eventi e stato delle sessioni. | HTTP/REST, MQTT |
| **Broker MQTT Mosquitto** | Smista i messaggi asincroni tra simulatori, edge e backend. | MQTT |
| **Simulatore software** | Genera eventi di gioco in sostituzione dei sensori fisici. | MQTT |
| **Componente Edge** | Rappresenta il nodo connesso associato al tavolo di gioco, bufferizza eventi e comunica con il broker. | MQTT |
| **PostgreSQL** | Memorizza eventi, sessioni e informazioni utili a ricostruire lo stato della partita. | SQL interno |

Il database non viene esposto direttamente ai client. Tutto l'accesso ai dati deve passare dal backend.

### 2.1 Implementazione attuale

| Componente | Stato | Note |
| :--- | :--- | :--- |
| Broker MQTT | Implementato in ambiente Docker | Disponibile tramite Mosquitto per i test locali. |
| Simulatore software | Implementato | Pubblica una mini-sessione D&D ordinata sul topic `events`. |
| Subscriber backend | Implementato | `event-service` riceve e interpreta gli eventi MQTT. |
| Persistenza eventi | Implementata e verificata | `event-service` salva gli eventi in `game_schema.game_events`; test end-to-end eseguito con PostgreSQL Docker. |
| API REST applicative | Da implementare | Gli endpoint sono gia definiti come contratto, ma non ancora esposti. |

## 3. Convenzioni di naming

Per evitare ambiguita tra documentazione, codice, payload JSON e database, i contratti tecnici usano nomi in inglese.

| Concetto | Nome tecnico | Esempio |
| :--- | :--- | :--- |
| Nodo/installazione dimostrativa | `venueId` | `venue-01` |
| Tavolo | `tableId` | `table-04` |
| Sessione di gioco | `sessionId` | `session-20260630-001` |
| Evento | `eventId` | `evt-000042` |
| Avventuriero / personaggio | `characterId` | `adv-01` |
| Mostro | `monsterId` | `mon-03` |
| Nodo edge | `edgeId` | `edge-venue-01-table-04` |

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

### 4.3 Sessioni di gioco

| Metodo | Endpoint | Scopo |
| :--- | :--- | :--- |
| `POST` | `/api/v1/sessions` | Crea una nuova sessione di gioco dimostrativa. |
| `GET` | `/api/v1/sessions/{sessionId}` | Restituisce lo stato corrente della sessione. |
| `GET` | `/api/v1/sessions/{sessionId}/events` | Restituisce lo storico eventi della sessione. |
| `GET` | `/api/v1/sessions/{sessionId}/stats` | Restituisce statistiche aggregate della sessione. |

### 4.4 Esempio creazione sessione

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
| `MOVE` | Movimento di un personaggio o mostro sulla griglia. |
| `SPAWN_MONSTER` | Creazione di un mostro sulla mappa. |
| `ATTACK` | Attacco tra due entita. |
| `DAMAGE` | Applicazione di danno a un bersaglio. |
| `OBSTACLE` | Inserimento di un ostacolo sulla mappa. |
| `WALL` | Inserimento o modifica di una parete. |
| `ROUND_END` | Fine di un round di gioco. |

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

### 6.3 Esempio status del nodo edge

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
| Stato | Contratto iniziale con primo subscriber backend implementato |
| Data | 2026-07-02 |
| Ambito | MVP BoardHub |

Prossimi passi:

- validare il contratto con il collaboratore;
- trasformare gli endpoint REST in una specifica OpenAPI;
- esporre una prima API REST per leggere gli eventi salvati;
- verificare i payload con MQTT Explorer;
- usare questi contratti come base per l'implementazione backend.
