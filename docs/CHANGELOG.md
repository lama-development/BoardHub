# BoardHub
> Changelog pubblico del repository GitHub.  
> Serve a tenere allineato il collaboratore su cosa e stato fatto e sullo stato del progetto.  
> I comandi dettagliati, i tentativi da terminale e le note personali restano nel diario di bordo privato.

---

## [0.8.0] - 2026-07-05

| Campo | Dettaglio |
| :--- | :--- |
| **Autore** | Andrea Perini |
| **Ambito** | API REST movimento |
| **Stato** | Endpoint implementato e testato |

## Added

| Area | Elemento | Motivo |
| :--- | :--- | :--- |
| REST API | `POST /api/v1/movement/reachable-cells`. | Rende utilizzabile dall'esterno il calcolo delle celle raggiungibili. |
| Contratto request | Griglia con dimensioni, terreno, celle bloccate, occupazione, muri e trappole. | Permette di testare il movimento senza dipendere ancora da sessioni persistite. |
| Contratto response | Celle raggiungibili con costo, percorso e trappole attraversate. | Fornisce dati pronti per app, dashboard o plancia. |

## Validated

- Endpoint verificato con test REST.
- Verificata esposizione di celle raggiungibili, percorso e trappole attraversate.
- Test complessivi dell'`event-service` superati.

## Result

Il calcolo del movimento non e piu solo logica interna: ora puo essere invocato via HTTP.

---

## [0.7.0] - 2026-07-05

| Campo | Dettaglio |
| :--- | :--- |
| **Autore** | Andrea Perini |
| **Ambito** | Calcolo movimento su griglia |
| **Stato** | Algoritmo implementato e testato |

## Added

| Area | Elemento | Motivo |
| :--- | :--- | :--- |
| Movimento | `MovementService` con Dijkstra semplificato. | Calcola le celle raggiungibili da un personaggio in base ai punti movimento. |
| Percorso | Ricostruzione del tragitto verso ogni cella raggiungibile. | Serve a sapere da quali caselle passa il personaggio. |
| Trappole | Rilevamento delle trappole armate lungo il percorso. | Permette di attivare trappole anche quando non sono la destinazione finale. |
| Diagonali | Movimento in 8 direzioni con regola anti-taglio angolo. | Permette un movimento piu naturale sulla griglia senza attraversare muri o angoli bloccati. |

## Validated

- Verificato movimento base su griglia.
- Verificato costo maggiore del terreno difficile.
- Verificati ostacoli, celle bloccate e celle occupate.
- Verificati muri sui bordi tra celle adiacenti.
- Verificato tracciamento delle trappole attraversate.
- Verificato movimento diagonale e blocco della diagonale quando taglia un angolo occupato da muro o cella non attraversabile.
- Test complessivi dell'`event-service` superati.

## Result

Il backend ora puo calcolare internamente:

```text
griglia + posizione iniziale + punti movimento -> celle raggiungibili + percorso + trappole attraversate
```

---

## [0.6.0] - 2026-07-04

| Campo | Dettaglio |
| :--- | :--- |
| **Autore** | Andrea Perini |
| **Ambito** | Modello logico della griglia D&D |
| **Stato** | Modello implementato e testato |

## Added

| Area | Elemento | Motivo |
| :--- | :--- | :--- |
| Griglia | Posizioni convertibili da coordinate tipo `A3` o `B12`. | Permette di rappresentare le caselle della plancia in modo stabile. |
| Terreno | Tipi `NORMAL`, `DIFFICULT`, `OBSTACLE`, `BLOCKED`. | Prepara il calcolo del movimento con costi diversi e caselle non attraversabili. |
| Celle | Stato di una cella: posizione, terreno e occupazione. | Serve a sapere se una casella e attraversabile e quanto costa entrarci. |
| Muri | Segmenti sui bordi tra due celle adiacenti. | Blocca il passaggio tra celle senza trasformare le celle in muri. |
| Trappole | Stato `HIDDEN`, `REVEALED`, `ALWAYS_HIDDEN`. | Distingue le trappole dagli ostacoli: possono essere attraversabili e non visibili ai giocatori. |
| Griglia di gioco | Modello rettangolare con celle configurabili e muri sui lati. | Base per l'algoritmo di movimento su plancia. |
| Movimento | Richiesta di movimento e risultato raggiungibile. | Prepara il futuro calcolo delle celle raggiungibili. |

## Validated

- Test automatici del modello griglia superati.
- Verificata conversione coordinate, gestione terreno difficile, ostacoli, celle bloccate, muri sui bordi, trappole nascoste e celle occupate.
- Test complessivi dell'`event-service` superati.

## Result

Il backend ora possiede i tipi di dominio necessari per implementare il prossimo step:

```text
posizione iniziale + punti movimento + griglia -> celle raggiungibili
```

---

## [0.5.0] - 2026-07-04

| Campo | Dettaglio |
| :--- | :--- |
| **Autore** | Andrea Perini |
| **Ambito** | Riallineamento idea progettuale |
| **Stato** | Impostazione progettuale aggiornata |

## Changed

- Il progetto viene impostato come plancia fisica Dungeons & Dragons connessa, non come semplice web app.
- Il flusso principale diventa: plancia fisica o simulata, nodo edge, MQTT, backend, database e API.
- Vengono introdotti app mobile, associazione pedina tramite QR/NFC e vista di controllo per il Dungeon Master.
- Vengono definiti nuovi eventi di dominio per pedine, turni, movimento, dadi e modifica del terreno.
- Vengono chiariti gli algoritmi previsti: ordinamento eventi gia implementato, Dijkstra semplificato per il movimento e parser dadi/event replay come sviluppi successivi.
- Viene definito il materiale operativo del tavolo: plancia, pedine, app, dadi, gestione DM, terreno, ostacoli e trappole.

## Result

Il progetto ora segue questo caso d'uso fisico:

```text
plancia D&D / simulatore -> edge -> MQTT -> backend -> database -> API -> app mobile
```

---

## [0.4.0] - 2026-07-03

| Campo | Dettaglio |
| :--- | :--- |
| **Autore** | Andrea Perini |
| **Ambito** | Prima API REST per lettura eventi |
| **Stato** | Endpoint implementato e testato |

## Added

| Area | File | Motivo |
| :--- | :--- | :--- |
| REST API | `GameEventController.java` | Espone gli eventi salvati di una sessione. |
| Repository | `GameEventRepository.java` | Aggiunge la lettura ordinata degli eventi per `sessionId`. |
| Test | `GameEventControllerTest.java` | Verifica la risposta JSON dell'endpoint REST. |
| Contratto API | `docs/openapi/event-service.openapi.yml` | Definisce in formato OpenAPI l'endpoint REST implementato. |

## Changed

- L'API di lettura eventi diventa il primo punto di integrazione concreto per dashboard e frontend.

## Validated

- Test automatici Java superati.
- Verificata la lettura ordinata degli eventi per sessione.

## Result

Il backend ora espone:

```text
GET /api/v1/sessions/{sessionId}/events
```

Questo endpoint e il primo punto di integrazione concreto per dashboard e frontend.

---

## [0.3.0] - 2026-07-02

| Campo | Dettaglio |
| :--- | :--- |
| **Autore** | Andrea Perini |
| **Ambito** | Persistenza eventi di gioco |
| **Stato** | Repository JDBC implementato e testato |

## Added

| Area | File | Motivo |
| :--- | :--- | :--- |
| Database | `docker/postgres/init.sql` | Aggiunge la tabella `game_schema.game_events` e un indice per leggere gli eventi per sessione. |
| Backend | `GameEventRepository.java` | Salva gli eventi MQTT ricevuti nel database. |
| Test | `GameEventRepositoryTest.java` | Verifica inserimento evento e gestione dei duplicati tramite `eventId`. |
| Configurazione | `application.yml` | Aggiunge la connessione PostgreSQL coerente con `docker-compose.yml`. |

## Changed

- `MqttEventSubscriber` ora, dopo il parsing, chiama il repository per persistere l'evento.
- `pom.xml` include JDBC, driver PostgreSQL e H2 per i test.

## Validated

- Test automatici Java superati.
- Verificato inserimento evento e gestione duplicati tramite `eventId`.
- Verificata la pipeline reale con Docker: simulatore, MQTT, backend e PostgreSQL.

## Result

La mini-sessione D&D generata dal simulatore viene ricevuta dal backend e salvata in PostgreSQL:

```text
SESSION_START -> MOVE -> SPAWN_MONSTER -> ATTACK -> DAMAGE -> ROUND_END
```

Totale eventi salvati nel test: `6`.

## Notes

- Se il volume Docker `postgres_data` era gia stato creato prima di questa modifica, lo script `init.sql` potrebbe non essere rieseguito automaticamente.
- Prossimo passo: esporre una API REST per leggere gli eventi salvati.

---

## [0.2.0] - 2026-07-02

| Campo | Dettaglio |
| :--- | :--- |
| **Autore** | Andrea Perini |
| **Ambito** | Primo microservizio backend Java/Spring Boot per ricezione eventi MQTT |
| **Stato** | Pipeline applicativa verificata con broker MQTT locale |

## Added

| Area | File | Motivo |
| :--- | :--- | :--- |
| Backend | `services/event-service/pom.xml` | Definisce il microservizio Spring Boot e le dipendenze MQTT/Jackson/Test. |
| Backend | `services/event-service/src/main/java/.../EventServiceApplication.java` | Punto di avvio del service. |
| Configurazione | `services/event-service/src/main/resources/application.yml` | Configura porta HTTP `8082`, Actuator e topic MQTT. |
| MQTT | `MqttEventSubscriber.java` | Si connette al broker, si iscrive al topic eventi e riceve messaggi di gioco. |
| Parsing | `GameEventParser.java`, `GameEvent.java` | Trasforma i payload JSON MQTT in oggetti Java tipizzati. |
| Test | `GameEventParserTest.java` | Verifica che un evento `MOVE` venga letto correttamente dal parser. |

## Changed

- Aggiornato `.gitignore` per escludere le cartelle Maven `target/`.

## Validated

- Test automatico del parser MQTT superato.
- Verificata la ricezione degli eventi da parte del microservizio.
- Health check del service funzionante.

## Result

```text
simulatore Python -> broker MQTT -> event-service Java/Spring Boot
```

## Notes

- In questa fase il service riceve e logga gli eventi.
- La persistenza su PostgreSQL viene aggiunta nello step successivo.

---

## [0.1.0] - 2026-07-01

| Campo | Dettaglio |
| :--- | :--- |
| **Autore** | Andrea Perini |
| **Ambito** | Infrastruttura Docker, contratti REST/MQTT e simulatore MQTT |
| **Stato** | Prima base tecnica funzionante |

## Added

| Area | Elemento | Motivo |
| :--- | :--- | :--- |
| Infrastruttura | `docker/docker-compose.yml` | Avvia in locale PostgreSQL e Mosquitto con un solo comando. |
| Database | `docker/postgres/init.sql` | Crea gli schemi iniziali `venue_schema`, `game_schema`, `stats_schema`. |
| MQTT | `docker/mosquitto/mosquitto.conf` | Configura Mosquitto per i test locali sulla porta `1883`. |
| Contratti REST/MQTT | Topic, payload JSON, API previste e regole di sincronizzazione. | Definisce come comunicano simulatore, broker, backend e client. |
| Simulatore | `simulator/publish_event.py` | Pubblica una mini-sessione D&D sul broker MQTT. |
| Subscriber | `simulator/subscribe_events.py` | Riceve gli eventi MQTT e li stampa in forma leggibile. |
| Dipendenze | `simulator/requirements.txt` | Definisce la libreria Python `paho-mqtt`. |

## Changed

- Separata la struttura del progetto in tre aree iniziali:

  ```text
  docker/     -> infrastruttura locale
  docs/       -> contratti tecnici condivisi
  simulator/  -> script Python per simulazione e debug MQTT
  ```

- Adottato un topic MQTT versionato:

  ```text
  boardhub/v1/venues/{venueId}/tables/{tableId}/events
  ```

- Aggiornato `.gitignore` per escludere ambienti virtuali, cache Python, file IDE, `.env` e file macOS.

## Validated

- Infrastruttura Docker avviata correttamente.
- Broker MQTT raggiungibile.
- Simulatore e subscriber Python verificati con una mini-sessione D&D.

## Result

```text
SESSION_START -> MOVE -> SPAWN_MONSTER -> ATTACK -> DAMAGE -> ROUND_END
```

## Notes

- Backend Java/Spring Boot non ancora implementato.
- Il lavoro attuale dimostra la prima parte della pipeline PISSIR: generazione evento, trasporto MQTT e ricezione.
