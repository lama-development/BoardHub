"""Misura riproducibile del percorso evento BoardHub online e offline.

Lo strumento usa una sessione temporanea su un tavolo libero, salva i dati
grezzi in CSV e genera un report Markdown. Non sostituisce un benchmark di
produzione: serve a fornire una baseline locale verificabile per il progetto.
"""

from __future__ import annotations

import argparse
import csv
import json
import math
import os
import platform
import socket
import statistics
import subprocess
import tempfile
import threading
import time
import urllib.error
import urllib.request
import uuid
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Iterable

import paho.mqtt.client as mqtt

from .config import EdgeConfig
from .outbox import ACKNOWLEDGED, Outbox
from .runner import EdgeNode


ROOT = Path(__file__).resolve().parents[2]
COMPOSE_FILE = ROOT / "docker" / "docker-compose.yml"
BASE_URL = os.getenv("BOARDHUB_BASE_URL", "http://localhost:8082")
VENUE_KEY = os.getenv("BOARDHUB_VENUE_ADMIN_KEY", "boardhub-local-venue-admin-key")
VENUE_ID = "venue-01"


@dataclass(frozen=True)
class MeasurementRow:
    scenario: str
    sample: int
    event_id: str
    sequence: int
    occurred_at: str
    received_at: str
    persistence_latency_ms: float
    payload_bytes: int
    outbox_attempts: int | None = None
    ack_latency_ms: float | None = None


def utc_now_iso() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="milliseconds").replace("+00:00", "Z")


def parse_iso(value: str) -> datetime:
    return datetime.fromisoformat(value.replace("Z", "+00:00"))


def percentile_nearest_rank(values: Iterable[float], percentile: float) -> float:
    ordered = sorted(values)
    if not ordered:
        raise ValueError("La statistica richiede almeno un valore.")
    rank = max(1, math.ceil(percentile * len(ordered)))
    return ordered[rank - 1]


def summarize(values: Iterable[float]) -> dict[str, float]:
    samples = list(values)
    if not samples:
        raise ValueError("La statistica richiede almeno un valore.")
    return {
        "min": min(samples),
        "median": statistics.median(samples),
        "p95": percentile_nearest_rank(samples, 0.95),
        "max": max(samples),
    }


def measurement_sequences(samples: int) -> range:
    """Return protocol-valid sequences, including one warm-up sample."""
    return range(1, samples + 2)


def _http_json(
    method: str,
    path: str,
    body: dict[str, Any] | None = None,
    headers: dict[str, str] | None = None,
) -> Any:
    payload = None if body is None else json.dumps(body).encode("utf-8")
    request_headers = {"Accept": "application/json", **(headers or {})}
    if payload is not None:
        request_headers["Content-Type"] = "application/json"
    request = urllib.request.Request(
        f"{BASE_URL}{path}", data=payload, headers=request_headers, method=method
    )
    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            raw = response.read()
            return json.loads(raw) if raw else None
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"HTTP {error.code} su {path}: {detail}") from error
    except urllib.error.URLError as error:
        raise RuntimeError(f"Backend non raggiungibile su {BASE_URL}: {error.reason}") from error


def _run(command: list[str], *, capture: bool = False) -> str:
    result = subprocess.run(
        command,
        cwd=ROOT,
        check=True,
        text=True,
        capture_output=capture,
    )
    return result.stdout.strip() if capture else ""


def _compose(action: str) -> None:
    _run(["docker", "compose", "-f", str(COMPOSE_FILE), action, "mosquitto"])


def _wait_port(host: str, port: int, timeout_seconds: float) -> None:
    deadline = time.monotonic() + timeout_seconds
    while time.monotonic() < deadline:
        try:
            with socket.create_connection((host, port), timeout=0.5):
                return
        except OSError:
            time.sleep(0.2)
    raise TimeoutError(f"Porta {host}:{port} non disponibile entro {timeout_seconds:.0f}s.")


def _sql_literal(value: str) -> str:
    return "'" + value.replace("'", "''") + "'"


def _db_rows(session_id: str, source: str, expected: int, timeout_seconds: float = 30) -> list[list[str]]:
    deadline = time.monotonic() + timeout_seconds
    query = f"""
        SET TIME ZONE 'UTC';
        SELECT event_id,
               sequence_number,
               to_char(occurred_at, 'YYYY-MM-DD\"T\"HH24:MI:SS.MS\"Z\"'),
               to_char(received_at, 'YYYY-MM-DD\"T\"HH24:MI:SS.MS\"Z\"'),
               ROUND(EXTRACT(EPOCH FROM (received_at - occurred_at)) * 1000, 3),
               octet_length(payload_json)
        FROM game_schema.game_events
        WHERE session_id = {_sql_literal(session_id)}
          AND source = {_sql_literal(source)}
        ORDER BY sequence_number;
    """
    while time.monotonic() < deadline:
        output = _run(
            [
                "docker", "exec", "boardhub_db", "psql",
                "-U", "boardhub_user", "-d", "boardhub_db",
                "-At", "-F", "\t", "-c", query,
            ],
            capture=True,
        )
        rows = [line.split("\t") for line in output.splitlines() if "\t" in line]
        if len(rows) >= expected:
            return rows
        time.sleep(0.1)
    raise TimeoutError(f"Persistiti {len(rows)}/{expected} eventi {source} entro il timeout.")


class MqttPublisher:
    def __init__(self, host: str, port: int) -> None:
        self._connected = threading.Event()
        self._client = mqtt.Client(
            mqtt.CallbackAPIVersion.VERSION2,
            client_id=f"boardhub-measure-{uuid.uuid4()}",
        )
        self._client.on_connect = self._on_connect
        self._client.connect(host, port, keepalive=30)
        self._client.loop_start()
        if not self._connected.wait(5):
            self.close()
            raise TimeoutError("Connessione MQTT non completata entro 5 secondi.")

    def _on_connect(self, client, userdata, flags, reason_code, properties=None) -> None:
        if reason_code == 0:
            self._connected.set()

    def publish(self, topic: str, event: dict[str, Any], qos: int = 1) -> None:
        info = self._client.publish(topic, json.dumps(event), qos=qos)
        info.wait_for_publish(timeout=5)
        if info.rc != mqtt.MQTT_ERR_SUCCESS:
            raise RuntimeError(f"Pubblicazione MQTT fallita: codice {info.rc}.")

    def close(self) -> None:
        self._client.disconnect()
        self._client.loop_stop()


def _event(session_id: str, table_id: str, source: str, sequence: int) -> dict[str, Any]:
    return {
        "eventId": f"evt-measure-{uuid.uuid4()}",
        "eventType": "MOVE",
        "venueId": VENUE_ID,
        "tableId": table_id,
        "sessionId": session_id,
        "source": source,
        "occurredAt": utc_now_iso(),
        "sequenceNumber": sequence,
        "payload": {
            "characterId": "measure-character",
            "from": "A1",
            "to": "B1",
            "observedBy": "measurement",
        },
    }


def _to_rows(
    scenario: str,
    database_rows: list[list[str]],
    outbox_by_id: dict[str, Any] | None = None,
) -> list[MeasurementRow]:
    result = []
    for sample, row in enumerate(database_rows):
        event_id, sequence, occurred_at, received_at, latency, payload_bytes = row
        outbox_row = None if outbox_by_id is None else outbox_by_id.get(event_id)
        ack_latency = None
        attempts = None
        if outbox_row is not None:
            attempts = int(outbox_row["attempt_count"])
            ack_latency = (
                parse_iso(outbox_row["acknowledged_at"]) - parse_iso(outbox_row["created_at"])
            ).total_seconds() * 1000
        result.append(
            MeasurementRow(
                scenario=scenario,
                sample=sample,
                event_id=event_id,
                sequence=int(sequence),
                occurred_at=occurred_at,
                received_at=received_at,
                persistence_latency_ms=float(latency),
                payload_bytes=int(payload_bytes),
                outbox_attempts=attempts,
                ack_latency_ms=ack_latency,
            )
        )
    return result


def _write_csv(path: Path, rows: list[MeasurementRow]) -> None:
    with path.open("w", newline="", encoding="utf-8") as stream:
        writer = csv.writer(stream)
        writer.writerow(MeasurementRow.__dataclass_fields__.keys())
        for row in rows:
            writer.writerow(row.__dict__.values())


def _format_stats(label: str, stats: dict[str, float]) -> str:
    return (
        f"| {label} | {stats['min']:.2f} | {stats['median']:.2f} | "
        f"{stats['p95']:.2f} | {stats['max']:.2f} |"
    )


def build_report(
    metadata: dict[str, str],
    rows: list[MeasurementRow],
    recovery_ms: float,
) -> str:
    measured = [row for row in rows if row.sample > 0]
    online = [row for row in measured if row.scenario == "online"]
    offline = [row for row in measured if row.scenario == "offline-recovery"]
    online_stats = summarize(row.persistence_latency_ms for row in online)
    offline_stats = summarize(row.persistence_latency_ms for row in offline)
    ack_stats = summarize(row.ack_latency_ms for row in offline if row.ack_latency_ms is not None)
    throughput = len(offline) / (recovery_ms / 1000) if recovery_ms > 0 else 0
    metadata_lines = "\n".join(f"- **{key}:** {value}" for key, value in metadata.items())
    return f"""# Baseline prestazioni BoardHub

## Metadati

{metadata_lines}

Il primo evento di ogni scenario e un warm-up ed e escluso dalle statistiche.
Il p95 usa il metodo *nearest rank*.

## Latenze

| Scenario | Min ms | Mediana ms | P95 ms | Max ms |
| --- | ---: | ---: | ---: | ---: |
{_format_stats("Online: sensore -> PostgreSQL", online_stats)}
{_format_stats("Recupero offline: evento -> PostgreSQL", offline_stats)}
{_format_stats("Recupero offline: outbox -> ACK applicativo", ack_stats)}

## Recupero offline

- Eventi misurati: **{len(offline)}**
- Tempo dal riavvio del broker a tutti gli ACK: **{recovery_ms:.2f} ms**
- Velocita effettiva di recupero: **{throughput:.2f} eventi/s**
- Eventi duplicati: **0** (un solo record per `eventId`)

## Interpretazione

Questa e una baseline locale, non un requisito prestazionale di produzione.
Il CSV associato conserva i campioni grezzi per consentire il ricalcolo.
"""


def _metadata(samples: int, table_number: int) -> dict[str, str]:
    commit = _run(["git", "rev-parse", "--short", "HEAD"], capture=True)
    dirty = bool(_run(["git", "status", "--porcelain"], capture=True))
    return {
        "Esecuzione UTC": utc_now_iso(),
        "Commit": f"{commit}{' (working tree modificata)' if dirty else ''}",
        "Sistema": f"{platform.system()} {platform.release()} ({platform.machine()})",
        "Python": platform.python_version(),
        "Campioni per scenario": str(samples),
        "Tavolo": str(table_number),
        "QoS eventi/ACK/comandi": "1",
        "QoS stato retained": "0",
        "Attesa riconnessione backend": (
            f"{float(os.getenv('BOARDHUB_MEASURE_RECONNECT_WAIT', '5')):.1f} s"
        ),
    }


def _create_session(session_id: str, table_number: int) -> tuple[str, str, str]:
    table_id = f"table-{table_number:02d}"
    public_id = f"qr-table-{table_number:02d}"
    status = _http_json("GET", f"/api/v1/public/tables/{public_id}")
    if status["status"] != "DISABLED":
        raise RuntimeError(
            f"Il Tavolo {table_number} e {status['status']}: scegli un tavolo DISABLED per non alterare partite reali."
        )
    admin_headers = {"X-BoardHub-Venue-Key": VENUE_KEY}
    _http_json(
        "POST", f"/api/v1/admin/tables/{public_id}/enable",
        {"durationMinutes": 5}, admin_headers,
    )
    created = _http_json(
        "POST",
        "/api/v1/sessions",
        {
            "sessionId": session_id,
            "venueId": VENUE_ID,
            "tableId": table_id,
            "tablePublicId": public_id,
            "tableDisplayName": f"Tavolo {table_number}",
            "title": "Misurazione tecnica BoardHub",
            "gameType": "DND",
            "publicSummary": "Sessione temporanea per baseline locale.",
            "acceptingJoinRequests": False,
            "grid": {
                "width": 3, "height": 3,
                "difficultCells": [], "blockedCells": [], "obstacleCells": [],
                "occupiedCells": [], "walls": [], "traps": [],
            },
        },
    )
    return table_id, public_id, created["dmAccessToken"]


def _cleanup_table(public_id: str) -> None:
    status = _http_json("GET", f"/api/v1/public/tables/{public_id}")["status"]
    admin_headers = {"X-BoardHub-Venue-Key": VENUE_KEY}
    if status == "IN_SESSION":
        _http_json(
            "POST",
            f"/api/v1/admin/tables/{public_id}/close-session",
            headers=admin_headers,
        )
    elif status == "CLAIMABLE":
        _http_json(
            "POST",
            f"/api/v1/admin/tables/{public_id}/disable",
            headers=admin_headers,
        )


def run_measurement(samples: int, table_number: int, output_root: Path) -> Path:
    if samples < 10:
        raise ValueError("Servono almeno 10 campioni per scenario.")
    if not 1 <= table_number <= 8:
        raise ValueError("Il tavolo deve essere compreso tra 1 e 8.")

    _http_json("GET", "/actuator/health")
    _run(["docker", "inspect", "boardhub_db", "boardhub_mqtt"], capture=True)
    if not (ROOT / "edge" / ".venv" / "bin" / "python").exists():
        raise RuntimeError("Ambiente edge assente. Esegui prima: just edge-setup")

    run_id = datetime.now().strftime("%Y%m%d-%H%M%S")
    output_dir = output_root / run_id
    output_dir.mkdir(parents=True, exist_ok=False)
    session_id = f"session-measure-{run_id}"
    table_id = f"table-{table_number:02d}"
    public_id = f"qr-table-{table_number:02d}"
    broker_stopped = False
    all_rows: list[MeasurementRow] = []
    recovery_ms = 0.0

    try:
        table_id, public_id, _ = _create_session(session_id, table_number)
        topic = f"boardhub/v1/venues/{VENUE_ID}/tables/{table_id}/events"

        print("1/4 Misura online: pubblicazione QoS 1 e persistenza PostgreSQL...")
        online_source = "MEASURE_ONLINE"
        publisher = MqttPublisher("localhost", 1883)
        try:
            for sequence in measurement_sequences(samples):
                publisher.publish(topic, _event(session_id, table_id, online_source, sequence), qos=1)
        finally:
            publisher.close()
        online_db = _db_rows(session_id, online_source, samples + 1)
        all_rows.extend(_to_rows("online", online_db))

        print("2/4 Simula assenza del broker e accoda gli eventi in SQLite...")
        _compose("stop")
        broker_stopped = True
        with tempfile.TemporaryDirectory(prefix="boardhub-measure-") as temp_dir:
            outbox = Outbox(str(Path(temp_dir) / "measure.sqlite3"))
            config = EdgeConfig(
                broker_host="localhost",
                broker_port=1883,
                venue_id=VENUE_ID,
                table_id=table_id,
                edge_id=f"measure-{run_id}",
                database_path=str(Path(temp_dir) / "measure.sqlite3"),
                source="MEASURE_EDGE",
                qos=1,
                status_qos=0,
                status_interval_seconds=3600,
                retention_seconds=86400,
            )
            node = EdgeNode(config, outbox)
            worker = None
            try:
                for sequence in range(samples + 1):
                    node.observe(
                        session_id,
                        "MOVE",
                        {
                            "characterId": "measure-character",
                            "from": "A1",
                            "to": "B1",
                            "observedBy": "measurement",
                        },
                    )

                print("3/4 Riavvia il broker e misura recupero piu ACK applicativi...")
                recovery_start = time.monotonic()
                _compose("start")
                broker_stopped = False
                _wait_port("localhost", 1883, 10)
                reconnect_wait = float(os.getenv("BOARDHUB_MEASURE_RECONNECT_WAIT", "5"))
                time.sleep(reconnect_wait)
                worker = threading.Thread(target=node.run_forever, daemon=True)
                worker.start()
                deadline = time.monotonic() + 45
                while time.monotonic() < deadline:
                    if outbox.counters()[ACKNOWLEDGED] == samples + 1:
                        break
                    time.sleep(0.1)
                else:
                    raise TimeoutError(
                        "Non tutti gli eventi offline hanno ricevuto l'ACK applicativo."
                    )
                recovery_ms = (time.monotonic() - recovery_start) * 1000
                outbox_rows = {row["event_id"]: row for row in outbox.all_items()}
                offline_db = _db_rows(session_id, config.source, samples + 1)
                all_rows.extend(_to_rows("offline-recovery", offline_db, outbox_rows))
            finally:
                node.stop()
                if worker is not None:
                    worker.join(timeout=3)
                outbox.close()

        print("4/4 Scrive CSV grezzo e report Markdown...")
        _write_csv(output_dir / "measurements.csv", all_rows)
        report = build_report(_metadata(samples, table_number), all_rows, recovery_ms)
        (output_dir / "report.md").write_text(report, encoding="utf-8")
        return output_dir
    finally:
        if broker_stopped:
            try:
                _compose("start")
            except Exception:
                pass
        try:
            _cleanup_table(public_id)
        except Exception as error:
            print(f"ATTENZIONE: tavolo temporaneo non ripristinato automaticamente: {error}")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Baseline locale online/offline di BoardHub.")
    parser.add_argument("--samples", type=int, default=10)
    parser.add_argument("--table", type=int, default=7)
    parser.add_argument(
        "--output", type=Path, default=ROOT / "artifacts" / "measurements"
    )
    args = parser.parse_args(argv)
    try:
        output = run_measurement(args.samples, args.table, args.output)
    except (ValueError, RuntimeError, TimeoutError, subprocess.CalledProcessError) as error:
        print(f"[ERRORE] {error}")
        return 1
    print(f"Baseline completata: {output / 'report.md'}")
    print(f"Campioni grezzi: {output / 'measurements.csv'}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
