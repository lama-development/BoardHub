## Piattaforma IoT per locali ludici con tavoli D&D connessi

**Sottotitolo:** Connected Games Platform for Smart Venues

**Tipologia:** progetto software distribuito con edge computing, MQTT, microservizi REST e interfaccia web

---

## Abstract

BoardHub e’ una piattaforma software pensata per locali ludici, ludoteche, sale gioco e spazi privati in cui si svolgono sessioni di gioco da tavolo e giochi di ruolo. Il progetto propone un caso d’uso specifico: un tavolo D&D/fantasy connesso, in cui una sessione fisica con avventurieri, mostri, mappa, ostacoli, round e combattimenti genera eventi digitali tramite sensori reali o simulatori software.

Gli eventi prodotti dal tavolo vengono raccolti da un componente edge installato nel locale. L’edge valida gli eventi, mantiene lo stato locale della sessione e comunica con una piattaforma centrale tramite un broker MQTT. Il backend centrale, organizzato secondo un’architettura a microservizi, registra eventi, sessioni, prenotazioni, statistiche e stato dei tavoli, esponendo API REST verso una web app utilizzata da giocatori e amministratori del locale.

L’obiettivo del progetto non e’ realizzare solamente un sito di prenotazione, ma dimostrare una piattaforma completa coerente con la traccia PISSIR: acquisizione dati da giochi fisici, componente edge, comunicazione tramite message broker, funzionamento offline, sincronizzazione, microservizi, interfaccia utente, test e demo end-to-end.

---

## 1. Contesto e motivazione

I giochi da tavolo e i giochi di ruolo sono attivita’ tradizionalmente fisiche. Le interazioni avvengono attorno a un tavolo, con mappe, pedine, miniature, dadi, personaggi e regole condivise. Questa natura fisica e’ parte del valore dell’esperienza, ma limita la raccolta automatica di dati: durata delle sessioni, eventi principali, andamento del combattimento, statistiche dei giocatori e utilizzo dei tavoli vengono normalmente gestiti a mano.

BoardHub nasce come piattaforma per collegare questo mondo fisico a un sistema digitale. Il locale rimane il centro dell’esperienza, ma ogni tavolo puo’ diventare un punto di raccolta dati. Gli eventi di gioco vengono acquisiti, trasmessi, salvati e visualizzati senza eliminare la componente fisica del gioco.

### Problema affrontato

| Problema                                                                                     | Impatto                                                                            |
| -------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------- |
| Le sessioni fisiche non producono dati strutturati.                                          | Statistiche e storico devono essere annotati manualmente.                          |
| I locali non hanno una visione precisa dell’utilizzo dei tavoli.                             | Diventa difficile gestire prenotazioni, disponibilita’ e preferenze dei giocatori. |
| Le partite non sono facilmente monitorabili in tempo reale.                                  | Giocatori, master e gestori non hanno una dashboard condivisa.                     |
| In caso di rete instabile, un sistema centralizzato puro puo’ interrompere la raccolta dati. | Serve un edge locale capace di continuare a funzionare offline.                    |

### Obiettivo generale

Realizzare una piattaforma distribuita in cui un tavolo D&D fisico produce eventi digitali che vengono raccolti da un edge locale, trasmessi via MQTT, elaborati da microservizi e mostrati in una web app.

---

## 2. Titolo e identita’ del progetto

### Titolo proposto

**BoardHub**

### Sottotitolo

**Connected Games Platform for Smart Venues**

### Razionale del nome

| Aspetto        | Motivazione                                                                                 |
| -------------- | ------------------------------------------------------------------------------------------- |
| Board          | Richiama giochi da tavolo, mappe, plance e sessioni fisiche.                                |
| Hub            | Indica una piattaforma centrale che collega locali, tavoli, utenti, sessioni e statistiche. |
| Espandibilita’ | Il nome non limita il progetto al solo D&D: in futuro puo’ includere altri giochi fisici.   |
| Chiarezza      | Il nome e’ breve, leggibile e adatto a una presentazione universitaria.                     |

---

## 3. Descrizione dell’idea progettuale

BoardHub gestisce un locale ludico dotato di uno o piu’ tavoli prenotabili. Ogni tavolo puo’ ospitare una sessione di gioco. Nel caso d’uso principale, il tavolo ospita una sessione D&D/fantasy.

Durante la sessione, il tavolo fisico viene rappresentato come una griglia. Sulla griglia possono essere presenti:

- avventurieri;
- mostri;
- ostacoli;
- muri;
- stanze o aree della mappa;
- eventi di movimento;
- eventi di combattimento;
- eventi di fine round.

Questi eventi possono essere generati in due modi:

1. **Sensori reali**, ad esempio pulsanti, lettori NFC/RFID, sensori sotto la griglia o pedine taggate.
2. **Simulatori software**, usati nella fase di sviluppo e nella demo d’esame per evitare dipendenze hardware.

Il simulatore non e’ un ripiego, ma un componente utile per testare scenari realistici in modo ripetibile.

---

## 4. Allineamento con la traccia PISSIR

BoardHub rispetta la traccia del progetto PISSIR perche’ contiene tutti gli elementi richiesti: gioco fisico, locale, sensori o emulatori, edge, MQTT/message broker, server centrale, microservizi, API REST, statistiche, interfaccia utente, offline sync e demo.

| Richiesta PISSIR                   | Realizzazione in BoardHub                                              |
| ---------------------------------- | ---------------------------------------------------------------------- |
| Gioco fisico tradizionale connesso | Tavolo D&D/fantasy fisico con mappa e miniature.                       |
| Locale identificabile              | Locale ludico con tavoli prenotabili e giochi installati.              |
| Sensori reali o simulati           | Simulatore eventi D&D, con possibile estensione hardware.              |
| Edge nel locale                    | Processo locale che raccoglie eventi, valida dati e gestisce offline.  |
| Broker MQTT                        | Mosquitto per pubblicazione eventi e stato edge.                       |
| Server centrale                    | Backend a microservizi con API REST.                                   |
| Database                           | Persistenza di utenti, locali, tavoli, sessioni, eventi e statistiche. |
| Interfaccia utente                 | Web app per giocatori, master e amministratori.                        |
| Statistiche                        | Danni inflitti, mostri sconfitti, round, sessioni, utilizzo tavoli.    |
| Demo funzionante                   | Scenario end-to-end con eventi, offline queue e sync.                  |

---

## 5. Attori del sistema

| Attore                           | Descrizione                                | Funzioni principali                                                                       |
| -------------------------------- | ------------------------------------------ | ----------------------------------------------------------------------------------------- |
| Giocatore                        | Utente che partecipa alle sessioni.        | Prenota, partecipa, consulta statistiche e storico.                                       |
| Master                           | Utente che conduce una sessione D&D.       | Avvia sessione, gestisce eventi, controlla stato di mappa e mostri.                       |
| Amministratore del locale        | Gestore del locale fisico.                 | Gestisce tavoli, prenotazioni, disponibilita’, dispositivi edge e statistiche del locale. |
| Amministratore della piattaforma | Gestore globale del sistema.               | Gestisce utenti, locali, configurazioni, monitoraggio e statistiche aggregate.            |
| Edge locale                      | Componente software installato nel locale. | Raccoglie eventi, pubblica su MQTT, salva offline e sincronizza.                          |
| Backend centrale                 | Insieme dei microservizi.                  | Elabora eventi, persiste dati, espone API REST e calcola statistiche.                     |

---

## 6. Modello di dominio

### Entita’ principali

| Entita’      | Descrizione                                           | Attributi indicativi                                    |
| ------------ | ----------------------------------------------------- | ------------------------------------------------------- |
| Locale       | Luogo fisico in cui sono installati tavoli e giochi.  | id, nome, indirizzo, gestore, stato.                    |
| Tavolo       | Postazione fisica prenotabile.                        | id, locale, nome, stato, tipo gioco, edge associato.    |
| Prenotazione | Fascia oraria riservata da un utente o gruppo.        | id, utente, tavolo, data, ora inizio, ora fine, stato.  |
| Sessione     | Istanza di gioco avviata su un tavolo.                | id, tavolo, master, giocatori, stato, round corrente.   |
| Avventuriero | Personaggio controllato da un giocatore.              | id, sessione, nome, giocatore, punti ferita, posizione. |
| Mostro       | Entita’ ostile nella sessione.                        | id, sessione, tipo, punti ferita, posizione, stato.     |
| Evento       | Fatto rilevante generato dal tavolo o dal simulatore. | id, tipo, timestamp, payload, sorgente, stato sync.     |
| Statistica   | Dato aggregato calcolato dal backend.                 | id, utente/sessione/tavolo, metrica, valore, periodo.   |

### Relazioni principali

| Relazione                                             | Significato                                           |
| ----------------------------------------------------- | ----------------------------------------------------- |
| Un locale contiene molti tavoli.                      | Ogni tavolo e’ identificato nel contesto del locale.  |
| Un tavolo ospita molte prenotazioni nel tempo.        | Le prenotazioni evitano sovrapposizioni.              |
| Una prenotazione puo’ generare una sessione.          | La sessione e’ l’esecuzione reale della prenotazione. |
| Una sessione contiene avventurieri, mostri ed eventi. | Il dominio D&D viene tracciato tramite eventi.        |
| Gli eventi alimentano statistiche e stato live.       | Il backend aggrega dati e aggiorna la UI.             |

---

## 7. Casi d’uso principali

### UC1, Prenotare una sessione

| Campo             | Descrizione                                                                                                      |
| ----------------- | ---------------------------------------------------------------------------------------------------------------- |
| Attore primario   | Giocatore                                                                                                        |
| Precondizioni     | Il locale e il tavolo sono registrati; esiste una fascia oraria disponibile.                                     |
| Flusso principale | Il giocatore seleziona locale, tavolo, data e orario; il sistema verifica disponibilita’ e crea la prenotazione. |
| Output            | Prenotazione confermata e associata a un tavolo.                                                                 |

### UC2, Avviare una sessione D&D

| Campo             | Descrizione                                                                   |
| ----------------- | ----------------------------------------------------------------------------- |
| Attore primario   | Master o amministratore del locale                                            |
| Precondizioni     | Esiste una prenotazione valida o un tavolo disponibile.                       |
| Flusso principale | Il master avvia la sessione, seleziona giocatori e imposta la mappa iniziale. |
| Output            | Sessione attiva, edge pronto a ricevere/generare eventi.                      |

### UC3, Registrare un evento di gioco

| Campo             | Descrizione                                                                                       |
| ----------------- | ------------------------------------------------------------------------------------------------- |
| Attore primario   | Edge locale                                                                                       |
| Precondizioni     | La sessione e’ attiva.                                                                            |
| Flusso principale | Un sensore o simulatore genera un evento; l’edge lo valida e lo pubblica sul topic MQTT corretto. |
| Output            | Evento ricevuto dal backend e salvato nel database.                                               |

### UC4, Gestire offline e sincronizzazione

| Campo             | Descrizione                                                                                    |
| ----------------- | ---------------------------------------------------------------------------------------------- |
| Attore primario   | Edge locale                                                                                    |
| Precondizioni     | La connessione verso broker/backend non e’ disponibile.                                        |
| Flusso principale | L’edge salva eventi in una coda locale; alla riconnessione invia gli eventi non sincronizzati. |
| Output            | Nessuna perdita di dati; stato centrale riallineato.                                           |

### UC5, Consultare statistiche e storico

| Campo             | Descrizione                                                                                              |
| ----------------- | -------------------------------------------------------------------------------------------------------- |
| Attore primario   | Giocatore, master, amministratore del locale                                                             |
| Precondizioni     | Esistono eventi e sessioni salvate.                                                                      |
| Flusso principale | L’utente apre la web app e consulta sessioni, eventi, danni, mostri sconfitti, durata e utilizzo tavoli. |
| Output            | Visualizzazione di dati e statistiche coerenti con il ruolo dell’utente.                                 |

---

## 8. Eventi di gioco

Gli eventi costituiscono il nucleo del sistema. Ogni evento descrive una variazione significativa dello stato della sessione.

| Evento          | Descrizione                                | Esempio payload                                   |
| --------------- | ------------------------------------------ | ------------------------------------------------- |
| `SESSION_START` | Avvio della sessione.                      | `{ "sessionId": "s-102", "table": "tavolo-04" }`  |
| `MOVE`          | Movimento di un personaggio sulla griglia. | `{ "actor": "hero-1", "from": "D4", "to": "E4" }` |
| `SPAWN_MONSTER` | Comparsa di un mostro sulla mappa.         | `{ "monster": "goblin", "cell": "F6" }`           |
| `ATTACK`        | Azione di attacco.                         | `{ "actor": "hero-1", "target": "monster-2" }`    |
| `DAMAGE`        | Applicazione di danno a un bersaglio.      | `{ "target": "monster-2", "amount": 8 }`          |
| `OBSTACLE`      | Attivazione o rimozione di un ostacolo.    | `{ "cell": "C3", "active": true }`                |
| `WALL`          | Aggiornamento di un muro della mappa.      | `{ "cell": "D4", "side": "top", "active": true }` |
| `ROUND_END`     | Fine di un round.                          | `{ "round": 4 }`                                  |
| `SESSION_END`   | Chiusura della sessione.                   | `{ "sessionId": "s-102", "status": "completed" }` |

---

## 9. Architettura logica

```
Sensori reali / Simulatore software
        |
        v
Componente Edge del tavolo
        |
        v
Broker MQTT
        |
        v
Microservizi backend
        |
        v
Database centrale
        |
        v
Web app BoardHub
```

### Descrizione dei livelli

| Livello            | Responsabilita’                                                          |
| ------------------ | ------------------------------------------------------------------------ |
| Sensori/simulatore | Generano eventi della sessione D&D.                                      |
| Edge               | Valida eventi, mantiene stato locale, pubblica su MQTT, salva offline.   |
| MQTT broker        | Disaccoppia edge e backend tramite publish/subscribe.                    |
| Backend            | Elabora eventi, aggiorna sessioni, calcola statistiche, espone API REST. |
| Database           | Persistenza di dominio, eventi, prenotazioni e statistiche.              |
| Web app            | Interfaccia per gestione, visualizzazione live e consultazione storico.  |

---

## 10. Topic MQTT

La struttura dei topic deve essere gerarchica, leggibile e coerente con locale, tavolo e tipo di gioco.

| Topic                                                        | Direzione                 | Utilizzo                               |
| ------------------------------------------------------------ | ------------------------- | -------------------------------------- |
| `boardhub/v1/venues/{venueId}/tables/{tableId}/events`       | Edge -> Broker -> Backend | Eventi principali della sessione.      |
| `boardhub/v1/venues/{venueId}/tables/{tableId}/status`       | Edge -> Broker -> Backend | Stato edge, heartbeat, online/offline. |
| `boardhub/v1/venues/{venueId}/tables/{tableId}/commands`     | Backend -> Broker -> Edge | Comandi verso edge o display locale.   |
| `boardhub/v1/venues/{venueId}/tables/{tableId}/sync`         | Edge -> Backend           | Invio degli eventi accumulati offline. |

### Esempio concreto

```
boardhub/v1/venues/venue-01/tables/table-04/events
```

Payload:

```json
{
  "eventId": "evt-000042",
  "eventType": "DAMAGE",
  "venueId": "venue-01",
  "tableId": "table-04",
  "sessionId": "session-20260701-001",
  "source": "SIMULATOR",
  "occurredAt": "2026-07-01T16:30:00Z",
  "sequenceNumber": 42,
  "payload": {
    "targetId": "mon-01",
    "amount": 8,
    "remainingHitPoints": 4
  }
}
```

---

## 11. Microservizi proposti

| Servizio        | Responsabilita’ principali                                    |
| --------------- | ------------------------------------------------------------- |
| User Service    | Utenti, ruoli, profili, autorizzazioni di base.               |
| Venue Service   | Locali, tavoli, giochi installati e configurazioni edge.      |
| Booking Service | Prenotazioni, disponibilita’ dei tavoli e fasce orarie.       |
| Session Service | Sessioni D&D, stato live, round, avventurieri e mostri.       |
| Event Service   | Ricezione, validazione e persistenza degli eventi MQTT.       |
| Stats Service   | Calcolo di statistiche per utenti, tavoli, locali e sessioni. |

Per un MVP e’ possibile implementare meno processi fisici, ad esempio accorpando alcuni servizi. La relazione dovra’ comunque mostrare la decomposizione logica e motivare eventuali semplificazioni.

---

## 12. API REST indicative

Le API REST usano il prefisso versionato `/api/v1`, in coerenza con il documento dei contratti di comunicazione.

| Metodo | Endpoint                                  | Descrizione                    |
| ------ | ----------------------------------------- | ------------------------------ |
| `GET`  | `/api/v1/health`                          | Stato del backend.             |
| `GET`  | `/api/v1/venues`                          | Lista dei locali disponibili.  |
| `POST` | `/api/v1/venues`                          | Registrazione di un locale.    |
| `GET`  | `/api/v1/venues/{venueId}`                | Dettaglio di un locale.        |
| `GET`  | `/api/v1/venues/{venueId}/tables`         | Tavoli presenti in un locale.  |
| `POST` | `/api/v1/venues/{venueId}/tables`         | Registrazione di un tavolo.    |
| `POST` | `/api/v1/sessions`                        | Avvio sessione.                |
| `GET`  | `/api/v1/sessions/{sessionId}`            | Stato corrente della sessione. |
| `GET`  | `/api/v1/sessions/{sessionId}/events`     | Storico eventi sessione.       |
| `GET`  | `/api/v1/sessions/{sessionId}/stats`      | Statistiche della sessione.    |

Le prenotazioni restano una funzionalita gestionale estendibile, ma non sono il centro dell'MVP tecnico. Le API definitive dovrebbero essere documentate tramite OpenAPI/Swagger YAML.

---

## 13. Web app

La web app non e’ un elemento accessorio isolato, ma la superficie che rende comprensibile il sistema.

### Funzioni per giocatori

- visualizzazione dei locali;
- prenotazione di un tavolo;
- storico delle sessioni;
- statistiche personali;
- consultazione della sessione in corso.

### Funzioni per master

- avvio sessione;
- monitoraggio mappa;
- visualizzazione eventi live;
- controllo dello stato di avventurieri e mostri.

### Funzioni per amministratori del locale

- gestione tavoli;
- gestione disponibilita’;
- controllo stato edge;
- statistiche di utilizzo;
- consultazione delle sessioni concluse.

---

## 14. Funzionamento offline

Il funzionamento offline e’ una parte essenziale del progetto. Il tavolo fisico deve continuare a produrre eventi anche se la connessione verso la piattaforma centrale non e’ disponibile.

### Strategia proposta

| Fase                | Comportamento                                          |
| ------------------- | ------------------------------------------------------ |
| Connessione attiva  | L’edge pubblica gli eventi su MQTT.                    |
| Connessione assente | L’edge salva eventi in una coda locale.                |
| Riconnessione       | L’edge invia gli eventi pendenti al backend.           |
| Conferma sync       | Gli eventi sincronizzati vengono marcati come inviati. |

### Dati da conservare localmente

- identificativo sessione;
- timestamp evento;
- tipo evento;
- payload;
- stato sincronizzazione;
- tentativi di invio.

---

## 15. Scope del progetto

### MVP consigliato

| Area     | Scelta                                                                 |
| -------- | ---------------------------------------------------------------------- |
| Locale   | Un locale demo.                                                        |
| Tavolo   | Un tavolo D&D/fantasy connesso.                                        |
| Sensori  | Simulatore software di eventi.                                         |
| Edge     | Processo locale con buffer offline.                                    |
| Broker   | Mosquitto MQTT.                                                        |
| Backend  | Java/Spring Boot con API REST.                                         |
| Database | PostgreSQL lato server, SQLite o file lato edge.                       |
| Frontend | Web app responsive dimostrativa.                                       |
| Demo     | Sessione D&D con movimento, mostri, danni, statistiche e sync offline. |

### Estensioni future

| Estensione             | Priorita’ | Motivazione                                                  |
| ---------------------- | --------- | ------------------------------------------------------------ |
| Sensori hardware reali | Media     | Interessante, ma non necessaria per validare l’architettura. |
| Snack e drink          | Bassa     | Utile per il locale, ma non centrale per PISSIR.             |
| OAuth                  | Bassa     | Aggiunge complessita’ di sicurezza non indispensabile.       |
| Notifiche push         | Bassa     | Richiede infrastruttura aggiuntiva.                          |
| Leaderboard globale    | Media     | Coerente con statistiche, ma implementabile dopo l’MVP.      |
| Analytics avanzate     | Bassa     | Valore gestionale, ma non necessario per la demo tecnica.    |

---

## 16. Valutazione di fattibilita’

| Aspetto                | Valutazione | Nota                                                                    |
| ---------------------- | ----------- | ----------------------------------------------------------------------- |
| Coerenza con PISSIR    | Alta        | Include tutti i componenti richiesti dalla traccia.                     |
| Complessita’ tecnica   | Media       | La complessita’ e’ gestibile se sensori e hardware restano simulati.    |
| Valore dimostrativo    | Alto        | Il caso D&D rende chiaro il passaggio da evento fisico a dato digitale. |
| Rischio di scope creep | Alto        | Prenotazioni, shop, OAuth e analytics devono rimanere secondari.        |
| Realizzabilita’ MVP    | Buona       | Un simulatore edge + MQTT + backend REST + web app e’ fattibile.        |

### Rischi principali

| Rischio                          | Impatto                             | Mitigazione                                                 |
| -------------------------------- | ----------------------------------- | ----------------------------------------------------------- |
| Troppi microservizi fisici       | Aumento tempi e complessita’.       | Distinguere decomposizione logica e implementazione MVP.    |
| Hardware non disponibile         | Demo bloccata o instabile.          | Usare simulatore software come sorgente principale.         |
| UI troppo orientata al marketing | Perdita di coerenza con PISSIR.     | Presentare sempre pipeline e componenti tecnici.            |
| Offline sync sottovalutato       | Perdita di un requisito importante. | Implementare almeno una coda locale semplice.               |
| Regole D&D troppo complesse      | Dominio difficile da completare.    | Usare regole semplificate: movimento, danno, round, mostri. |

---

## 17. Scenario demo

La demo deve raccontare il sistema in modo lineare.

| Passo | Azione                                              | Componente dimostrato              |
| ----- | --------------------------------------------------- | ---------------------------------- |
| 1     | Un utente prenota il Tavolo 04.                     | Web app, prenotazioni, locale.     |
| 2     | Il master avvia la sessione D&D.                    | Session service, stato iniziale.   |
| 3     | Il simulatore genera un movimento.                  | Sensore/simulatore, evento `MOVE`. |
| 4     | Compare un mostro sulla mappa.                      | Evento `SPAWN_MONSTER`.            |
| 5     | L’avventuriero attacca e produce danno.             | Eventi `ATTACK` e `DAMAGE`.        |
| 6     | L’edge pubblica su MQTT.                            | Broker e topic gerarchici.         |
| 7     | Il backend aggiorna database e statistiche.         | Microservizi e persistenza.        |
| 8     | La web app aggiorna mappa, log e metriche.          | UI e stato live.                   |
| 9     | Si simula una perdita di rete.                      | Modalita’ offline.                 |
| 10    | Gli eventi vengono salvati in coda e sincronizzati. | Offline buffer e sync.             |

---

## 18. Validazione e test

### Test unitari

- validazione payload eventi;
- aggiornamento stato sessione;
- calcolo danni e statistiche;
- gestione prenotazioni;
- gestione coda offline.

### Test di integrazione

- simulatore -> edge -> MQTT;
- MQTT -> backend -> database;
- backend -> REST API -> web app;
- offline -> coda locale -> sync.

### Test demo

| Scenario               | Risultato atteso                                     |
| ---------------------- | ---------------------------------------------------- |
| Avvio sessione         | Stato sessione attivo e tavolo disponibile nella UI. |
| Movimento avventuriero | Evento registrato e posizione aggiornata.            |
| Danno a mostro         | Punti ferita e statistiche aggiornate.               |
| Rete offline           | Eventi salvati in coda locale.                       |
| Riconnessione          | Eventi sincronizzati senza perdita dati.             |

---

## 19. Deliverable consigliati

| Deliverable              | Contenuto                                                                    |
| ------------------------ | ---------------------------------------------------------------------------- |
| Relazione tecnica        | Specifiche, analisi tecnologica, architettura, implementazione, validazione. |
| Diagramma casi d’uso     | Prenotazione, avvio sessione, evento, statistiche, sync offline.             |
| Diagramma classi dominio | Locale, Tavolo, Sessione, Evento, Avventuriero, Mostro, Prenotazione.        |
| Diagrammi di sequenza    | Evento live e sincronizzazione offline.                                      |
| OpenAPI YAML             | REST API del backend.                                                        |
| Tabella MQTT             | Topic, direzioni, payload.                                                   |
| Codice demo              | Simulatore, edge, backend, web app.                                          |
| Test                     | Unitari e integrazione.                                                      |

---

## 20. Conclusione

BoardHub e’ una proposta progettuale coerente con le richieste PISSIR perche’ combina un dominio fisico riconoscibile, il tavolo D&D/fantasy, con un’architettura distribuita completa. Il progetto permette di mostrare in modo concreto come un evento generato da un gioco fisico possa attraversare sensori o simulatori, edge computing, MQTT, microservizi, database e interfaccia web.

La forza dell’idea e’ la sua doppia natura: da un lato e’ comprensibile e presentabile come servizio per un locale ludico, dall’altro contiene tutti gli elementi tecnici richiesti dal corso. Per mantenerla realizzabile, e’ fondamentale limitare lo scope al tavolo D&D smart, usare sensori emulati per la demo, implementare l’offline sync in forma semplice e trattare prenotazioni avanzate, shop, OAuth, notifiche e hardware reale come estensioni future.

La versione consigliata per l’esame e’ quindi un MVP solido: un locale demo, un tavolo D&D connesso, un simulatore di eventi, un edge locale con buffer offline, un broker MQTT, un backend REST a microservizi e una web app capace di mostrare stato, eventi e statistiche in tempo reale.
