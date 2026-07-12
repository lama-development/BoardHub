# BoardHub
> Changelog pubblico del repository GitHub.
> Riassume le modifiche rilevanti al progetto per mantenere allineato il collaboratore.
> I comandi dettagliati, i test eseguiti e le note personali restano nel diario di bordo privato.

---

## [0.15.0] - 2026-07-11

Autore: Andrea Perini
Ambito: Robustezza del backend, sicurezza locale e integrita dei dati

## Changed

- Limitata l'elaborazione a griglie con massimo 2.500 celle e richieste fino a 100 punti movimento.
- Resi configurabili tramite variabili d'ambiente database, broker, client MQTT, topic e QoS.
- Limitata alle connessioni locali l'esposizione Docker di PostgreSQL e Mosquitto.
- Semplificata la ricostruzione dei percorsi calcolati dall'algoritmo di movimento.

## Fixed

- Centralizzata la costruzione della griglia per rifiutare celle fuori mappa, conflitti di terreno, muri diagonali e trappole duplicate.
- Impedita l'esposizione REST delle trappole non rivelate ai giocatori.
- Rifiutata una posizione iniziale su terreno bloccato o ostacolo.
- Aggiunta la validazione dei campi obbligatori degli eventi MQTT.
- Aggiunta l'unicita del sequenceNumber all'interno della stessa sessione PostgreSQL.
- Ottimizzata la costruzione e ricostruzione della griglia con una sola copia immutabile finale.
- Rimossi gli indici PostgreSQL ridondanti gia coperti dalle chiavi e dai vincoli delle tabelle.
- Documentato l'aggiornamento dello schema per volumi PostgreSQL gia esistenti.

---

## [0.14.0] - 2026-07-07

Autore: Andrea Perini
Ambito: Comandi rapidi di sviluppo e test

## Added

| Area | Elemento | Motivo |
| :--- | :--- | :--- |
| Tooling | justfile alla radice del progetto. | Semplifica l'avvio dell'infrastruttura, del backend e dei test. |
| README | Riferimento a just help. | Permette al collaboratore di scoprire i comandi disponibili. |

---

## [0.13.0] - 2026-07-06

Autore: Andrea Perini
Ambito: Gestione errori REST

## Added

| Area | Elemento | Motivo |
| :--- | :--- | :--- |
| REST API | Risposte strutturate per errori applicativi. | Evita risposte generiche 500 quando l'errore e prevedibile. |
| Sessioni | Gestione sessione duplicata con 409 Conflict. | Segnala correttamente un identificativo gia presente. |
| Movimento | Gestione sessione inesistente con 404 Not Found. | Segnala che non esiste una griglia persistita. |
| OpenAPI | Documentazione di 400, 404 e 409. | Allinea Apidog e collaboratore al comportamento reale dell'API. |

---

## [0.12.0] - 2026-07-05

Autore: Andrea Perini
Ambito: Creazione sessione con griglia iniziale

## Added

| Area | Elemento | Motivo |
| :--- | :--- | :--- |
| REST API | POST /api/v1/sessions. | Crea una sessione D&D e salva la griglia iniziale. |
| Backend | Servizio di creazione sessione. | Salva sessione, terreno, celle occupate, muri e trappole. |
| OpenAPI | Specifica per richiesta e risposta di creazione sessione. | Permette di utilizzare il flusso da Apidog. |
| Test | Test controller e servizio di creazione sessione. | Copre il salvataggio e il successivo uso della sessione. |

---

## [0.11.0] - 2026-07-05

Autore: Andrea Perini
Ambito: API movimento da sessione salvata

## Added

| Area | Elemento | Motivo |
| :--- | :--- | :--- |
| REST API | POST /api/v1/sessions/{sessionId}/movement/reachable-cells. | Calcola il movimento usando la griglia persistita. |
| Backend | Servizio applicativo per movimento da sessione. | Collega ricostruzione griglia e algoritmo di Dijkstra. |
| OpenAPI | Specifica aggiornata con il nuovo endpoint. | Permette di testare la chiamata da client REST. |
| Test | Test servizio e controller session movement. | Verifica il flusso sessione -> griglia -> celle raggiungibili. |

---

## [0.10.0] - 2026-07-05

Autore: Andrea Perini
Ambito: Ricostruzione griglia da sessione

## Added

| Area | Elemento | Motivo |
| :--- | :--- | :--- |
| Backend | Servizio di ricostruzione griglia da sessione persistita. | Collega il database al modello usato dal movimento. |
| Movimento | Conversione di celle, muri e trappole persistite in GameGrid. | Prepara il calcolo da una sessione reale. |
| Test | Verifica della ricostruzione completa della griglia. | Controlla dimensioni, terreno, occupazione, muri e trappole. |

---

## [0.9.0] - 2026-07-05

Autore: Andrea Perini
Ambito: Persistenza sessione e plancia

## Added

| Area | Elemento | Motivo |
| :--- | :--- | :--- |
| Database | Tabelle per sessioni, celle, muri e trappole. | Salva la configurazione della plancia associata a una sessione. |
| Backend | Modelli Java per sessione e stato griglia. | Separa stato persistito e griglia di movimento. |
| Repository | Lettura e scrittura JDBC dello stato sessione. | Collega database e calcolo delle celle raggiungibili. |
| Test | Test repository sessione e plancia. | Copre salvataggio e lettura dello stato. |

---

## [0.8.0] - 2026-07-05

Autore: Andrea Perini
Ambito: API REST movimento

## Added

| Area | Elemento | Motivo |
| :--- | :--- | :--- |
| REST API | POST /api/v1/movement/reachable-cells. | Rende invocabile dall'esterno il calcolo del movimento. |
| Request | Griglia con dimensioni, terreno, celle, muri e trappole. | Permette di testare il movimento senza sessioni persistite. |
| Response | Celle raggiungibili con costo, percorso e trappole attraversate. | Fornisce dati utilizzabili da app, dashboard e plancia. |

---

## [0.7.0] - 2026-07-05

Autore: Andrea Perini
Ambito: Calcolo movimento su griglia

## Added

| Area | Elemento | Motivo |
| :--- | :--- | :--- |
| Movimento | MovementService con Dijkstra semplificato. | Calcola le celle raggiungibili in base ai punti movimento. |
| Percorso | Ricostruzione del tragitto verso ogni cella raggiungibile. | Permette di sapere da quali caselle passa il personaggio. |
| Trappole | Rilevamento delle trappole armate lungo il percorso. | Permette di gestire trappole attraversate. |
| Diagonali | Movimento in 8 direzioni con regola anti-taglio angolo. | Evita il passaggio attraverso muri o angoli bloccati. |

---

## [0.6.0] - 2026-07-04

Autore: Andrea Perini
Ambito: Modello logico della griglia D&D

## Added

| Area | Elemento | Motivo |
| :--- | :--- | :--- |
| Griglia | Posizioni convertibili da coordinate tipo A3 o B12. | Rappresenta le caselle in modo stabile. |
| Terreno | Tipi NORMAL, DIFFICULT, OBSTACLE e BLOCKED. | Gestisce costi diversi e celle non attraversabili. |
| Celle | Stato di posizione, terreno e occupazione. | Determina attraversabilita e costo. |
| Muri | Segmenti sui bordi tra celle adiacenti. | Blocca il passaggio senza trasformare le celle in muri. |
| Trappole | Stati HIDDEN, REVEALED e ALWAYS_HIDDEN. | Gestisce trappole e visibilita ai giocatori. |
| Griglia di gioco | Modello rettangolare con celle e muri configurabili. | Fornisce la base all'algoritmo di movimento. |
| Movimento | Richiesta e risultato delle celle raggiungibili. | Prepara l'esposizione del calcolo via API. |

---

## [0.5.0] - 2026-07-04

Autore: Andrea Perini
Ambito: Riallineamento idea progettuale

## Changed

- Il progetto viene impostato come plancia fisica Dungeons & Dragons connessa, non come semplice web app.
- Il flusso principale diventa: plancia o simulatore, nodo edge, MQTT, backend, database e API.
- Vengono introdotti app mobile, associazione pedina tramite QR/NFC e vista di controllo per il Dungeon Master.
- Vengono definiti eventi per pedine, turni, movimento, dadi e modifica del terreno.
- Vengono chiariti Dijkstra semplificato per il movimento, ordinamento degli eventi e sviluppi futuri per dadi ed event replay.

---

## [0.4.0] - 2026-07-03

Autore: Andrea Perini
Ambito: Prima API REST per lettura eventi
## Added

| Area | File | Motivo |
| :--- | :--- | :--- |
| REST API | GameEventController.java | Espone gli eventi salvati di una sessione. |
| Repository | GameEventRepository.java | Legge gli eventi ordinati per sessionId. |
| Test | GameEventControllerTest.java | Verifica la risposta JSON dell'endpoint. |
| Contratto API | docs/openapi/event-service.openapi.yml | Definisce l'endpoint REST in OpenAPI. |

## Changed

- L'API di lettura eventi diventa il primo punto di integrazione concreto per dashboard e frontend.

---

## [0.3.0] - 2026-07-02

Autore: Andrea Perini
Ambito: Persistenza eventi di gioco

## Added

| Area | File | Motivo |
| :--- | :--- | :--- |
| Database | docker/postgres/init.sql | Crea game_schema.game_events e l'indice per sessione. |
| Backend | GameEventRepository.java | Salva gli eventi MQTT ricevuti. |
| Test | GameEventRepositoryTest.java | Verifica inserimento e duplicati tramite eventId. |
| Configurazione | application.yml | Configura la connessione PostgreSQL. |

## Changed

- MqttEventSubscriber persiste gli eventi dopo il parsing.
- pom.xml include JDBC, driver PostgreSQL e H2 per i test.

---

## [0.2.0] - 2026-07-02

Autore: Andrea Perini
Ambito: Primo microservizio Java/Spring Boot per ricezione MQTT

## Added

| Area | File | Motivo |
| :--- | :--- | :--- |
| Backend | services/event-service/pom.xml | Definisce microservizio e dipendenze MQTT, Jackson e test. |
| Backend | EventServiceApplication.java | Avvia il microservizio. |
| Configurazione | application.yml | Configura porta HTTP 8082, Actuator e topic MQTT. |
| MQTT | MqttEventSubscriber.java | Riceve gli eventi dal broker. |
| Parsing | GameEventParser.java, GameEvent.java | Trasforma JSON MQTT in oggetti Java tipizzati. |
| Test | GameEventParserTest.java | Verifica il parsing di un evento MOVE. |

## Changed

- Aggiornato .gitignore per escludere le cartelle Maven target/.

---

## [0.1.0] - 2026-07-01

Autore: Andrea Perini
Ambito: Infrastruttura Docker, contratti REST/MQTT e simulatore MQTT

## Added

| Area | Elemento | Motivo |
| :--- | :--- | :--- |
| Infrastruttura | docker/docker-compose.yml | Avvia PostgreSQL e Mosquitto con un comando. |
| Database | docker/postgres/init.sql | Crea gli schemi iniziali del progetto. |
| MQTT | docker/mosquitto/mosquitto.conf | Configura Mosquitto per i test locali. |
| Contratti | Topic, payload JSON e API previste. | Definisce la comunicazione tra i componenti. |
| Simulatore | simulator/publish_event.py | Pubblica una mini-sessione D&D su MQTT. |
| Subscriber | simulator/subscribe_events.py | Riceve e stampa gli eventi MQTT. |
| Dipendenze | simulator/requirements.txt | Definisce paho-mqtt. |

## Changed

- Separata la struttura iniziale in docker/, docs/ e simulator/.
- Adottato il topic MQTT versionato boardhub/v1/venues/{venueId}/tables/{tableId}/events.
- Aggiornato .gitignore per ambienti virtuali, cache Python, file IDE, .env e file macOS.
