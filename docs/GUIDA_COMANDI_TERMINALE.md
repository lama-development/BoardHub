# Guida completa ai comandi di BoardHub

Questa guida spiega come avviare, usare e verificare BoardHub dalla radice del
repository. E pensata sia per chi sviluppa il backend sia per chi deve collegare
il frontend alle API.

Tutti i comandi `just` devono essere eseguiti da:

```text
BoardHub/
```

Per vedere l'elenco sintetico disponibile nella versione corrente:

```bash
just help
```

Scrivere soltanto `just` esegue la ricetta `default`, che mostra la stessa
guida sintetica:

```bash
just
just default
```

Le due forme sono equivalenti.

## 1. Regole essenziali

1. Avviare Docker Desktop prima di usare `just up`.
2. Non avviare due volte lo stesso servizio sulla stessa porta.
3. I comandi `backend`, `frontend`, `stats` ed `edge` restano in esecuzione:
   vanno lasciati aperti nel proprio terminale e si fermano con `Ctrl+C`.
4. I token DM e giocatore sono credenziali. Non inserirli in Git, screenshot
   pubblici o messaggi condivisi.
5. Gli identificativi restituiti dai comandi, come `REQUEST_ID`,
   `CHARACTER_ID`, `PIECE_ID` e `RESOLUTION_ID`, devono essere copiati nel
   comando successivo. Non scrivere letteralmente i nomi dei segnaposto.
6. La finestra impostata da `enable-table` permette di **avviare** una
   sessione entro un certo numero di minuti. Non e la durata massima della
   partita.

## 2. Terminali consigliati

Per una prova completa conviene usare cinque schede del terminale.

| Scheda | Contenuto | Comando principale |
| --- | --- | --- |
| 1 | Controllo e comandi brevi | `just ...` |
| 2 | Event-service | `just backend` |
| 3 | Frontend web | `just frontend` |
| 4 | Stats-service | `just stats` |
| 5 | Edge gateway, se necessario | `just edge` |

Le schede 4 e 5 servono solo quando si provano statistiche, sensori e modalita
offline. Per il flusso web di base bastano le prime tre.

## 3. Indirizzi locali

| Componente | Indirizzo | Cosa mostra |
| --- | --- | --- |
| Frontend | `http://localhost:5173` | Monitor e interfacce web |
| Tavolo 1 | `http://localhost:5173/t/qr-table-01` | Accesso pubblico al tavolo 1 |
| Tavolo 8 | `http://localhost:5173/t/qr-table-08` | Accesso pubblico al tavolo 8 |
| Event-service | `http://localhost:8082` | API principali |
| Salute event-service | `http://localhost:8082/actuator/health` | Stato tecnico del backend |
| Stats-service | `http://localhost:8083` | API statistiche |

Da un telefono sulla stessa rete del Mac bisogna sostituire `localhost` con
l'indirizzo IP locale del Mac, per esempio `192.168.1.4`.

## 4. Parametri e identificativi

| Parametro | Significato | Esempio |
| --- | --- | --- |
| `table_number` | Numero fisico del tavolo, da 1 a 8 | `8` |
| `minutes` | Minuti entro cui si puo creare la sessione | `15` |
| `session_id` | Identificativo univoco scelto per la sessione | `session-demo-001` |
| `table_id` | Identificativo interno del tavolo | `table-08` |
| `table_public_id` | Identificativo contenuto nell'URL/QR | `qr-table-08` |
| `table_display_name` | Nome leggibile del tavolo | `"Tavolo 8"` |
| `player_reference` | Identita stabile del browser o dispositivo | `player-device-01` |
| `display_name` | Nome mostrato al DM | `Andrea` |
| `request_id` | Identificativo della richiesta di ingresso | UUID restituito dall'API |
| `character_id` | Identificativo del personaggio | UUID restituito dall'API |
| `session_piece_id` | Identificativo della pedina nella sessione | UUID restituito dall'API |
| `start_cell` | Cella iniziale della pedina | `B2` |
| `destination` | Cella di destinazione | `D2` |
| `expected_version` | Versione corrente della pedina o risoluzione | `0`, poi `1`, `2`, ... |
| `command_id` | Chiave idempotente di un comando | UUID, normalmente omesso |
| `resolution_id` | Identificativo di una trappola in risoluzione | UUID restituito dall'API |
| `event_id` | Identificativo univoco di un evento MQTT | normalmente omesso |
| `tournament_id` | Identificativo di un torneo | UUID restituito dall'API |
| `samples` | Numero di campioni della misura | `10` |
| `public_host` | Host/IP da inserire nel QR | `192.168.1.4` |

`expected_version` implementa il controllo di concorrenza ottimistico. Se due
dispositivi provano a modificare la stessa pedina partendo dalla stessa
versione, solo il primo comando valido viene accettato. Il secondo deve
ricaricare lo stato e usare la nuova versione.

`command_id` rende una richiesta ripetibile senza duplicare l'operazione. Lo
stesso valore va riutilizzato solo per ritentare la medesima operazione dopo un
errore di rete. Una nuova azione deve avere un nuovo `command_id`.

## 5. Variabili d'ambiente e credenziali

### Token DM

Viene restituito quando si crea una sessione. Autorizza le operazioni del
Dungeon Master nella scheda corrente del terminale.

```bash
export BOARDHUB_DM_TOKEN='bhd1...'
```

### Token giocatore

Viene restituito quando il DM accetta una richiesta. Autorizza personaggi,
pedine, movimenti ed eventi del giocatore.

```bash
export BOARDHUB_PLAYER_TOKEN='bhp1...'
```

Non aggiungere spazi prima o dopo `=`. Le virgolette devono racchiudere solo il
token. Un `export` vale nella scheda del terminale in cui viene eseguito.

### Altre variabili utili

```bash
export BOARDHUB_VENUE_ADMIN_KEY='boardhub-local-venue-admin-key'
export BOARDHUB_OUTPUT=json
export BOARDHUB_CHECK_TABLE=8
export BOARDHUB_MAX_ACTIVE_TABLES=8
```

| Variabile | Uso |
| --- | --- |
| `BOARDHUB_VENUE_ADMIN_KEY` | Credenziale amministrativa del locale |
| `BOARDHUB_OUTPUT=json` | Mostra il JSON originale invece delle tabelle leggibili |
| `BOARDHUB_CHECK_TABLE` | Tavolo usato da `just check` |
| `BOARDHUB_MAX_ACTIVE_TABLES` | Numero massimo configurato di tavoli |

## 6. Infrastruttura e servizi

### `just up`

```bash
just up
```

Avvia PostgreSQL e Mosquitto tramite Docker. Non avvia Java, il frontend, le
statistiche o il processo edge. Al primo avvio puo impiegare alcuni secondi per
creare rete, container e volumi.

### `just down`

```bash
just down
```

Ferma PostgreSQL e Mosquitto. I volumi e i dati persistenti restano presenti.
Non sostituisce la chiusura logica delle sessioni con `close-session`.

### `just ps`

```bash
just ps
```

Mostra lo stato dei container Docker di BoardHub.

### `just db-tables`

```bash
just db-tables
```

Mostra le tabelle presenti negli schemi PostgreSQL di BoardHub. Richiede il
container database attivo.

### `just backend`

```bash
just backend
```

Avvia l'event-service Spring Boot sulla porta `8082`. Lasciare il terminale
aperto. Flyway controlla e applica le migrazioni del database durante l'avvio.

### `just frontend`

```bash
just frontend
```

Avvia Vite sulla porta `5173` e rende il sito raggiungibile anche dagli altri
dispositivi della rete locale. Lasciare il terminale aperto.

### `just stats`

```bash
just stats
```

Avvia lo stats-service sulla porta `8083`. Il servizio riceve i risultati delle
sessioni concluse e costruisce statistiche e classifiche.

### `just edge`

```bash
just edge
```

Avvia il gateway edge. Legge osservazioni dei sensori, pubblica su MQTT e usa
una outbox SQLite locale quando il broker non e disponibile.

### `just close-all`

```bash
just close-all
```

**Attenzione:** chiude tutte le sessioni attive e revoca tutte le abilitazioni
dei tavoli. Usarlo solo per ripulire un ambiente locale di prova.

## 7. Controlli e test automatici

### `just health`

```bash
just health
```

Controlla che l'event-service risponda sulla porta `8082`.

### `just test`

```bash
just test
```

Esegue tutti i test Maven dell'event-service. Non richiede che il frontend sia
aperto. Il risultato corretto termina con `BUILD SUCCESS` e zero fallimenti.

### `just stats-test`

```bash
just stats-test
```

Esegue i test automatici dello stats-service.

### `just edge-setup`

```bash
just edge-setup
```

Crea l'ambiente virtuale Python sotto `edge/.venv` e installa le dipendenze
del gateway. Va eseguito al primo utilizzo o dopo una modifica alle dipendenze.

### `just edge-test`

```bash
just edge-test
```

Esegue i test Python di outbox, configurazione, runner e misurazioni edge.

### `just check`

```bash
BOARDHUB_CHECK_TABLE=8 just check
```

Esegue una prova integrata: health check, sessione temporanea, calcolo del
movimento, pubblicazione MQTT, verifica della persistenza e chiusura della
sessione. Richiede `just up` e `just backend` gia attivi. Il tavolo selezionato
deve essere libero.

### `just move-stateless`

```bash
just move-stateless
```

Invia al backend una griglia dimostrativa completa nel corpo della richiesta e
mostra le celle raggiungibili. Non salva una sessione.

### `just move-session`

```bash
just move-session session-demo-001
```

Calcola il movimento dimostrativo del personaggio `adv-01` nella griglia della
sessione indicata. Il parametro e il `session_id` di una sessione esistente.

## 8. Gestione del locale, dei tavoli e del QR

### `just venue-tables`

```bash
just venue-tables
```

Mostra tutti i tavoli e i relativi stati:

- `DISABLED`: non abilitato dal locale;
- `READY`: finestra di creazione aperta;
- `IN_SESSION`: sessione attiva.

### `just enable-table`

```bash
just enable-table 8 15
```

Abilita il tavolo 8 per 15 minuti. Il primo parametro e `table_number`; il
secondo e `minutes`. Entro quella finestra un utente puo creare la sessione e
diventare DM.

### `just disable-table`

```bash
just disable-table 8
```

Revoca la finestra di avvio di un tavolo non occupato. Il parametro e
`table_number`.

### `just venue-close-table`

```bash
just venue-close-table 8
```

Permette al personale del locale di chiudere la sessione attiva sul tavolo
indicato, anche senza usare il token del DM.

### `just qr`

```bash
just qr 8
just qr 8 192.168.1.4
```

Stampa nel terminale il QR del tavolo 8. Il secondo parametro opzionale e
l'host/IP raggiungibile dal telefono. Il QR identifica sempre il tavolo, non
una singola sessione.

### `just table-status`

```bash
just table-status qr-table-08
```

Mostra lo stato pubblico del tavolo identificato da `table_public_id`, anche
quando non ha una sessione attiva.

### `just table-session`

```bash
just table-session qr-table-08
```

Restituisce la sessione pubblica attiva del tavolo. Un `404` e corretto quando
non esiste alcuna sessione attiva.

## 9. Sessioni, richieste e partecipanti

### `just create-session`

```bash
just create-session session-demo-001 table-08 qr-table-08 "Tavolo 8"
```

Crea una sessione dimostrativa 3 x 3 e restituisce il token DM.

Parametri, nell'ordine:

1. `session_id`: identificativo univoco della nuova sessione;
2. `table_id`: identificativo interno;
3. `table_public_id`: identificativo usato dal QR;
4. `table_display_name`: nome leggibile, tra virgolette se contiene spazi.

Il tavolo deve essere stato prima abilitato dal locale.

### `just create-trap-session`

```bash
just create-trap-session session-trap-001 table-08 qr-table-08 "Tavolo 8"
```

Crea una demo 4 x 3 con una trappola configurata sul percorso. I parametri sono
gli stessi di `create-session`. Serve per provare il flusso autorevole delle
trappole.

### `just request-join`

```bash
just request-join session-demo-001 player-device-01 Andrea
```

Invia una richiesta di partecipazione.

1. `session_id`: sessione a cui entrare;
2. `player_reference`: identita stabile del dispositivo;
3. `display_name`: nome mostrato al DM.

La risposta contiene il `request_id`. Non inviare ripetutamente la stessa
richiesta se e gia `PENDING`.

### `just pending-joins`

```bash
just pending-joins session-demo-001
```

Mostra le richieste in attesa. Richiede `BOARDHUB_DM_TOKEN`.

### `just accept-join`

```bash
just accept-join session-demo-001 REQUEST_ID
```

Accetta la richiesta indicata e restituisce il token giocatore. Sostituire
`REQUEST_ID` con l'UUID mostrato da `pending-joins`.

### `just reject-join`

```bash
just reject-join session-demo-001 REQUEST_ID
```

Rifiuta la richiesta indicata. Richiede il token DM.

### `just participants`

```bash
just participants session-demo-001
```

Mostra DM e giocatori attivi nella sessione. Richiede il token DM.

### `just close-session`

```bash
just close-session session-demo-001
```

Conclude la sessione, disattiva i partecipanti, libera il tavolo e rende
disponibili i risultati alle statistiche. Richiede il token DM.

## 10. Personaggi e pedine

### `just create-character`

```bash
just create-character session-demo-001
```

Crea il personaggio dimostrativo Elaria per il giocatore autenticato. Al
momento questo comando non riceve nome, specie o classe da terminale: usa il
payload demo del `justfile`. Richiede `BOARDHUB_PLAYER_TOKEN`.

### `just my-characters`

```bash
just my-characters session-demo-001
```

Mostra i personaggi posseduti dal giocatore autenticato.

### `just dm-characters`

```bash
just dm-characters session-demo-001
```

Mostra al DM i personaggi della sessione nel rispetto delle regole di
visibilita. Richiede il token DM.

### `just create-piece`

```bash
just create-piece session-demo-001 CHARACTER_ID B2
```

Crea una pedina virtuale per il personaggio e la posiziona in `B2`.

1. `session_id`: sessione attiva;
2. `character_id`: UUID del personaggio;
3. `start_cell`: cella iniziale libera.

### `just my-pieces`

```bash
just my-pieces session-demo-001
```

Mostra le pedine controllate dal giocatore, la cella e la versione corrente.

### `just dm-pieces`

```bash
just dm-pieces session-demo-001
```

Mostra tutte le pedine visibili al DM.

## 11. Movimento autorevole

### `just piece-reachable`

```bash
just piece-reachable session-demo-001 PIECE_ID
```

Calcola le destinazioni raggiungibili dalla pedina del giocatore usando stato
persistito, punti movimento, muri, occupazioni e trappole conosciute.

### `just move-piece`

```bash
just move-piece session-demo-001 PIECE_ID C2 0
```

Richiede lo spostamento della pedina verso `C2`.

1. `session_id`;
2. `session_piece_id`;
3. `destination`;
4. `expected_version`, opzionale e inizialmente `0`;
5. `command_id`, opzionale.

Se il movimento e immediatamente valido, la risposta contiene la nuova cella
e versione. Se incontra una trappola nascosta, restituisce una risoluzione in
attesa invece di completare tutto il percorso.

### `just dm-piece-reachable`

```bash
just dm-piece-reachable session-demo-001 PIECE_ID
```

Calcola le destinazioni quando la pedina e sotto controllo del DM.

### `just dm-move-piece`

```bash
just dm-move-piece session-demo-001 PIECE_ID C2 0
```

Muove una pedina di cui il DM ha assunto temporaneamente il controllo. I
parametri coincidono con `move-piece`.

## 12. Controllo temporaneo del DM

### `just assume-character`

```bash
just assume-character session-demo-001 CHARACTER_ID
```

Trasferisce temporaneamente al DM il controllo del personaggio, per esempio se
il telefono del giocatore non e disponibile.

### `just release-character`

```bash
just release-character session-demo-001 CHARACTER_ID
```

Restituisce il controllo al giocatore proprietario.

## 13. Risoluzione delle trappole

### `just pending-traps`

```bash
just pending-traps session-trap-001
```

Mostra le risoluzioni ancora aperte e i relativi `resolution_id` e versione.
Richiede il token DM.

### `just trap-status`

```bash
just trap-status session-trap-001 RESOLUTION_ID
```

Mostra lo stato di una specifica risoluzione dal punto di vista del giocatore.

### `just roll-trap`

```bash
just roll-trap session-trap-001 RESOLUTION_ID 0
```

Esegue lato server il tiro salvezza del giocatore.

1. `session_id`;
2. `resolution_id`;
3. `expected_version` della risoluzione;
4. `command_id`, opzionale.

Il valore `VERSIONE` mostrato nelle guide e un segnaposto: va sostituito con un
numero reale, per esempio `0`.

### `just continue-trap`

```bash
just continue-trap session-trap-001 RESOLUTION_ID 1
```

Conferma la prosecuzione o la conclusione del movimento dopo il tiro. Usare la
nuova versione restituita da `roll-trap`.

### `just dm-roll-trap`

```bash
just dm-roll-trap session-trap-001 RESOLUTION_ID 0
```

Permette al DM di eseguire il tiro quando controlla il personaggio.

### `just dm-continue-trap`

```bash
just dm-continue-trap session-trap-001 RESOLUTION_ID 1
```

Permette al DM di completare la risoluzione e il movimento residuo.

## 14. Eventi e MQTT

### `just publish-event`

```bash
just publish-event session-demo-001
just publish-event session-demo-001 evt-prova-001 table-08
```

Pubblica su Mosquitto un evento `MOVE` dimostrativo da `A1` a `B1`.

1. `session_id`;
2. `event_id`, opzionale e generato se omesso;
3. `table_id`, opzionale con default `table-04`.

Il `table_id` deve coincidere con il tavolo della sessione, altrimenti il
backend non considera il messaggio parte di quella sessione.

### `just events`

```bash
just events session-demo-001
```

Legge gli eventi persistiti della sessione tramite l'endpoint generale.

### `just player-events`

```bash
just player-events session-demo-001
```

Mostra solo gli eventi che il giocatore autenticato puo conoscere, senza
informazioni segrete del DM.

### `just dm-events`

```bash
just dm-events session-demo-001
```

Mostra al DM lo stream completo, inclusi dettagli autorevoli e trappole.

## 15. Edge, coda offline e misure

### `just edge-observe`

```bash
just edge-observe session-demo-001 adv-01
```

Inserisce un'osservazione sensore dimostrativa per il gateway.

1. `session_id`: sessione a cui appartiene l'osservazione;
2. `character_id`: personaggio rilevato, default `adv-01`.

### `just edge-queue`

```bash
just edge-queue
```

Mostra gli eventi ancora presenti nell'outbox SQLite locale.

### `just edge-status`

```bash
just edge-status
```

Mostra configurazione e stato operativo del gateway edge.

### `just check-offline`

```bash
just check-offline session-offline-demo-001
```

Simula l'assenza del broker: ferma Mosquitto, accoda osservazioni in SQLite,
riavvia il broker, esegue il replay e controlla la persistenza. Il parametro e
il `session_id` di una sessione preparata per la prova. Usarlo solo in ambiente
locale, perche interrompe temporaneamente MQTT.

### `just measure`

```bash
just measure 10 7
```

Misura in modo riproducibile:

1. latenza online fino alla persistenza PostgreSQL;
2. accodamento offline;
3. recupero dopo il riavvio del broker;
4. acknowledgement applicativo.

Il primo parametro e il numero di campioni utili; il secondo e il numero di un
tavolo `DISABLED`. Il comando include un warm-up, ferma e riavvia Mosquitto e
produce:

```text
artifacts/measurements/<timestamp>/measurements.csv
artifacts/measurements/<timestamp>/report.md
```

Gli artefatti sono locali e ignorati da Git. Non eseguire `measure` durante
una partita reale o mentre un collaboratore sta usando il broker.

## 16. Statistiche e tornei

### `just stats-health`

```bash
just stats-health
```

Controlla che lo stats-service risponda. Richiede `just stats` attivo.

### `just stats-sessions`

```bash
just stats-sessions
```

Elenca le sessioni concluse e acquisite dal servizio statistiche.

### `just stats-session`

```bash
just stats-session session-demo-001
```

Mostra risultato, durata, giocatori, personaggi, mosse, tiri salvezza e punti
della sessione indicata.

### `just stats-player`

```bash
just stats-player player-device-01
```

Mostra lo storico aggregato del `player_reference`: partite, sopravvivenza,
punti, celle percorse, tiri riusciti e danni.

### `just create-tournament`

```bash
just create-tournament "Coppa del Locale" DND venue-01
```

Crea un torneo.

1. `name`: nome, tra virgolette se contiene spazi;
2. `game_type`: tipo di gioco, default `DND`;
3. `venue_id`: identificativo del locale, default `venue-01`.

### `just tournaments`

```bash
just tournaments
```

Elenca i tornei presenti nello stats-service.

### `just leaderboard`

```bash
just leaderboard TOURNAMENT_ID
```

Mostra la classifica del torneo indicato. Sostituire `TOURNAMENT_ID` con
l'identificativo restituito da `create-tournament` o `tournaments`.

## 17. Flusso completo da terminale e browser

### Fase A: avvio

Scheda 1:

```bash
just up
```

Scheda 2, da lasciare aperta:

```bash
just backend
```

Scheda 3, da lasciare aperta:

```bash
just frontend
```

Scheda 1:

```bash
just health
just venue-tables
```

### Fase B: abilita il tavolo e apri il QR

Scheda 1:

```bash
just enable-table 8 15
just qr 8
```

Nel browser aprire:

```text
http://localhost:5173/t/qr-table-08
```

La pagina deve mostrare il tavolo pronto. Il primo browser che crea la
sessione diventa il dispositivo DM e conserva localmente la propria sessione.

In alternativa, per la prova solo da terminale:

```bash
just create-session session-demo-001 table-08 qr-table-08 "Tavolo 8"
export BOARDHUB_DM_TOKEN='bhd1...'
```

### Fase C: ingresso del giocatore

Da un altro browser o telefono aprire lo stesso URL, inserire il nome e
inviare la richiesta. La console DM deve mostrare la richiesta e consentire di
accettarla o rifiutarla.

Equivalente da terminale:

```bash
just request-join session-demo-001 player-device-01 Andrea
just pending-joins session-demo-001
just accept-join session-demo-001 REQUEST_ID
export BOARDHUB_PLAYER_TOKEN='bhp1...'
just participants session-demo-001
```

### Fase D: personaggio, pedina e movimento

```bash
just create-character session-demo-001
just my-characters session-demo-001
just create-piece session-demo-001 CHARACTER_ID B2
just my-pieces session-demo-001
just piece-reachable session-demo-001 PIECE_ID
just move-piece session-demo-001 PIECE_ID C2 0
just my-pieces session-demo-001
```

Nel pannello giocatore devono comparire personaggio e pedina. Nel monitor della
sessione la pedina deve occupare la nuova cella dopo l'aggiornamento.

### Fase E: trappola

Per questa prova creare dall'inizio una `create-trap-session`. Dopo aver
creato giocatore, personaggio e pedina, muovere la pedina lungo il percorso che
attraversa la trappola e poi usare:

```bash
just pending-traps session-trap-001
just roll-trap session-trap-001 RESOLUTION_ID 0
just continue-trap session-trap-001 RESOLUTION_ID 1
just player-events session-trap-001
just dm-events session-trap-001
```

La versione da usare e sempre quella mostrata dalla risposta precedente.

### Fase F: chiusura e statistiche

Scheda 4, da lasciare aperta prima della chiusura:

```bash
just stats
```

Scheda 1:

```bash
just close-session session-demo-001
just stats-session session-demo-001
just stats-player player-device-01
just stats-sessions
just venue-tables
```

La sessione deve risultare `ENDED`, il tavolo `DISABLED` e il risultato deve
comparire nelle statistiche.

### Fase G: arresto

Premere `Ctrl+C` nelle schede di frontend, backend, stats ed edge. Poi:

```bash
just down
```

## 18. Risoluzione dei problemi comuni

### Docker non raggiungibile

Errore tipico:

```text
failed to connect to the docker API
```

Aprire Docker Desktop, attendere che il motore sia pronto e ripetere
`just up`.

### Porta gia occupata

Se Vite usa `5174` invece di `5173`, esiste gia un frontend sulla porta
standard. Fermare il processo vecchio oppure usare l'indirizzo stampato dal
nuovo processo. Per una prova coerente e preferibile mantenere un solo
frontend.

### Token assente o non valido

Ripetere l'`export` nella stessa scheda in cui si esegue il comando protetto.
Non usare un `request_id` al posto di un token.

### Versione non valida

Rileggere la pedina con `my-pieces` o la risoluzione con `trap-status`, quindi
usare il numero corrente come `expected_version`.

### Richiesta duplicata

Controllare `pending-joins` prima di inviare una nuova richiesta con lo stesso
`player_reference`.

### Evento MQTT non persistito

Controllare che Mosquitto e backend siano attivi e che il `table_id` del topic
coincida con quello della sessione.

### Statistiche mancanti

Lo stats-service deve essere attivo e la sessione deve essere conclusa. Le
statistiche non rappresentano una sessione ancora in corso.

## 19. Confine attuale del progetto

Questi comandi coprono il prototipo locale web, il backend, l'edge gateway e
le statistiche. L'app Android con NFC non e ancora parte del flusso eseguibile:
quando verra introdotta usera gli stessi identificativi e contratti API, ma
avra una guida separata per build, installazione e associazione NFC.
