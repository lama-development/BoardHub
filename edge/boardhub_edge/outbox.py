import json
import sqlite3
import threading
import uuid
from datetime import datetime, timezone
from typing import Any, Iterable


PENDING = "PENDING"
PUBLISHED = "PUBLISHED"
ACKNOWLEDGED = "ACKNOWLEDGED"
REJECTED = "REJECTED"
CONFLICT = "CONFLICT"

OPEN_STATES = (PENDING, PUBLISHED)
CLOSED_STATES = (ACKNOWLEDGED, REJECTED, CONFLICT)

SCHEMA = """
CREATE TABLE IF NOT EXISTS edge_outbox (
    event_id TEXT PRIMARY KEY,
    session_id TEXT NOT NULL,
    source TEXT NOT NULL,
    sequence_number INTEGER NOT NULL,
    topic TEXT NOT NULL,
    payload_json TEXT NOT NULL,
    status TEXT NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_attempt_at REAL NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL,
    published_at TEXT,
    acknowledged_at TEXT,
    last_error TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_edge_outbox_sequence
    ON edge_outbox (session_id, source, sequence_number);

-- Supporta la selezione ordinata per sessione e sorgente. Il worker consegna
-- una sequenza solo quando tutte le precedenti sono gia in uno stato finale.
CREATE INDEX IF NOT EXISTS idx_edge_outbox_delivery
    ON edge_outbox (session_id, source, sequence_number, status, next_attempt_at);

-- Evita di ordinare tutti gli elementi confermati per leggerne uno solo.
CREATE INDEX IF NOT EXISTS idx_edge_outbox_acknowledged
    ON edge_outbox (status, acknowledged_at);

-- Superati dai due precedenti: costerebbero scritture senza servire query.
DROP INDEX IF EXISTS idx_edge_outbox_worker;
DROP INDEX IF EXISTS idx_edge_outbox_created;

CREATE TABLE IF NOT EXISTS edge_sequences (
    session_id TEXT NOT NULL,
    source TEXT NOT NULL,
    last_sequence INTEGER NOT NULL,
    PRIMARY KEY (session_id, source)
);

CREATE TABLE IF NOT EXISTS edge_state (
    key TEXT PRIMARY KEY,
    value TEXT
);
"""


def _utc_now_iso() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="milliseconds").replace("+00:00", "Z")


class Outbox:
    """Coda persistente locale: si scrive qui prima di ogni invio MQTT."""

    def __init__(self, path: str) -> None:
        self._lock = threading.Lock()
        self._connection = sqlite3.connect(path, check_same_thread=False)
        self._connection.row_factory = sqlite3.Row
        self._connection.execute("PRAGMA journal_mode=WAL")
        self._connection.execute("PRAGMA synchronous=FULL")
        with self._lock:
            self._connection.executescript(SCHEMA)
            self._connection.commit()

    def close(self) -> None:
        with self._lock:
            self._connection.close()

    # -- scrittura ---------------------------------------------------------

    def enqueue(
        self,
        session_id: str,
        event_type: str,
        payload: dict[str, Any],
        venue_id: str,
        table_id: str,
        source: str,
        topic: str,
    ) -> dict[str, Any]:
        """Alloca la sequenza e salva l'evento nella stessa transazione."""
        event_id = f"evt-{uuid.uuid4()}"
        created_at = _utc_now_iso()

        with self._lock:
            cursor = self._connection.cursor()
            cursor.execute("BEGIN IMMEDIATE")
            try:
                row = cursor.execute(
                    "SELECT last_sequence FROM edge_sequences WHERE session_id = ? AND source = ?",
                    (session_id, source),
                ).fetchone()
                next_sequence = (row["last_sequence"] if row else 0) + 1
                cursor.execute(
                    """
                    INSERT INTO edge_sequences (session_id, source, last_sequence)
                    VALUES (?, ?, ?)
                    ON CONFLICT (session_id, source)
                    DO UPDATE SET last_sequence = excluded.last_sequence
                    """,
                    (session_id, source, next_sequence),
                )

                event = {
                    "eventId": event_id,
                    "eventType": event_type,
                    "venueId": venue_id,
                    "tableId": table_id,
                    "sessionId": session_id,
                    "source": source,
                    "occurredAt": created_at,
                    "sequenceNumber": next_sequence,
                    "payload": payload,
                }

                cursor.execute(
                    """
                    INSERT INTO edge_outbox (
                        event_id, session_id, source, sequence_number, topic,
                        payload_json, status, attempt_count, next_attempt_at, created_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, 0, 0, ?)
                    """,
                    (
                        event_id,
                        session_id,
                        source,
                        next_sequence,
                        topic,
                        json.dumps(event, ensure_ascii=False),
                        PENDING,
                        created_at,
                    ),
                )
                self._connection.commit()
            except Exception:
                self._connection.rollback()
                raise

        return event

    def mark_published(self, event_id: str) -> None:
        """Il broker ha confermato la consegna. Non significa ancora persistito."""
        with self._lock:
            self._connection.execute(
                "UPDATE edge_outbox SET status = ?, published_at = ? WHERE event_id = ? AND status = ?",
                (PUBLISHED, _utc_now_iso(), event_id, PENDING),
            )
            self._connection.commit()

    def mark_acknowledged(self, event_id: str, ack_status: str, detail: str | None = None) -> bool:
        """Chiude l'elemento secondo l'esito applicativo del backend.

        PERSISTED e DUPLICATE sono entrambi successi: il fatto risulta
        registrato una sola volta. Un acknowledgement ripetuto non ha effetto.
        """
        mapping = {
            "PERSISTED": ACKNOWLEDGED,
            "DUPLICATE": ACKNOWLEDGED,
            "REJECTED": REJECTED,
            "CONFLICT": CONFLICT,
        }
        final_status = mapping.get(ack_status)
        if final_status is None:
            return False

        with self._lock:
            cursor = self._connection.execute(
                """
                UPDATE edge_outbox
                SET status = ?, acknowledged_at = ?, last_error = ?
                WHERE event_id = ? AND status IN (?, ?)
                """,
                (final_status, _utc_now_iso(), detail, event_id, PENDING, PUBLISHED),
            )
            self._connection.commit()
            return cursor.rowcount > 0

    def schedule_retry(
        self,
        event_id: str,
        attempt_count: int,
        base_seconds: float,
        max_seconds: float,
        now_epoch: float,
        error: str | None = None,
    ) -> None:
        """Backoff esponenziale su tempo di parete, confrontabile dopo un riavvio."""
        delay = min(base_seconds * (2 ** attempt_count), max_seconds)
        with self._lock:
            self._connection.execute(
                """
                UPDATE edge_outbox
                SET status = ?, attempt_count = ?, next_attempt_at = ?, last_error = ?
                WHERE event_id = ?
                """,
                (PENDING, attempt_count + 1, now_epoch + delay, error, event_id),
            )
            self._connection.commit()

    # -- lettura -----------------------------------------------------------

    def claim_due(self, now_epoch: float, limit: int) -> list[sqlite3.Row]:
        """Restituisce solo sequenze dovute che non possono superarne una aperta."""
        with self._lock:
            return self._connection.execute(
                """
                SELECT current.*
                FROM edge_outbox AS current
                WHERE current.status = ?
                  AND current.next_attempt_at <= ?
                  AND NOT EXISTS (
                      SELECT 1
                      FROM edge_outbox AS previous
                      WHERE previous.session_id = current.session_id
                        AND previous.source = current.source
                        AND previous.sequence_number < current.sequence_number
                        AND previous.status IN (?, ?)
                  )
                ORDER BY current.session_id, current.source, current.sequence_number
                LIMIT ?
                """,
                (PENDING, now_epoch, PENDING, PUBLISHED, limit),
            ).fetchall()

    def reopen_stale_published(self, older_than_iso: str) -> int:
        """Rimette in coda cio che e stato pubblicato ma mai confermato."""
        with self._lock:
            cursor = self._connection.execute(
                "UPDATE edge_outbox SET status = ?, next_attempt_at = 0 WHERE status = ? AND published_at < ?",
                (PENDING, PUBLISHED, older_than_iso),
            )
            self._connection.commit()
            return cursor.rowcount

    def counters(self) -> dict[str, int]:
        with self._lock:
            rows = self._connection.execute(
                "SELECT status, COUNT(*) AS total FROM edge_outbox GROUP BY status"
            ).fetchall()
        counters = {state: 0 for state in (*OPEN_STATES, *CLOSED_STATES)}
        for row in rows:
            counters[row["status"]] = row["total"]
        return counters

    def last_acknowledged_event_id(self) -> str | None:
        with self._lock:
            row = self._connection.execute(
                """
                SELECT event_id FROM edge_outbox
                WHERE status = ? ORDER BY acknowledged_at DESC LIMIT 1
                """,
                (ACKNOWLEDGED,),
            ).fetchone()
        return row["event_id"] if row else None

    def open_items(self) -> list[sqlite3.Row]:
        with self._lock:
            return self._connection.execute(
                "SELECT * FROM edge_outbox WHERE status IN (?, ?) ORDER BY session_id, sequence_number",
                OPEN_STATES,
            ).fetchall()

    def all_items(self) -> list[sqlite3.Row]:
        with self._lock:
            return self._connection.execute(
                "SELECT * FROM edge_outbox ORDER BY created_at, sequence_number"
            ).fetchall()

    # -- manutenzione ------------------------------------------------------

    def purge_acknowledged(self, before_iso: str) -> int:
        with self._lock:
            cursor = self._connection.execute(
                "DELETE FROM edge_outbox WHERE status = ? AND acknowledged_at < ?",
                (ACKNOWLEDGED, before_iso),
            )
            self._connection.commit()
            return cursor.rowcount

    def set_state(self, key: str, value: str) -> None:
        with self._lock:
            self._connection.execute(
                "INSERT INTO edge_state (key, value) VALUES (?, ?) "
                "ON CONFLICT (key) DO UPDATE SET value = excluded.value",
                (key, value),
            )
            self._connection.commit()

    def get_state(self, key: str) -> str | None:
        with self._lock:
            row = self._connection.execute(
                "SELECT value FROM edge_state WHERE key = ?", (key,)
            ).fetchone()
        return row["value"] if row else None
