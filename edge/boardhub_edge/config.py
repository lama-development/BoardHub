import os
from dataclasses import dataclass


@dataclass(frozen=True)
class EdgeConfig:
    """Configurazione del nodo edge, letta dall'ambiente con valori di sviluppo."""

    broker_host: str = "localhost"
    broker_port: int = 1883
    venue_id: str = "venue-01"
    table_id: str = "table-04"
    edge_id: str = ""
    database_path: str = "edge_outbox.sqlite3"
    source: str = "EDGE"
    qos: int = 1
    batch_size: int = 25
    backoff_base_seconds: float = 1.0
    backoff_max_seconds: float = 30.0
    status_interval_seconds: float = 10.0
    retention_seconds: int = 3600

    @staticmethod
    def from_environment() -> "EdgeConfig":
        venue_id = os.getenv("BOARDHUB_EDGE_VENUE_ID", "venue-01")
        table_id = os.getenv("BOARDHUB_EDGE_TABLE_ID", "table-04")
        return EdgeConfig(
            broker_host=os.getenv("BOARDHUB_EDGE_BROKER_HOST", "localhost"),
            broker_port=int(os.getenv("BOARDHUB_EDGE_BROKER_PORT", "1883")),
            venue_id=venue_id,
            table_id=table_id,
            edge_id=os.getenv("BOARDHUB_EDGE_ID", f"edge-{venue_id}-{table_id}"),
            database_path=os.getenv("BOARDHUB_EDGE_DB", "edge_outbox.sqlite3"),
            source=os.getenv("BOARDHUB_EDGE_SOURCE", "EDGE"),
            qos=int(os.getenv("BOARDHUB_EDGE_QOS", "1")),
            batch_size=int(os.getenv("BOARDHUB_EDGE_BATCH", "25")),
            backoff_base_seconds=float(os.getenv("BOARDHUB_EDGE_BACKOFF_BASE", "1.0")),
            backoff_max_seconds=float(os.getenv("BOARDHUB_EDGE_BACKOFF_MAX", "30.0")),
            status_interval_seconds=float(os.getenv("BOARDHUB_EDGE_STATUS_INTERVAL", "10.0")),
            retention_seconds=int(os.getenv("BOARDHUB_EDGE_RETENTION", "3600")),
        )

    @property
    def events_topic(self) -> str:
        return f"boardhub/v1/venues/{self.venue_id}/tables/{self.table_id}/events"

    @property
    def acks_topic(self) -> str:
        return f"boardhub/v1/venues/{self.venue_id}/tables/{self.table_id}/event-acks"

    @property
    def commands_topic(self) -> str:
        return f"boardhub/v1/venues/{self.venue_id}/tables/{self.table_id}/commands"

    @property
    def status_topic(self) -> str:
        return f"boardhub/v1/venues/{self.venue_id}/tables/{self.table_id}/status"
