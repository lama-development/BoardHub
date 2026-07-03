# BoardHub
> Changelog pubblico del repository GitHub.  
> Serve a tenere allineato il collaboratore su cosa e stato fatto e sullo stato del progetto.  
> I comandi dettagliati, i tentativi da terminale e le note personali restano nel diario di bordo privato.

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
- `README.md` segnala che la pipeline prevista arriva fino a PostgreSQL.

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

- Aggiornato `README.md` con comandi per avviare e testare `event-service`.
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

| Area | File | Motivo |
| :--- | :--- | :--- |
| Infrastruttura | `docker/docker-compose.yml` | Avvia in locale PostgreSQL e Mosquitto con un solo comando. |
| Database | `docker/postgres/init.sql` | Crea gli schemi iniziali `venue_schema`, `game_schema`, `stats_schema`. |
| MQTT | `docker/mosquitto/mosquitto.conf` | Configura Mosquitto per i test locali sulla porta `1883`. |
| Contratti | `docs/CONTRATTI_DI_COMUNICAZIONE.md` | Definisce topic MQTT, payload JSON, REST API previste e regole di sync. |
| Changelog | `docs/CHANGELOG.md` | Documenta le modifiche condivise nel repository. |
| Simulatore | `simulator/publish_event.py` | Pubblica una mini-sessione D&D sul broker MQTT. |
| Subscriber | `simulator/subscribe_events.py` | Riceve gli eventi MQTT e li stampa in forma leggibile. |
| Dipendenze | `simulator/requirements.txt` | Definisce la libreria Python `paho-mqtt`. |

## Changed

- Separata la struttura del progetto in tre aree iniziali:

  ```text
  docker/     -> infrastruttura locale
  docs/       -> documentazione condivisa
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
