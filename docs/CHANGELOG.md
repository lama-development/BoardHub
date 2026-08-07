# BoardHub
> Changelog pubblico del repository GitHub.
> Riassume le modifiche rilevanti al progetto per mantenere allineato il collaboratore.
> I comandi dettagliati, i test eseguiti e le note personali restano nel diario di bordo privato.

---

## [0.22.0] - 2026-07-29

Autore: Andrea Perini
Ambito: Movimento autorevole delle pedine

## Added

- Aggiunte API protette per calcolare le destinazioni dalla posizione
  persistita e confermare atomicamente il movimento di una pedina posseduta.
- Aggiunti `commandId` idempotente, controllo della versione della pedina ed
  evento `MOVE_CONFIRMED` salvato insieme alla nuova posizione.
- Aggiunta la migrazione Flyway `V6`, i comandi `just` e i test di servizio,
  controller, repository e schema relativi al movimento.

## Changed

- Posizione, velocita, occupazione e percorso vengono ora ricavati dallo stato
  autorevole del backend invece che dichiarati dal client.
- La sequenza degli eventi generati dal backend e separata da quella delle
  sorgenti MQTT; lo storico usa un ordinamento cronologico deterministico.
- Aggiornati OpenAPI, contratti, scope e guida operativa al nuovo flusso.

## Fixed

- Impediti doppi spostamenti durante i retry, aggiornamenti da una versione
  superata e movimenti di pedine appartenenti ad altri partecipanti.
- Evitata l'esposizione delle trappole nascoste nelle risposte del giocatore.
- Resa atomica la scrittura di posizione ed evento, evitando stati parziali.
- Riservata la sorgente `BACKEND` all'event-service per evitare collisioni
  provocate da messaggi MQTT esterni.

---

## [0.21.0] - 2026-07-27

Autore: Andrea Perini
Ambito: Pedine virtuali autorevoli della sessione

## Added

- Aggiunta la migrazione Flyway `V5` con associazione vincolata tra sessione,
  partecipante, personaggio, pedina virtuale e cella corrente.
- Aggiunte API protette per creare e leggere le pedine del giocatore e una
  proiezione completa riservata al DM.
- Aggiunti comandi `just` e rendering leggibile per posizionare e ispezionare
  le pedine senza comporre manualmente le richieste REST.

## Changed

- La ricostruzione della griglia considera occupate anche le celle contenenti
  pedine persistite, rendendo il loro stato effettivo per il calcolo del
  movimento.
- Il posizionamento riusa lo stato occupato della griglia senza una seconda
  query sulla stessa cella; il vincolo univoco del database continua a gestire
  eventuali richieste concorrenti.
- Aggiornati OpenAPI, contratti e guide operative al nuovo flusso
  personaggio-pedina-cella.

## Fixed

- Impediti l'uso di personaggi appartenenti ad altri giocatori, la doppia
  associazione dello stesso personaggio e il posizionamento simultaneo di due
  pedine sulla stessa cella.
- Rifiutati posizionamenti fuori griglia, su terreno non attraversabile o con
  modalita hardware non ancora implementate.

---

## [0.20.2] - 2026-07-27

Autore: Andrea Perini
Ambito: Leggibilita dei comandi operativi

## Added

- Aggiunto un formatter condiviso per presentare in terminale tavoli,
  sessioni, richieste, partecipanti, personaggi, movimenti ed eventi.
- Aggiunta la modalita `BOARDHUB_OUTPUT=json` per consultare, quando serve, la
  risposta JSON completa e colorata.

## Changed

- Sostituiti gli header HTTP e i JSON compatti dei comandi `just` con
  riepiloghi, tabelle, conteggi e indicazioni operative coerenti.

## Fixed

- Rese comprensibili le liste vuote e le risposte di errore senza perdere il
  codice e il messaggio restituiti dall'API.

---

## [0.20.1] - 2026-07-27

Autore: Andrea Perini
Ambito: Instradamento MQTT multi-tavolo

## Changed

- Estesa la sottoscrizione MQTT predefinita a tutti i tavoli del locale tramite
  una wildcard limitata al segmento `tableId`.
- Reso dinamico il topic usato da `just publish-event` in base al tavolo della
  sessione.

## Fixed

- Corretto `just check` sui tavoli diversi dal Tavolo 4: l'evento viene ora
  pubblicato sul topic corretto e ricevuto dall'`event-service`.
- Rifiutati gli eventi nei quali `venueId` o `tableId` del payload non
  corrispondono al topic MQTT, evitando attribuzioni incoerenti.

---

## [0.20.0] - 2026-07-26

Autore: Andrea Perini
Ambito: Controllo tavoli del locale e credenziale DM per sessione

## Added

- Aggiunta una console REST privata per elencare, abilitare, disabilitare e
  chiudere i tavoli del locale.
- Aggiunta una finestra temporanea di claim che consente a un solo dispositivo
  di creare la sessione dopo l'autorizzazione del personale.
- Aggiunto il token Bearer `bhd1` associato al partecipante DM e a una sola
  sessione.
- Aggiunti comandi `just` per le operazioni del locale e per la prova
  end-to-end sul reale inventario degli otto tavoli.

## Changed

- Sostituito lo stato pubblico `AVAILABLE` con gli stati `DISABLED`,
  `CLAIMABLE` e `IN_SESSION`.
- Rimossa la password DM globale dal QR e dal sito pubblico; il browser che
  crea la sessione conserva soltanto il token della relativa sessione.
- Aggiornati frontend, OpenAPI, contratti e guida operativa al nuovo ciclo di
  vita controllato dal locale.

## Fixed

- Impedito a un visitatore del QR di avviare una partita senza
  un'abilitazione temporanea del personale.
- Impedito a due dispositivi concorrenti di reclamare lo stesso tavolo.
- Revocati i token DM e giocatore alla chiusura e riportato il tavolo allo
  stato disabilitato.
- Corretto `just check`, che ora usa un tavolo configurato e rifiuta di
  sovrascrivere una sessione gia attiva.
- Validata all'avvio la configurazione della finestra di claim e della chiave
  amministrativa, evitando durate impossibili o credenziali troppo deboli.

---

## [0.19.0] - 2026-07-26

Autore: Andrea Perini
Ambito: Personaggi posseduti dai partecipanti

## Added

- Aggiunta la migrazione Flyway `V3` con personaggi collegati in modo vincolato
  al partecipante e alla sessione.
- Aggiunte creazione e lettura dei personaggi tramite token Bearer del
  giocatore, oltre alla vista completa riservata al Dungeon Master.
- Aggiunti campi tattici minimi, visibilita, versione, limite configurabile e
  comandi `just` per la prova manuale.
- Aggiunti test di migrazione, proprieta, isolamento, autenticazione,
  validazione, capienza e revoca alla chiusura della sessione.

## Changed

- Esteso il contratto protetto del giocatore oltre `/me`, senza permettere al
  client di scegliere proprietario o sessione del personaggio.
- Aggiornati OpenAPI, contratti, scope e guida operativa allo stato realmente
  implementato.

## Fixed

- Serializzato il controllo del numero di personaggi per evitare che richieste
  concorrenti superino il limite del partecipante.
- Impedita la lettura dei personaggi di altri giocatori dalla proiezione
  autenticata del proprietario.
- Rimosso il limite universale inventato sull'eta: resta positiva, mentre
  l'eventuale massimo dipende dalla specie e dalla campagna.
- Distinte nei contratti le regole ufficiali D&D dai limiti tecnici BoardHub
  applicati a caselle, HP e Classe Armatura.

---

## [0.18.2] - 2026-07-25

Autore: Andrea Perini
Ambito: Architettura backend e autenticazione del giocatore

## Added

- Aggiunti input applicativi indipendenti dai DTO HTTP per sessioni, richieste
  di ingresso e configurazione della griglia.
- Aggiunti verifica HMAC del token giocatore, controllo del partecipante e
  revoca legata allo stato della sessione.
- Aggiunto `GET /api/v1/player/sessions/{sessionId}/me` come primo endpoint
  realmente protetto dalla credenziale Bearer del giocatore.
- Aggiunti test positivi e negativi per token alterati, sessione errata,
  credenziale mancante e sessione conclusa.

## Changed

- Separati controller, mapper, comandi applicativi, servizi ed eccezioni in
  package coerenti con le rispettive responsabilita.
- Rinominato il controller DM per rappresentare richieste, partecipanti e ciclo
  di vita della sessione senza cambiare i path HTTP esistenti.
- Aggiornati OpenAPI e contratti di comunicazione con il ciclo completo della
  credenziale giocatore.

## Fixed

- Eliminata la dipendenza dei servizi dai DTO del layer HTTP.
- Impedito l'uso di token giocatore contraffatti, associati a un'altra sessione
  o appartenenti a partecipanti e sessioni non piu attivi.
- Isolato `just check` dalle sessioni reali del locale mediante un tavolo
  temporaneo univoco, evitando conflitti con tavoli gia occupati.

---

## [0.18.1] - 2026-07-24

Autore: Andrea Perini
Ambito: Continuita dell'accesso DM e giocatore

## Added

- Aggiunta la lettura autenticata dello stato di una richiesta di ingresso da parte del dispositivo che l'ha creata.
- Aggiunto il polling automatico della decisione del Dungeon Master.

## Changed

- Il browser conserva richiesta e credenziale di possesso per ripristinare lo stato dopo un refresh.
- Dopo l'accettazione il dispositivo del giocatore riceve la propria credenziale firmata di sessione.
- Il dispositivo che avvia una sessione come Dungeon Master conserva localmente
  l'accesso alla relativa dashboard.

## Fixed

- Impedito che il refresh riporti un giocatore in attesa al modulo iniziale.
- Impedito che il refresh riporti il Dungeon Master alla pagina pubblica del
  giocatore durante una sessione ancora attiva.
- Gestiti esplicitamente nel sito gli stati accettato, rifiutato e scaduto.
- Impedita la lettura dello stato tramite il solo identificativo pubblico della richiesta.
- Resa compatibile la generazione degli identificativi del browser anche quando
  il sito viene aperto da un telefono tramite HTTP sulla rete locale.

---

## [0.18.0] - 2026-07-23

Autore: Andrea Perini
Ambito: Pagina pubblica del tavolo e avvio tramite QR

## Added

- Aggiunta la pagina web pubblica aperta dal QR stabile di ogni tavolo.
- Aggiunto lo stato pubblico `AVAILABLE` o `IN_SESSION` con informazioni minime della sessione.
- Aggiunti avvio autorizzato della sessione per il DM e richiesta di ingresso del giocatore direttamente dal sito.
- Aggiunti i comandi `just frontend`, `just qr` e `just table-status`.

## Changed

- Il QR apre la pagina del tavolo invece di mostrare direttamente una risposta JSON del backend.
- La creazione di una sessione richiede la chiave DM anche tramite API.
- Il limite dei tavoli attivi considera soltanto i tavoli che ospitano una sessione.

## Fixed

- Distinto un tavolo valido ma libero da un QR inesistente o fuori intervallo.
- Aggiunto un errore esplicito per numeri di tavolo inferiori a 1 o superiori a 8.

---

## [0.17.0] - 2026-07-23

Autore: Andrea Perini
Ambito: Chiusura della sessione e consolidamento del flusso di ingresso

## Added

- Aggiunta la chiusura della sessione con liberazione del tavolo.
- Aggiunti comandi rapidi per verificare QR, richieste, partecipanti e conclusione della sessione.
- Aggiunta copertura automatica del flusso completo su schema database migrato.

## Changed

- La chiusura di una sessione disattiva i partecipanti, conclude le richieste ancora pendenti e rende nuovamente disponibile il tavolo.
- Il controllo end-to-end chiude sempre la sessione temporanea anche quando una verifica intermedia fallisce.

## Fixed

- Evitato che una sessione conclusa lasci tavolo, richieste o partecipanti ancora attivi.
- Resi fallibili i comandi REST quando il backend restituisce una risposta HTTP di errore.
- Corretto il controllo end-to-end affinche si interrompa subito con un messaggio operativo quando il backend non e raggiungibile.
- Corretto il controllo end-to-end affinche verifichi che l'evento MQTT sia stato realmente ricevuto e persistito.

---

## [0.16.3] - 2026-07-22

Autore: Andrea Perini
Ambito: Approvazione del Dungeon Master e partecipanti

## Added

- Aggiunte approvazione e rifiuto delle richieste tramite API riservate al Dungeon Master.
- Aggiunti partecipanti persistiti con ruolo, stato e collegamento alla richiesta accettata.
- Aggiunta una credenziale locale firmata per il partecipante dopo l'approvazione.
- Aggiunta la lettura protetta delle richieste pendenti e dei partecipanti attivi.

## Changed

- Applicato un limite configurabile di otto giocatori attivi per sessione.
- Centralizzato il controllo della credenziale del Dungeon Master.

## Fixed

- Impediti partecipanti duplicati e approvazioni concorrenti oltre la capienza.
- Gestite con risposte HTTP specifiche credenziali DM mancanti o errate.

---

## [0.16.2] - 2026-07-21

Autore: Andrea Perini
Ambito: Risoluzione del QR e richieste di ingresso

## Added

- Aggiunta la risoluzione del QR pubblico del tavolo nella sessione attiva.
- Aggiunte richieste di ingresso idempotenti con scadenza automatica.
- Aggiunto un limite configurabile alle richieste ripetute dallo stesso giocatore.

## Changed

- Limitata la risposta pubblica a titolo, introduzione e dati non riservati della sessione.
- Separato il riferimento del dispositivo del giocatore dall'identificativo del partecipante.

## Fixed

- Impedito il riuso della stessa chiave idempotente con dati differenti.
- Impedita la creazione di più richieste pendenti equivalenti per lo stesso giocatore.

---

## [0.16.1] - 2026-07-20

Autore: Andrea Perini
Ambito: Tavoli riutilizzabili e informazioni pubbliche della sessione

## Added

- Aggiunti tavoli persistiti con identificatore QR pubblico stabile.
- Aggiunto il collegamento univoco tra tavolo e sessione attiva.
- Aggiunti introduzione pubblica e controllo dell'apertura delle richieste di ingresso.

## Changed

- Estesa la creazione della sessione con identificativo pubblico e nome del tavolo.
- Applicato un limite configurabile di otto tavoli attivi contemporaneamente.

## Fixed

- Impedita la creazione concorrente di due sessioni attive sullo stesso tavolo.
- Impedito l'uso dello stesso QR pubblico per tavoli differenti.

---

## [0.16.0] - 2026-07-19

Autore: Andrea Perini
Ambito: Migrazioni versionate del database

## Added

- Introdotto Flyway con una migrazione iniziale verificabile per lo schema dell'event-service.
- Aggiunti test automatici per database vuoto e schema PostgreSQL preesistente.

## Changed

- Spostata la responsabilita di creare e aggiornare le tabelle da Docker all'avvio controllato dell'event-service.
- Sostituita l'applicazione manuale di `init.sql` con migrazioni incrementali che conservano i dati esistenti.

---

## [0.15.1] - 2026-07-12

Autore: Andrea Perini
Ambito: Comando MQTT di prova

## Fixed

- Resa univoca ogni pubblicazione eseguita con `just publish-event`, evitando che eventi successivi vengano scartati come duplicati.
- Corretta la gestione delle variabili shell usate dai comandi `publish-event` e `check`.

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
