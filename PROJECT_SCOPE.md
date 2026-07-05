# BoardHub - Project Scope

## 1. Visione del progetto

BoardHub e una piattaforma distribuita per sessioni fisiche di **Dungeons & Dragons** giocate su una plancia connessa.

Il progetto immagina un locale ludico con piu tavoli. Ogni tavolo puo ospitare una sessione D&D e puo essere dotato di una plancia fisica intelligente. La plancia comunica con un nodo edge locale, il nodo edge invia eventi tramite MQTT, il backend centrale salva e rende disponibili gli eventi tramite API REST, mentre giocatori e Dungeon Master usano un'app mobile o una dashboard.

L'obiettivo per PISSIR non e realizzare un gestionale commerciale del locale, ma dimostrare un sistema distribuito che collega un oggetto fisico a servizi di rete.

```text
plancia fisica / simulatore
-> nodo edge del tavolo
-> broker MQTT
-> backend Java/Spring Boot
-> PostgreSQL
-> API REST
-> app mobile / dashboard DM
```

## 2. Funzionamento nel locale

Nel locale sono presenti piu tavoli da gioco. Un tavolo BoardHub contiene:

- una griglia fisica per D&D;
- pedine o miniature dei personaggi;
- un possibile QR code o tag NFC per associare pedina e giocatore;
- un nodo edge collegato alla rete;
- una sessione D&D gestita da un Dungeon Master;
- una o piu app mobile collegate alla sessione;
- un set di dadi fisici o digitali registrabili dal sistema.

Il giocatore arriva al tavolo, prende una pedina e la associa al proprio dispositivo. L'associazione puo avvenire scansionando un QR code o leggendo un tag NFC. Da quel momento il sistema sa che quella pedina rappresenta il personaggio di quel giocatore.

Il Dungeon Master prepara la sessione: sceglie la mappa, imposta il combattimento, posiziona personaggi e creature, modifica il terreno, inserisce muri, ostacoli e trappole. La plancia fisica rappresenta la mappa. In una versione reale potrebbe illuminare caselle, mostrare aree raggiungibili e segnalare errori di movimento. Nella demo universitaria questa parte viene simulata.

## 2.1 Materiale e regole operative

Il progetto assume che il tavolo BoardHub fornisca un kit minimo per la sessione:

| Materiale | Descrizione | Uso nel sistema |
| :--- | :--- | :--- |
| Plancia a griglia | Campo fisico della sessione D&D. | Produce o simula eventi di posizione. |
| Pedine/miniature | Rappresentano personaggi e creature. | Vengono associate a `tokenId` e `characterId`. |
| QR code o NFC | Identificatore fisico della pedina o del tavolo. | Collega dispositivo, giocatore, personaggio e tavolo. |
| App mobile | Applicazione da installare sul dispositivo del giocatore. | Entra in sessione, configura personaggio, mostra azioni e log. |
| Vista DM | App o dashboard del Dungeon Master. | Configura mappa, terreno, creature, turni e round. |
| Dadi | Set fisico o digitale: `d4`, `d6`, `d8`, `d10`, `d%`, `d12`, `d20`. | I tiri digitali vengono salvati come eventi `DICE_ROLLED`. |

Le regole D&D gestite dall'MVP sono intenzionalmente limitate:

- il Dungeon Master controlla mappa, terreno, creature, trappole, turni e round;
- il giocatore controlla il proprio personaggio tramite app mobile;
- il movimento dipende dalla velocita del personaggio e dal costo delle caselle, non dal dado;
- il `d20` viene usato per prove e attacchi;
- gli altri dadi vengono usati per danni o effetti;
- il sistema registra gli eventi principali, non automatizza l'intero regolamento D&D.

## 3. Ruoli

| Ruolo | Descrizione | Funzioni principali |
| :--- | :--- | :--- |
| Giocatore | Persona che controlla un personaggio. | Associa pedina, gestisce scheda base, vede turno, azioni, dadi e log. |
| Dungeon Master | Gestisce sessione e combattimento. | Avvia encounter, posiziona mostri, muri, trappole, round e turni. |
| Nodo edge | Componente vicino al tavolo fisico. | Raccoglie eventi, valida dati locali, pubblica MQTT, gestisce buffer offline. |
| Backend centrale | Servizi lato server. | Riceve eventi, salva storico, espone API REST, ricostruisce stato. |
| App mobile | Interfaccia principale per giocatori e DM. | Mostra personaggio, azioni, dadi, movimento e registro eventi. |
| Dashboard opzionale | Vista pubblica o tecnica. | Mostra stato sessione, eventi e dati utili alla demo. |

## 4. Flusso di una sessione

1. Il Dungeon Master crea o avvia una sessione D&D su un tavolo.
2. I giocatori entrano nella sessione dall'app mobile.
3. Ogni giocatore associa una pedina al proprio personaggio tramite QR/NFC.
4. Il DM posiziona personaggi, mostri, muri e ostacoli sulla mappa.
5. Il sistema genera eventi iniziali di sessione e posizionamento.
6. Durante il turno, il giocatore sceglie se muoversi, attaccare o eseguire un'altra azione.
7. Se sceglie il movimento, il sistema calcola le caselle raggiungibili.
8. La plancia o l'app mostra le caselle valide.
9. Il giocatore sposta la pedina.
10. Il nodo edge rileva o riceve la nuova posizione.
11. Se serve un tiro, l'app registra il risultato del dado come evento.
12. Il backend salva gli eventi e li rende disponibili tramite API REST.

## 5. Movimento D&D su griglia

BoardHub si concentra su Dungeons & Dragons. Per l'MVP, il movimento viene espresso direttamente in caselle della plancia. Una configurazione standard puo assegnare a un personaggio 6 caselle di movimento per turno, considerando ogni casella come unita logica della mappa, circa 1,5 metri.

Il movimento non dipende dal tiro di dado. Dipende da:

- velocita del personaggio;
- posizione iniziale;
- muri sui bordi delle caselle;
- ostacoli o celle inaccessibili;
- trappole visibili o nascoste;
- caselle occupate;
- movimento ortogonale e diagonale;
- eventuali condizioni o bonus;
- regole semplificate scelte per l'MVP.

Per calcolare le caselle raggiungibili, l'MVP usera una versione semplificata dell'algoritmo di Dijkstra.

```text
input:
  posizione iniziale
  punti movimento disponibili
  caselle normali
  caselle di terreno difficile
  muri sui bordi tra celle
  ostacoli / celle inaccessibili / celle occupate
  trappole nascoste o rivelate

output:
  insieme delle caselle raggiungibili con costo totale <= punti movimento
```

La scelta di Dijkstra e motivata dal terreno difficile: non tutte le caselle hanno lo stesso costo. Una casella normale costa 1 punto movimento, una casella di terreno difficile costa 2 punti, mentre ostacoli, celle inaccessibili e celle occupate non sono attraversabili. I muri non sono terreno della cella: sono segmenti sui bordi e bloccano il passaggio tra due celle adiacenti.

Il movimento diagonale e supportato in forma semplificata. Una diagonale costa quanto la casella di arrivo, ma non puo tagliare un angolo: se una delle due celle laterali e non attraversabile o se un muro blocca uno dei passaggi laterali coinvolti, la diagonale viene rifiutata.

Le trappole non sono ostacoli: possono trovarsi su una casella attraversabile. Una trappola puo essere nascosta ai giocatori, rivelata dopo un tiro di percezione o attivata quando il percorso del personaggio attraversa la casella. Se il Dungeon Master la configura come sempre nascosta, il sistema puo registrare l'attivazione senza mostrarla preventivamente sulla plancia.

La complessita, usando una coda di priorita, e:

```text
O((V + E) log V)
```

dove `V` e il numero di caselle e `E` il numero di collegamenti tra caselle. Su una plancia D&D dimostrativa il costo resta molto basso.

## 6. Algoritmi del progetto

| Algoritmo | Stato | Dove viene usato | Perche viene usato |
| :--- | :--- | :--- | :--- |
| Ordinamento eventi per `sequenceNumber` | Implementato | `event-service`, lettura eventi sessione. | Ricostruisce l'ordine logico degli eventi salvati. |
| Dijkstra semplificato | Implementato | Movimento su griglia. | Gestisce costi del terreno, celle non attraversabili e muri tra celle adiacenti. |
| Parser dadi | Da implementare | Tiri digitali dall'app mobile. | Interpreta formule come `1d20+3` e registra risultati verificabili. |
| Verifica trappole sul percorso | Implementata nel calcolo movimento | Movimento su griglia. | Controlla se il percorso scelto attraversa caselle con trappole nascoste o rivelate. |
| Event replay | Da implementare | Ricostruzione stato partita. | Applica gli eventi in ordine per ricavare stato corrente. |
| Linea di vista | Estensione futura | Attacchi a distanza e magie. | Verifica se muri, ostacoli o celle bloccate interrompono la visibilita. |

### 6.1 Perche non BFS come scelta finale

La BFS e corretta solo se ogni passaggio tra caselle ha lo stesso costo. BoardHub deve invece considerare anche il terreno difficile, che consuma piu movimento. Per questo Dijkstra e piu adatto come algoritmo principale del movimento.

## 7. Validazione del movimento

Quando il giocatore prova a spostare una pedina, il sistema confronta la casella di arrivo con le caselle raggiungibili.

| Caso | Evento | Effetto |
| :--- | :--- | :--- |
| Movimento valido | `MOVE_CONFIRMED` | La posizione viene aggiornata. |
| Movimento troppo lungo | `MOVE_REJECTED` | Il sistema segnala che la casella non e raggiungibile. |
| Movimento bloccato da muro | `MOVE_REJECTED` | Il sistema segnala che il bordo tra due celle e chiuso. |
| Movimento su casella occupata | `MOVE_REJECTED` | Il sistema impedisce la sovrapposizione. |

Esempio:

```json
{
  "eventType": "MOVE_REJECTED",
  "payload": {
    "characterId": "adv-01",
    "from": "A3",
    "to": "A10",
    "reason": "OUT_OF_RANGE",
    "maxCells": 6
  }
}
```

## 8. Eventi principali

| Evento | Significato |
| :--- | :--- |
| `SESSION_START` | Avvio della sessione. |
| `PLAYER_JOINED` | Un giocatore entra nella sessione. |
| `TOKEN_ASSIGNED` | Una pedina viene associata a giocatore/personaggio. |
| `CHARACTER_CREATED` | Il giocatore configura il personaggio. |
| `ENCOUNTER_START` | Il DM avvia un combattimento. |
| `TOKEN_PLACED` | Una pedina viene posizionata sulla griglia. |
| `TURN_STARTED` | Inizia il turno di un personaggio. |
| `REACHABLE_CELLS_CALCULATED` | Il sistema calcola le celle raggiungibili. |
| `MOVE_CONFIRMED` | Movimento accettato. |
| `MOVE_REJECTED` | Movimento rifiutato. |
| `DICE_ROLLED` | Tiro di dado digitale registrato dall'app. |
| `ATTACK` | Attacco dichiarato. |
| `DAMAGE` | Danno applicato. |
| `TERRAIN_UPDATED` | Il DM modifica terreno normale, terreno difficile o aree bloccate. |
| `TRAP_REVEALED` | Trappola rivelata. |
| `TRAP_TRIGGERED` | Trappola attivata dal passaggio o dalla posizione del personaggio. |
| `ROUND_END` | Fine round. |
| `SESSION_END` | Fine sessione. |

## 9. Interfacce previste

### App mobile giocatore

- associazione pedina tramite QR/NFC;
- configurazione personaggio;
- visualizzazione turno corrente;
- azioni disponibili;
- tiro dadi digitale;
- registro combattimento;
- feedback su movimento valido/non valido.

### App o dashboard Dungeon Master

- avvio sessione;
- avvio encounter;
- posizionamento mostri;
- gestione muri, ostacoli e trappole;
- consultazione log eventi;
- controllo dello stato dei personaggi.

### Plancia fisica

- rappresentazione della griglia;
- rilevamento o simulazione posizione pedine;
- possibile illuminazione delle caselle raggiungibili;
- segnalazione visiva di muri, ostacoli e celle non valide;
- invio eventi al nodo edge.

## 10. Componenti tecnici

| Componente | Stato attuale | Ruolo |
| :--- | :--- | :--- |
| Docker | Implementato | Avvia Mosquitto e PostgreSQL. |
| Mosquitto MQTT | Implementato | Broker eventi. |
| Simulatore Python | Implementato | Simula eventi della plancia. |
| `event-service` Spring Boot | Implementato | Riceve eventi MQTT, salva su DB, espone API REST. |
| PostgreSQL | Implementato | Salva eventi in `game_schema.game_events`. |
| OpenAPI | Implementata | Documenta l'API REST esistente. |
| Modello griglia | Implementato | Rappresenta posizioni, celle, terreno e richieste di movimento. |
| Algoritmo movimento | Implementato | Calcola celle raggiungibili, percorso e trappole attraversate. |
| API celle raggiungibili | Implementata | Espone il risultato del movimento a dashboard/app. |
| App mobile | Da implementare | Interfaccia giocatore/DM. |
| Edge avanzato | Da implementare | Simulazione piu vicina alla plancia fisica. |

## 11. MVP per l'esame

L'MVP deve restare concentrato sulla parte PISSIR:

- una sessione D&D demo;
- un tavolo/plancia simulata;
- eventi MQTT;
- backend che riceve e salva;
- API REST per leggere eventi;
- algoritmo movimento su griglia;
- documentazione dei contratti;
- demo end-to-end.

Non sono prioritari:

- shop snack/drink;
- prenotazioni avanzate;
- pagamento;
- autenticazione complessa;
- hardware reale completo;
- app mobile completa in produzione.

## 12. Stato attuale del progetto

Gia realizzato:

- infrastruttura Docker con PostgreSQL e Mosquitto;
- simulatore Python che pubblica eventi D&D;
- subscriber Python leggibile per debug;
- `event-service` Java/Spring Boot;
- parsing eventi MQTT;
- salvataggio eventi su PostgreSQL;
- API REST `GET /api/v1/sessions/{sessionId}/events`;
- modello logico della griglia D&D;
- algoritmo di movimento con Dijkstra semplificato;
- API REST `POST /api/v1/movement/reachable-cells`;
- test automatici;
- documentazione tecnica iniziale;
- specifica OpenAPI dell'endpoint implementato.

Prossimi passi consigliati:

1. aggiungere eventi `MOVE_CONFIRMED`, `MOVE_REJECTED` e `TRAP_TRIGGERED`;
2. aggiungere modello logico per personaggio, pedina e associazione QR/NFC;
3. collegare il movimento a sessioni salvate invece che a una griglia inviata nel body;
4. preparare una demo piu vicina alla plancia fisica.

## 13. Perche il progetto resta coerente con PISSIR

BoardHub permette di mostrare:

- oggetto fisico connesso;
- nodo edge;
- comunicazione MQTT;
- broker publish/subscribe;
- backend REST;
- persistenza eventi;
- sincronizzazione dello stato;
- possibile funzionamento offline;
- algoritmo distribuito/applicativo legato alla plancia.

Il progetto resta quindi centrato su reti, servizi, comunicazione e sistemi distribuiti. L'app mobile e la dashboard servono a rendere visibile il sistema, ma non sostituiscono la parte tecnica principale.
