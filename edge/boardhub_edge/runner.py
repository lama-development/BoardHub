import json
import logging
import threading
import time
from datetime import datetime, timedelta, timezone
from typing import Any

import paho.mqtt.client as mqtt

from .config import EdgeConfig
from .outbox import Outbox, PUBLISHED

log = logging.getLogger("boardhub.edge")

# Un elemento pubblicato ma non confermato entro questa finestra torna in coda.
ACK_TIMEOUT_SECONDS = 15


def _iso(moment: datetime) -> str:
    return moment.astimezone(timezone.utc).isoformat(timespec="seconds").replace("+00:00", "Z")


class EdgeNode:
    """Raccoglie le osservazioni del tavolo e le consegna al backend.

    Non decide nulla sulle regole di gioco: movimento, trappole, danni e punti
    ferita restano di competenza del backend anche quando la rete manca.
    """

    def __init__(self, config: EdgeConfig, outbox: Outbox) -> None:
        self._config = config
        self._outbox = outbox
        self._stopping = threading.Event()
        self._connected = threading.Event()
        self._last_connection_at: str | None = outbox.get_state("last_broker_connection_at")

        self._client = mqtt.Client(
            mqtt.CallbackAPIVersion.VERSION2,
            client_id=f"{config.edge_id}-{int(time.time())}",
        )
        self._client.on_connect = self._on_connect
        self._client.on_disconnect = self._on_disconnect
        self._client.on_message = self._on_message
        self._client.reconnect_delay_set(min_delay=1, max_delay=int(config.backoff_max_seconds))

    # -- ciclo di vita -----------------------------------------------------

    def start(self) -> None:
        """Avvia la connessione senza bloccare: offline si lavora comunque."""
        try:
            self._client.connect_async(self._config.broker_host, self._config.broker_port, keepalive=30)
            self._client.loop_start()
        except Exception as error:  # pragma: no cover - dipende dalla rete
            log.warning("Connessione iniziale non riuscita, si prosegue offline: %s", error)

    def stop(self) -> None:
        self._stopping.set()
        try:
            self._client.loop_stop()
            self._client.disconnect()
        except Exception:  # pragma: no cover
            pass

    def run_forever(self) -> None:
        """Alterna consegna della coda, ripristino e pubblicazione dello stato."""
        self.start()
        last_status = 0.0
        try:
            while not self._stopping.is_set():
                self._requeue_unconfirmed()
                delivered = self.deliver_pending()

                now = time.time()
                if now - last_status >= self._config.status_interval_seconds:
                    self.publish_status()
                    self._purge_old()
                    last_status = now

                self._stopping.wait(0.2 if delivered else 1.0)
        except KeyboardInterrupt:  # pragma: no cover
            log.info("Interruzione richiesta dall'operatore.")
        finally:
            self.stop()

    # -- acquisizione ------------------------------------------------------

    def observe(self, session_id: str, event_type: str, payload: dict[str, Any]) -> dict[str, Any]:
        """Registra un'osservazione salvandola in locale prima di ogni invio."""
        event = self._outbox.enqueue(
            session_id=session_id,
            event_type=event_type,
            payload=payload,
            venue_id=self._config.venue_id,
            table_id=self._config.table_id,
            source=self._config.source,
            topic=self._config.events_topic,
        )
        log.info(
            "Osservazione accodata: %s seq=%s sessione=%s",
            event["eventType"], event["sequenceNumber"], event["sessionId"],
        )
        return event

    # -- consegna ----------------------------------------------------------

    def deliver_pending(self) -> int:
        """Pubblica gli elementi scaduti, in ordine di sequenza."""
        if not self._connected.is_set():
            return 0

        due = self._outbox.claim_due(time.time(), self._config.batch_size)
        delivered = 0
        for row in due:
            if not self._connected.is_set():
                break
            if self._publish_row(row):
                delivered += 1
        return delivered

    def _publish_row(self, row) -> bool:
        try:
            info = self._client.publish(row["topic"], row["payload_json"], qos=self._config.qos)
            info.wait_for_publish(timeout=5)
            if info.rc != mqtt.MQTT_ERR_SUCCESS:
                raise RuntimeError(f"codice MQTT {info.rc}")
        except Exception as error:
            self._outbox.schedule_retry(
                row["event_id"],
                row["attempt_count"],
                self._config.backoff_base_seconds,
                self._config.backoff_max_seconds,
                time.time(),
                str(error),
            )
            log.warning("Pubblicazione fallita per %s: %s", row["event_id"], error)
            return False

        # Il PUBACK conferma il broker, non la persistenza: si resta in attesa
        # dell'acknowledgement applicativo prima di chiudere l'elemento.
        self._outbox.mark_published(row["event_id"])
        return True

    def _requeue_unconfirmed(self) -> None:
        threshold = _iso(datetime.now(timezone.utc) - timedelta(seconds=ACK_TIMEOUT_SECONDS))
        reopened = self._outbox.reopen_stale_published(threshold)
        if reopened:
            log.info("Rimessi in coda %s elementi non confermati dal backend.", reopened)

    def _purge_old(self) -> None:
        threshold = _iso(datetime.now(timezone.utc) - timedelta(seconds=self._config.retention_seconds))
        removed = self._outbox.purge_acknowledged(threshold)
        if removed:
            log.debug("Rimossi %s elementi confermati e scaduti.", removed)

    # -- stato tecnico -----------------------------------------------------

    def status_snapshot(self) -> dict[str, Any]:
        counters = self._outbox.counters()
        return {
            "edgeId": self._config.edge_id,
            "venueId": self._config.venue_id,
            "tableId": self._config.table_id,
            "observedAt": _iso(datetime.now(timezone.utc)),
            "brokerConnected": self._connected.is_set(),
            "lastBrokerConnectionAt": self._last_connection_at,
            "outboxPending": counters["PENDING"],
            "outboxPublished": counters["PUBLISHED"],
            "lastAcknowledgedEventId": self._outbox.last_acknowledged_event_id(),
            "edgeVersion": "boardhub-edge-0.1.0",
        }

    def publish_status(self) -> None:
        if not self._connected.is_set():
            return
        payload = json.dumps(self.status_snapshot(), ensure_ascii=False)
        try:
            self._client.publish(self._config.status_topic, payload, qos=self._config.qos, retain=True)
        except Exception as error:  # pragma: no cover
            log.debug("Stato tecnico non pubblicato: %s", error)

    # -- callback MQTT -----------------------------------------------------

    def _on_connect(self, client, userdata, flags, reason_code, properties=None) -> None:
        if reason_code != 0:
            log.warning("Connessione al broker rifiutata: %s", reason_code)
            return
        self._connected.set()
        self._last_connection_at = _iso(datetime.now(timezone.utc))
        self._outbox.set_state("last_broker_connection_at", self._last_connection_at)
        client.subscribe(self._config.acks_topic, qos=self._config.qos)
        client.subscribe(self._config.commands_topic, qos=self._config.qos)
        log.info("Edge collegato al broker; coda in consegna.")

    def _on_disconnect(self, client, userdata, *args) -> None:
        self._connected.clear()
        log.warning("Broker non raggiungibile: le osservazioni restano in coda locale.")

    def _on_message(self, client, userdata, message) -> None:
        try:
            body = json.loads(message.payload.decode("utf-8"))
        except (UnicodeDecodeError, json.JSONDecodeError) as error:
            log.warning("Messaggio non leggibile su %s: %s", message.topic, error)
            return

        if message.topic == self._config.acks_topic:
            self._handle_ack(body)
        elif message.topic == self._config.commands_topic:
            log.info(
                "Comando plancia %s per %s",
                body.get("commandType", "?"), body.get("sessionPieceId", "-"),
            )

    def _handle_ack(self, body: dict[str, Any]) -> None:
        event_id = body.get("eventId")
        status = body.get("status")
        if not event_id or not status:
            log.warning("Acknowledgement incompleto ignorato: %s", body)
            return

        closed = self._outbox.mark_acknowledged(event_id, status, body.get("detail"))
        if closed:
            log.info("Evento %s confermato dal backend: %s", event_id, status)
        else:
            # Un ack ripetuto e innocuo: l'elemento e gia stato chiuso.
            log.debug("Acknowledgement senza effetto per %s (%s).", event_id, status)
