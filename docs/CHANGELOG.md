# BoardHub
> Changelog pubblico del repository GitHub.  
> Serve a tenere allineato il collaboratore su cosa e stato fatto, perche e come testarlo.

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

### Avvio infrastruttura

Comando usato:

```bash
docker compose -f docker/docker-compose.yml up -d
```

Motivo: avviare PostgreSQL e Mosquitto in locale.

Controllo usato:

```bash
docker compose -f docker/docker-compose.yml ps
```

Motivo: verificare che `boardhub_db` e `boardhub_mqtt` siano in stato `Up`.

### Test MQTT leggibile

Terminale 1, subscriber:

```bash
cd simulator
source .venv/bin/activate
python subscribe_events.py
```

Motivo: ascoltare il topic MQTT e mostrare gli eventi in forma comprensibile.

Terminale 2, publisher:

```bash
cd simulator
source .venv/bin/activate
python publish_event.py
```

Motivo: generare e pubblicare una mini-sessione D&D composta da:

```text
SESSION_START -> MOVE -> SPAWN_MONSTER -> ATTACK -> DAMAGE -> ROUND_END
```

Flusso confermato:

```text
simulatore Python -> broker MQTT Mosquitto -> subscriber Python
```

## Notes

- Backend Java/Spring Boot non ancora implementato.
- Il lavoro attuale dimostra la prima parte della pipeline PISSIR: generazione evento, trasporto MQTT e ricezione.
