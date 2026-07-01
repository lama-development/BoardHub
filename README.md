# BoardHub

**Connected Games Platform for Smart Venues**

BoardHub e una piattaforma sperimentale per collegare tavoli D&D/fantasy fisici a un sistema digitale tramite eventi MQTT, backend REST e interfaccia web.

Lo stato attuale del progetto copre la prima parte della pipeline:

```text
simulatore Python -> broker MQTT Mosquitto -> subscriber Python
```

## Struttura

| Cartella | Scopo |
| :--- | :--- |
| `docker/` | Infrastruttura locale con PostgreSQL e Mosquitto. |
| `docs/` | Documentazione condivisa: contratti e changelog. |
| `simulator/` | Script Python per pubblicare e leggere eventi MQTT demo. |

## Avvio rapido

Avviare i servizi locali:

```bash
docker compose -f docker/docker-compose.yml up -d
```

Controllare lo stato:

```bash
docker compose -f docker/docker-compose.yml ps
```

Avviare il subscriber leggibile:

```bash
cd simulator
source .venv/bin/activate
python subscribe_events.py
```

In un secondo terminale pubblicare la mini-sessione D&D:

```bash
cd simulator
source .venv/bin/activate
python publish_event.py
```

## Documentazione

- [Contratti di comunicazione](docs/CONTRATTI_DI_COMUNICAZIONE.md)
- [Changelog](docs/CHANGELOG.md)

## Stato

- Docker locale configurato.
- Contratti REST/MQTT definiti.
- Simulatore MQTT funzionante.
- Subscriber MQTT leggibile funzionante.
- Backend Java e web app ancora da implementare.
