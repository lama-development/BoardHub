# Nodo edge BoardHub

Processo locale del tavolo. Raccoglie le osservazioni della plancia, le conserva
in una coda persistente e le consegna al backend centrale tramite MQTT.

Esiste per soddisfare il requisito di continuita della traccia: la partita
fisica deve poter proseguire anche quando il collegamento con il server
centrale non e disponibile.

## Cosa fa e cosa non fa

L'edge **non** e un secondo motore di gioco. Non calcola percorsi, non applica
trappole, non decide danni o punti ferita. Il backend resta l'unica autorita
sulle regole, anche durante una disconnessione.

Durante l'assenza di rete il sistema lavora in **modalita degradata**: l'edge
registra cio che osserva e lo mette in coda, il Dungeon Master prosegue
manualmente, e al ritorno della connessione il backend elabora i fatti raccolti.

## Invarianti

1. L'osservazione viene scritta in SQLite **prima** di qualunque tentativo di rete.
2. L'`eventId` non cambia durante i tentativi successivi.
3. Il `sequenceNumber` e monotono per `(sessionId, source)` e viene allocato
   nella stessa transazione dell'inserimento in coda.
4. Il PUBACK di QoS 1 conferma il broker, **non** la persistenza nel backend.
5. Un elemento si chiude solo con l'acknowledgement applicativo del backend.
6. Un acknowledgement ripetuto e innocuo.
7. Un riavvio non perde gli elementi ancora aperti.

## Stati della coda

| Stato | Significato |
| :--- | :--- |
| `PENDING` | Salvato localmente, da pubblicare o da ritentare. |
| `PUBLISHED` | Consegnato al broker, in attesa di conferma applicativa. |
| `ACKNOWLEDGED` | Il backend ha confermato la persistenza (`PERSISTED` o `DUPLICATE`). |
| `REJECTED` | Payload rifiutato definitivamente: non si ritenta. |
| `CONFLICT` | Sessione conclusa o stato incompatibile: serve revisione. |

## Uso

```bash
just edge-setup                          # prepara l'ambiente Python
just edge                                # avvia il nodo
just edge-observe session-demo-001       # accoda un percorso osservato
just edge-queue                          # mostra la coda locale
just edge-status                         # stato tecnico del nodo
just edge-test                           # test dei componenti
```

Lo scenario completo di disconnessione e recupero, con backend attivo:

```bash
just check-offline session-offline-demo-001
```

Spegne il broker, fa osservare la partita al sensore, riaccende il broker e
verifica che gli eventi vengano riallineati una sola volta.

## Baseline riproducibile

Con Docker e `event-service` attivi, scegliere un tavolo nello stato
`DISABLED` ed eseguire dalla radice del repository:

```bash
just edge-setup
just measure 10 7
```

Il comando usa undici eventi per scenario: uno di warm-up escluso dalle
statistiche e dieci campioni effettivi. Misura prima la persistenza PostgreSQL
con broker disponibile, poi ferma Mosquitto, accoda gli eventi in SQLite,
riavvia il broker e misura il tempo fino agli acknowledgement applicativi.

I risultati vengono salvati in una nuova cartella sotto
`artifacts/measurements/`: `measurements.csv` contiene i campioni grezzi e
`report.md` riporta minimo, mediana, p95 e massimo. La cartella e ignorata da
Git. La misura rifiuta tavoli gia abilitati o occupati e ripristina il tavolo
tecnico al termine, anche in caso di errore.

## Configurazione

Tutto e sovrascrivibile da ambiente; i valori predefiniti servono allo sviluppo.

| Variabile | Predefinito | Uso |
| :--- | :--- | :--- |
| `BOARDHUB_EDGE_BROKER_HOST` | `localhost` | Host del broker MQTT. |
| `BOARDHUB_EDGE_BROKER_PORT` | `1883` | Porta del broker. |
| `BOARDHUB_EDGE_VENUE_ID` | `venue-01` | Locale di appartenenza. |
| `BOARDHUB_EDGE_TABLE_ID` | `table-04` | Tavolo servito dal nodo. |
| `BOARDHUB_EDGE_DB` | `edge_outbox.sqlite3` | File della coda locale. |
| `BOARDHUB_EDGE_QOS` | `1` | QoS affidabile per eventi, acknowledgement e comandi. |
| `BOARDHUB_EDGE_STATUS_QOS` | `0` | QoS leggero per lo stato tecnico retained. |
| `BOARDHUB_EDGE_BACKOFF_MAX` | `30.0` | Attesa massima tra due tentativi. |

## Topic usati

| Topic | Direzione |
| :--- | :--- |
| `.../events` | Edge -> Backend |
| `.../event-acks` | Backend -> Edge |
| `.../commands` | Backend -> Edge |
| `.../status` | Edge -> Backend |

I payload sono definiti in [`../docs/CONTRATTI_DI_COMUNICAZIONE.md`](../docs/CONTRATTI_DI_COMUNICAZIONE.md),
sezioni 5.5 e 5.6.
