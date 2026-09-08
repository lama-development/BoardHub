# BoardHub

> Changelog pubblico del repository GitHub.
> Riassume le modifiche rilevanti al progetto per mantenere allineato il collaboratore.
> I comandi dettagliati, i test eseguiti e le note personali restano nel diario di bordo privato.

---

## [0.28.0] - 2026-09-08

Autore: Davide La Marca
Ambito: Preparazione della plancia e gestione dei turni nel frontend

## Added

- Aggiunta una preparazione guidata per il DM in tre fasi: titolo
  dell'avventura, configurazione visuale della plancia 5 x 5 e riepilogo prima
  della creazione della sessione.
- Il DM puo ora impostare dalla griglia celle difficili, bloccate e con
  ostacoli, oltre a posizionare e configurare trappole con visibilita, tiro
  salvezza, danno, durata e stato iniziale.
- Aggiunto alla console DM un tracker locale per iniziativa, mostri e PNG,
  avanzamento dei turni e dei round, pausa e preparazione di un nuovo
  combattimento; lo stato resta associato alla sessione sul dispositivo DM.
- Introdotti i contratti TypeScript della configurazione della plancia, ora
  inviata alle API al momento della creazione della sessione.

## Changed

- Centralizzati colori semantici e componenti del design system tramite il
  tema Tailwind, uniformando console, pagina pubblica, statistiche, eventi e
  controlli condivisi.

## Fixed

- L'errore restituito durante la creazione di una sessione resta visibile anche
  dopo l'aggiornamento dello stato del tavolo.

---

## [0.27.6] - 2026-09-07

Autore: Davide La Marca
Ambito: Gestione condivisa dei dati live delle sessioni

## Changed

- Estratta dai pannelli DM e giocatore la gestione di caricamento, errori e
  aggiornamento dei dati in hook dedicati e riutilizzabili.
- Centralizzato il raggruppamento degli eventi SSE in un singolo aggiornamento
  REST, mantenendo il backend come fonte autorevole e ripulendo correttamente
  i timer alla chiusura dei componenti.
- Abilitati i controlli TypeScript per parametri e variabili locali non
  utilizzati, così da intercettare prima il codice morto nel frontend.

---

## [0.27.5] - 2026-08-31

Autore: Davide La Marca
Ambito: Aggiornamenti live SSE nel frontend

## Added

- Console giocatore e DM collegate ai rispettivi stream SSE autenticati tramite
  header Bearer, senza esporre credenziali negli URL.
- Aggiunti stato di connessione, riconnessione automatica e sincronizzazione
  REST al ripristino dello stream per recuperare eventuali eventi persi.
- Rimossa l'interrogazione periodica della console DM: gli aggiornamenti degli
  eventi vengono ora raggruppati e applicati in risposta allo stream live.

---

## [0.27.4] - 2026-08-31

Autore: Davide La Marca
Ambito: Controllo temporaneo del Dungeon Master

## Added

- La console DM permette ora di assumere e restituire il controllo di un
  personaggio, mantenendo il backend come fonte autorevole dello stato.
- Il DM può calcolare le destinazioni e muovere soltanto le pedine assunte.
- Per un personaggio sotto controllo DM, la console consente anche tiro
  salvezza e prosecuzione autorevole di una trappola in attesa.

---

## [0.27.3] - 2026-08-26

Autore: Davide La Marca
Ambito: Formattazione e pulizia generale del codice

## Changed

- Uniformata la formattazione del codice frontend e riordinati dettagli di
  manutenzione, senza modificare il comportamento funzionale dell'applicazione.

---

## [0.27.2] - 2026-08-26

Autore: Davide La Marca
Ambito: Dashboard statistiche e risoluzione delle trappole nel frontend

## Added

- Aggiunto nel pannello giocatore il flusso autorevole `TRAP_PENDING`: lettura
  della risoluzione, tiro salvezza sul server e prosecuzione del movimento.
- La console DM mostra ora le trappole in attesa, con personaggio coinvolto,
  cella di attivazione e stato del flusso.
- Introdotti contratti TypeScript e chiamate API per le risoluzioni e gli esiti
  delle trappole; il client conserva e invia la versione restituita dal server.
- Aggiunta la pagina `/stats` con storico delle sessioni concluse, ricerca delle
  statistiche aggregate di un giocatore e classifica dei tornei.
- Il frontend usa ora un proxy dedicato per lo stats-service, separato dalle
  API di gioco, e contratti TypeScript per risultati, statistiche e classifiche.

---

## [0.27.1] - 2026-08-23

Autore: Davide La Marca
Ambito: Selezione visuale delle celle nella console giocatore

## Changed

- La posizione iniziale di una pedina si sceglie ora cliccando direttamente la
  plancia, senza inserire manualmente coordinate come `B2` o `C3`.
- Le destinazioni raggiungibili vengono evidenziate sulla plancia e il giocatore
  conferma il movimento selezionando la relativa cella invece di usare un
  elenco di pulsanti.
- La griglia riutilizzabile supporta selezione controllata, celle non
  disponibili e indicazioni contestuali, migliorando anche accessibilita e
  chiarezza dell'interazione.

## Fixed

- Le celle gia occupate dalle pedine del giocatore non sono selezionabili come
  posizione iniziale nella relativa console.

---

## [0.27.0] - 2026-08-22

Autore: Andrea Perini
Ambito: Classi di traffico MQTT e baseline sperimentale

## Added

- Aggiunto `just measure`, che raccoglie campioni riproducibili di latenza
  online, recupero offline e tempo fino all'acknowledgement applicativo.
- Aggiunti report Markdown, dati grezzi CSV e metadati dell'ambiente; gli
  artefatti locali di misura sono esclusi da Git.
- Aggiunti test unitari per configurazione QoS, pubblicazione dello stato,
  calcolo statistico e ripristino sicuro del tavolo usato dalla misura.

## Changed

- Eventi, acknowledgement e comandi mantengono QoS 1; lo stato tecnico
  retained usa QoS 0 perche rappresenta soltanto l'ultimo valore disponibile.
- I timestamp prodotti dall'outbox edge includono i millisecondi, necessari
  per una baseline locale significativa.

## Fixed

- La procedura di misura rifiuta tavoli gia occupati e ripristina broker,
  risorse edge e stato amministrativo del tavolo anche in caso di errore.
- Il campione di warm-up online usa ora una sequenza valida a partire da `1`,
  evitando che il backend scarti uno degli eventi della baseline.

---

## [0.26.3] - 2026-08-20

Autore: Andrea Perini
Ambito: Ordinamento edge, consegna risultati e contratti API

## Added

- Aggiunta la migrazione Flyway `V8` con un'outbox transazionale per i risultati
  di sessione e il relativo worker di consegna con ritentativi.
- Aggiunta la specifica OpenAPI separata di `stats-service` e uniformate le
  risposte di errore del servizio statistiche.
- Aggiunti test per outbox dei risultati, ordinamento edge ed errori REST del
  servizio statistiche.

## Changed

- La coda edge mantiene ora un ordine stretto per `(sessionId, source)`: un
  evento in backoff o in attesa dell'acknowledgement applicativo blocca i suoi
  successori, senza bloccare le altre sorgenti.
- Chiarito nei contratti che un evento MQTT `MOVE` proveniente dalla plancia e
  un'osservazione; lo stato autorevole cambia soltanto tramite le API protette
  di movimento e l'evento applicativo `MOVE_CONFIRMED`.
- Riallineati README, ambito, contratti e paper personali allo stato dei due
  microservizi e del nodo edge.

## Fixed

- Impedito che un evento successivo superi il precedente durante backoff o fra
  PUBACK MQTT e conferma applicativa del backend.
- Eliminata la finestra in cui il commit di chiusura poteva riuscire senza che
  il risultato raggiungesse `stats-service`: fatto e outbox vengono ora salvati
  nella stessa transazione e la pubblicazione viene ritentata fino alla conferma
  del broker.

---

## [0.26.2] - 2026-08-18

Autore: Andrea Perini
Ambito: Consegna garantita dei risultati di partita

## Fixed

- Il risultato di una partita conclusa mentre il servizio statistiche era spento
  andava perduto: la sottoscrizione usava una sessione MQTT pulita e il broker
  scartava i messaggi destinati a un client non collegato. Il servizio si
  collega ora con sessione persistente e identificativo stabile, e riceve alla
  riconnessione tutto cio che era stato pubblicato nel frattempo.

## Changed

- Abilitata la persistenza su disco del broker Mosquitto, cosi le code dei
  client scollegati sopravvivono anche a un riavvio del broker.

## Added

- Tre test che impediscono il ritorno del difetto verificando sessione
  persistente, riconnessione automatica e stabilita dell'identificativo client.

---

## [0.26.1] - 2026-08-18

Autore: Andrea Perini
Ambito: Comandi operativi del locale

## Added

- Aggiunto `just close-all`: chiude tutte le partite in corso, revoca le
  abilitazioni non ancora consumate e mostra lo stato finale dei tavoli. Serve a
  riportare il locale a tavoli liberi prima di una dimostrazione.

---

## [0.26.0] - 2026-08-18

Autore: Andrea Perini
Ambito: Secondo microservizio per risultati, statistiche e tornei

## Added

- Aggiunto `services/stats-service`, secondo microservizio backend su porta
  8083, con schema dedicato `stats_schema` gestito da Flyway.
- Aggiunto il contratto del risultato di sessione sul topic `session-results`:
  `event-service` lo pubblica alla chiusura, `stats-service` lo consuma.
- Il servizio statistiche espone risultati di sessione, statistiche per
  giocatore, tornei e classifica finale.
- Aggiunta la regola di punteggio dichiarata, consultabile via API: e una
  convenzione dimostrativa, perche D&D e cooperativo e non prevede un vincitore.
- Aggiunti i comandi `just stats`, `stats-sessions`, `stats-session`,
  `stats-player`, `create-tournament`, `tournaments`, `leaderboard` e
  `stats-test`.
- Aggiunti undici test del servizio statistiche, compresi consegna ripetuta e
  correttezza della classifica su piu sessioni.

## Changed

- La chiusura di una sessione calcola il riepilogo dei partecipanti prima delle
  mutazioni e lo pubblica dopo il commit.

---

## [0.25.0] - 2026-08-18

Autore: Andrea Perini
Ambito: Nodo edge con coda offline e acknowledgement applicativi

## Added

- Aggiunto il nodo edge `edge/`: processo Python separato che raccoglie le
  osservazioni della plancia, le conserva in una coda SQLite persistente e le
  consegna al backend con ritentativi e backoff.
- Aggiunto il contratto degli acknowledgement applicativi sul topic
  `event-acks`, con esiti `PERSISTED`, `DUPLICATE`, `REJECTED` e `CONFLICT`.
- Aggiunto il contratto dello stato tecnico del nodo edge sul topic `status`.
- Il backend pubblica l'esito applicativo di ogni evento ricevuto via MQTT, cosi
  l'edge puo chiudere un elemento solo dopo la persistenza effettiva.
- Il backend rifiuta con `CONFLICT` gli eventi destinati a una sessione conclusa.
- Aggiunti i comandi `just edge-setup`, `just edge`, `just edge-observe`,
  `just edge-queue`, `just edge-status`, `just edge-test` e `just check-offline`.
- Aggiunti dodici test dei componenti della coda locale.

## Fixed

- L'`event-service` non si iscriveva piu al topic degli eventi dopo un riavvio
  del broker MQTT. Con `cleanSession` attivo il broker scarta le sottoscrizioni
  alla disconnessione e la sola riconnessione automatica ristabiliva il socket
  ma non l'iscrizione: il servizio restava collegato e in silenzio. La
  sottoscrizione viene ora ripristinata a ogni connessione riuscita.

---

## [0.24.1] - 2026-08-16

Autore: Andrea Perini
Ambito: Schermata pubblica del tavolo

## Changed

- Riorganizzata la pagina pubblica del QR secondo il design system
  neo-brutalist condiviso, con gerarchia piu chiara fra tavolo, stato e azione
  disponibile.
- Resi piu leggibili gli stati di tavolo disabilitato, pronto e con sessione
  attiva, mantenendo invariato il flusso applicativo.

## Fixed

- Ridotta l'ambiguita fra uscita, aggiornamento dello stato e avvio della
  sessione sui layout desktop e mobile.

---

## [0.24.0] - 2026-08-11

Autore: Davide La Marca
Ambito: Integrazione frontend e design system condiviso

## Added

- Aggiunti al frontend i contratti TypeScript e le chiamate API per
  personaggi, pedine, raggiungibilita e movimento autorevole.
- Completata la console giocatore per creare il personaggio, associarlo a una
  pedina virtuale, visualizzare le destinazioni e richiedere uno spostamento.
- Estesa la console DM con viste operative per partecipanti, personaggi e
  pedine della sessione.
- Introdotti componenti UI condivisi e un design system neo-brutalist coerente
  fra pagina pubblica, dashboard, griglia e registro eventi.

## Changed

- Uniformate dashboard, griglia, metriche, schede informative e
  visualizzazione degli eventi.
- Le viste giocatore e DM usano ora lo stato persistito restituito dalle API
  invece di limitarsi alla ricostruzione dimostrativa del monitor eventi.

## Fixed

- Migliorata la coerenza visuale e informativa fra i diversi flussi web senza
  spostare nel browser le regole autorevoli di movimento.

---

## [0.23.0] - 2026-08-08

Autore: Andrea Perini
Ambito: Risoluzione autorevole delle trappole

## Added

- Aggiunta la migrazione Flyway `V7` con definizioni complete delle trappole,
  risoluzioni persistite, tiri, danni, stato tattico e controllo temporaneo DM.
- Aggiunte API protette per interrompere il movimento, eseguire un tiro
  salvezza idempotente, applicare gli effetti e proseguire con il budget residuo.
- Aggiunte proiezioni evento pubblica, giocatore e DM, aggiornamenti live SSE e
  comandi MQTT backend-edge privi di dettagli riservati.
- Aggiunti comandi `just` e output leggibili per trappole, eventi autenticati e
  controllo temporaneo dei personaggi.

## Changed

- Il movimento autorevole si arresta ora sulla prima trappola applicabile e
  conserva destinazione, percorso residuo e versione della risoluzione.
- I personaggi persistono i bonus ai sei tiri salvezza, HP aggiornabili e stato
  tattico; le pedine dichiarano chi ne possiede il controllo corrente.
- Gli eventi ricevuti da MQTT, dopo il salvataggio, aggiornano anche i client
  connessi allo stream live.
- Aggiornati OpenAPI, contratti, scope e guida operativa al flusso approvato.

## Fixed

- Impediti doppi tiri, doppi danni e prosecuzioni duplicate durante retry o
  richieste concorrenti tramite `commandId` e controllo versione.
- Impedita l'esposizione di trappole nascoste, configurazioni del DM e identita
  interne nelle API e negli stream destinati a pubblico e giocatori.
- Annullate le risoluzioni pendenti alla chiusura della sessione e reso
  idempotente il passaggio temporaneo di controllo al DM.

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

| Area    | Elemento                           | Motivo                                                          |
| :------ | :--------------------------------- | :-------------------------------------------------------------- |
| Tooling | justfile alla radice del progetto. | Semplifica l'avvio dell'infrastruttura, del backend e dei test. |
| README  | Riferimento a just help.           | Permette al collaboratore di scoprire i comandi disponibili.    |

---

## [0.13.0] - 2026-07-06

Autore: Andrea Perini
Ambito: Gestione errori REST

## Added

| Area      | Elemento                                         | Motivo                                                          |
| :-------- | :----------------------------------------------- | :-------------------------------------------------------------- |
| REST API  | Risposte strutturate per errori applicativi.     | Evita risposte generiche 500 quando l'errore e prevedibile.     |
| Sessioni  | Gestione sessione duplicata con 409 Conflict.    | Segnala correttamente un identificativo gia presente.           |
| Movimento | Gestione sessione inesistente con 404 Not Found. | Segnala che non esiste una griglia persistita.                  |
| OpenAPI   | Documentazione di 400, 404 e 409.                | Allinea Apidog e collaboratore al comportamento reale dell'API. |

---

## [0.12.0] - 2026-07-05

Autore: Andrea Perini
Ambito: Creazione sessione con griglia iniziale

## Added

| Area     | Elemento                                                  | Motivo                                                    |
| :------- | :-------------------------------------------------------- | :-------------------------------------------------------- |
| REST API | POST /api/v1/sessions.                                    | Crea una sessione D&D e salva la griglia iniziale.        |
| Backend  | Servizio di creazione sessione.                           | Salva sessione, terreno, celle occupate, muri e trappole. |
| OpenAPI  | Specifica per richiesta e risposta di creazione sessione. | Permette di utilizzare il flusso da Apidog.               |
| Test     | Test controller e servizio di creazione sessione.         | Copre il salvataggio e il successivo uso della sessione.  |

---

## [0.11.0] - 2026-07-05

Autore: Andrea Perini
Ambito: API movimento da sessione salvata

## Added

| Area     | Elemento                                                    | Motivo                                                         |
| :------- | :---------------------------------------------------------- | :------------------------------------------------------------- |
| REST API | POST /api/v1/sessions/{sessionId}/movement/reachable-cells. | Calcola il movimento usando la griglia persistita.             |
| Backend  | Servizio applicativo per movimento da sessione.             | Collega ricostruzione griglia e algoritmo di Dijkstra.         |
| OpenAPI  | Specifica aggiornata con il nuovo endpoint.                 | Permette di testare la chiamata da client REST.                |
| Test     | Test servizio e controller session movement.                | Verifica il flusso sessione -> griglia -> celle raggiungibili. |

---

## [0.10.0] - 2026-07-05

Autore: Andrea Perini
Ambito: Ricostruzione griglia da sessione

## Added

| Area      | Elemento                                                      | Motivo                                                       |
| :-------- | :------------------------------------------------------------ | :----------------------------------------------------------- |
| Backend   | Servizio di ricostruzione griglia da sessione persistita.     | Collega il database al modello usato dal movimento.          |
| Movimento | Conversione di celle, muri e trappole persistite in GameGrid. | Prepara il calcolo da una sessione reale.                    |
| Test      | Verifica della ricostruzione completa della griglia.          | Controlla dimensioni, terreno, occupazione, muri e trappole. |

---

## [0.9.0] - 2026-07-05

Autore: Andrea Perini
Ambito: Persistenza sessione e plancia

## Added

| Area       | Elemento                                       | Motivo                                                          |
| :--------- | :--------------------------------------------- | :-------------------------------------------------------------- |
| Database   | Tabelle per sessioni, celle, muri e trappole.  | Salva la configurazione della plancia associata a una sessione. |
| Backend    | Modelli Java per sessione e stato griglia.     | Separa stato persistito e griglia di movimento.                 |
| Repository | Lettura e scrittura JDBC dello stato sessione. | Collega database e calcolo delle celle raggiungibili.           |
| Test       | Test repository sessione e plancia.            | Copre salvataggio e lettura dello stato.                        |

---

## [0.8.0] - 2026-07-05

Autore: Andrea Perini
Ambito: API REST movimento

## Added

| Area     | Elemento                                                         | Motivo                                                      |
| :------- | :--------------------------------------------------------------- | :---------------------------------------------------------- |
| REST API | POST /api/v1/movement/reachable-cells.                           | Rende invocabile dall'esterno il calcolo del movimento.     |
| Request  | Griglia con dimensioni, terreno, celle, muri e trappole.         | Permette di testare il movimento senza sessioni persistite. |
| Response | Celle raggiungibili con costo, percorso e trappole attraversate. | Fornisce dati utilizzabili da app, dashboard e plancia.     |

---

## [0.7.0] - 2026-07-05

Autore: Andrea Perini
Ambito: Calcolo movimento su griglia

## Added

| Area      | Elemento                                                   | Motivo                                                     |
| :-------- | :--------------------------------------------------------- | :--------------------------------------------------------- |
| Movimento | MovementService con Dijkstra semplificato.                 | Calcola le celle raggiungibili in base ai punti movimento. |
| Percorso  | Ricostruzione del tragitto verso ogni cella raggiungibile. | Permette di sapere da quali caselle passa il personaggio.  |
| Trappole  | Rilevamento delle trappole armate lungo il percorso.       | Permette di gestire trappole attraversate.                 |
| Diagonali | Movimento in 8 direzioni con regola anti-taglio angolo.    | Evita il passaggio attraverso muri o angoli bloccati.      |

---

## [0.6.0] - 2026-07-04

Autore: Andrea Perini
Ambito: Modello logico della griglia D&D

## Added

| Area             | Elemento                                             | Motivo                                                  |
| :--------------- | :--------------------------------------------------- | :------------------------------------------------------ |
| Griglia          | Posizioni convertibili da coordinate tipo A3 o B12.  | Rappresenta le caselle in modo stabile.                 |
| Terreno          | Tipi NORMAL, DIFFICULT, OBSTACLE e BLOCKED.          | Gestisce costi diversi e celle non attraversabili.      |
| Celle            | Stato di posizione, terreno e occupazione.           | Determina attraversabilita e costo.                     |
| Muri             | Segmenti sui bordi tra celle adiacenti.              | Blocca il passaggio senza trasformare le celle in muri. |
| Trappole         | Stati HIDDEN, REVEALED e ALWAYS_HIDDEN.              | Gestisce trappole e visibilita ai giocatori.            |
| Griglia di gioco | Modello rettangolare con celle e muri configurabili. | Fornisce la base all'algoritmo di movimento.            |
| Movimento        | Richiesta e risultato delle celle raggiungibili.     | Prepara l'esposizione del calcolo via API.              |

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

| Area          | File                                   | Motivo                                     |
| :------------ | :------------------------------------- | :----------------------------------------- |
| REST API      | GameEventController.java               | Espone gli eventi salvati di una sessione. |
| Repository    | GameEventRepository.java               | Legge gli eventi ordinati per sessionId.   |
| Test          | GameEventControllerTest.java           | Verifica la risposta JSON dell'endpoint.   |
| Contratto API | docs/openapi/event-service.openapi.yml | Definisce l'endpoint REST in OpenAPI.      |

## Changed

- L'API di lettura eventi diventa il primo punto di integrazione concreto per dashboard e frontend.

---

## [0.3.0] - 2026-07-02

Autore: Andrea Perini
Ambito: Persistenza eventi di gioco

## Added

| Area           | File                         | Motivo                                                |
| :------------- | :--------------------------- | :---------------------------------------------------- |
| Database       | docker/postgres/init.sql     | Crea game_schema.game_events e l'indice per sessione. |
| Backend        | GameEventRepository.java     | Salva gli eventi MQTT ricevuti.                       |
| Test           | GameEventRepositoryTest.java | Verifica inserimento e duplicati tramite eventId.     |
| Configurazione | application.yml              | Configura la connessione PostgreSQL.                  |

## Changed

- MqttEventSubscriber persiste gli eventi dopo il parsing.
- pom.xml include JDBC, driver PostgreSQL e H2 per i test.

---

## [0.2.0] - 2026-07-02

Autore: Andrea Perini
Ambito: Primo microservizio Java/Spring Boot per ricezione MQTT

## Added

| Area           | File                                 | Motivo                                                     |
| :------------- | :----------------------------------- | :--------------------------------------------------------- |
| Backend        | services/event-service/pom.xml       | Definisce microservizio e dipendenze MQTT, Jackson e test. |
| Backend        | EventServiceApplication.java         | Avvia il microservizio.                                    |
| Configurazione | application.yml                      | Configura porta HTTP 8082, Actuator e topic MQTT.          |
| MQTT           | MqttEventSubscriber.java             | Riceve gli eventi dal broker.                              |
| Parsing        | GameEventParser.java, GameEvent.java | Trasforma JSON MQTT in oggetti Java tipizzati.             |
| Test           | GameEventParserTest.java             | Verifica il parsing di un evento MOVE.                     |

## Changed

- Aggiornato .gitignore per escludere le cartelle Maven target/.

---

## [0.1.0] - 2026-07-01

Autore: Andrea Perini
Ambito: Infrastruttura Docker, contratti REST/MQTT e simulatore MQTT

## Added

| Area           | Elemento                            | Motivo                                       |
| :------------- | :---------------------------------- | :------------------------------------------- |
| Infrastruttura | docker/docker-compose.yml           | Avvia PostgreSQL e Mosquitto con un comando. |
| Database       | docker/postgres/init.sql            | Crea gli schemi iniziali del progetto.       |
| MQTT           | docker/mosquitto/mosquitto.conf     | Configura Mosquitto per i test locali.       |
| Contratti      | Topic, payload JSON e API previste. | Definisce la comunicazione tra i componenti. |
| Simulatore     | simulator/publish_event.py          | Pubblica una mini-sessione D&D su MQTT.      |
| Subscriber     | simulator/subscribe_events.py       | Riceve e stampa gli eventi MQTT.             |
| Dipendenze     | simulator/requirements.txt          | Definisce paho-mqtt.                         |

## Changed

- Separata la struttura iniziale in docker/, docs/ e simulator/.
- Adottato il topic MQTT versionato boardhub/v1/venues/{venueId}/tables/{tableId}/events.
- Aggiornato .gitignore per ambienti virtuali, cache Python, file IDE, .env e file macOS.
