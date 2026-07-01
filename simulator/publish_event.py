import json
import uuid
from datetime import datetime, timezone

import paho.mqtt.client as mqtt


"""
Simulatore MQTT minimale per BoardHub.

Lo script genera una breve sequenza di eventi D&D e li pubblica sul topic MQTT
definito nel contratto di comunicazione del progetto.
"""


BROKER_HOST = "localhost"
BROKER_PORT = 1883

VENUE_ID = "venue-01"
TABLE_ID = "table-04"
SESSION_ID = "session-20260630-001"

TOPIC = f"boardhub/v1/venues/{VENUE_ID}/tables/{TABLE_ID}/events"


class Style:
    """Codici ANSI sobri per rendere il terminale piu leggibile."""

    RESET = "\033[0m"
    BOLD = "\033[1m"
    DIM = "\033[2m"
    RED = "\033[31m"
    GREEN = "\033[32m"
    YELLOW = "\033[33m"
    BLUE = "\033[34m"
    MAGENTA = "\033[35m"
    CYAN = "\033[36m"


def styled(text: str, *styles: str) -> str:
    """Applica uno o piu stili ANSI a una stringa."""
    return f"{''.join(styles)}{text}{Style.RESET}"


def event_color(event_type: str) -> str:
    """Associa ogni tipo di evento a un colore leggibile e non troppo acceso."""
    return {
        "SESSION_START": Style.CYAN,
        "MOVE": Style.GREEN,
        "SPAWN_MONSTER": Style.MAGENTA,
        "ATTACK": Style.YELLOW,
        "DAMAGE": Style.RED,
        "ROUND_END": Style.BLUE,
    }.get(event_type, Style.CYAN)


def build_event(event_type: str, sequence_number: int, payload: dict) -> dict:
    """Costruisce un evento BoardHub conforme al contratto MQTT."""
    return {
        "eventId": f"evt-{uuid.uuid4()}",
        "eventType": event_type,
        "venueId": VENUE_ID,
        "tableId": TABLE_ID,
        "sessionId": SESSION_ID,
        "source": "SIMULATOR",
        "occurredAt": datetime.now(timezone.utc).isoformat(),
        "sequenceNumber": sequence_number,
        "payload": payload
    }


def build_demo_session_events() -> list[dict]:
    """Genera una mini-sessione D&D composta da eventi ordinati."""
    return [
        build_event(
            "SESSION_START",
            1,
            {
                "title": "Cripta del Re Caduto",
                "gameType": "DND",
                "characters": ["adv-01"],
            },
        ),
        build_event(
            "MOVE",
            2,
            {
                "characterId": "adv-01",
                "from": "A3",
                "to": "A4",
            },
        ),
        build_event(
            "SPAWN_MONSTER",
            3,
            {
                "monsterId": "mon-01",
                "monsterType": "goblin",
                "position": "C5",
                "hitPoints": 12,
            },
        ),
        build_event(
            "ATTACK",
            4,
            {
                "attackerId": "adv-01",
                "targetId": "mon-01",
                "weapon": "longsword",
                "hit": True,
            },
        ),
        build_event(
            "DAMAGE",
            5,
            {
                "targetId": "mon-01",
                "amount": 8,
                "remainingHitPoints": 4,
            },
        ),
        build_event(
            "ROUND_END",
            6,
            {
                "round": 1,
            },
        ),
    ]


def describe_event(event: dict) -> str:
    """Restituisce una descrizione leggibile dell'evento pubblicato."""
    event_type = event["eventType"]
    sequence_number = event["sequenceNumber"]
    payload = event["payload"]
    label = styled(event_type, event_color(event_type), Style.BOLD)
    sequence = styled(f"seq={sequence_number}", Style.DIM)
    prefix = styled("Published", Style.DIM)

    if event_type == "SESSION_START":
        return f"{prefix} {label} {sequence}: {payload['title']}"
    if event_type == "MOVE":
        return (
            f"{prefix} {label} {sequence}: "
            f"{payload['characterId']} {payload['from']} -> {payload['to']}"
        )
    if event_type == "SPAWN_MONSTER":
        return (
            f"{prefix} {label} {sequence}: "
            f"{payload['monsterId']} at {payload['position']}"
        )
    if event_type == "ATTACK":
        return (
            f"{prefix} {label} {sequence}: "
            f"{payload['attackerId']} attacks {payload['targetId']}"
        )
    if event_type == "DAMAGE":
        return (
            f"{prefix} {label} {sequence}: "
            f"{payload['targetId']} takes {payload['amount']} damage"
        )
    if event_type == "ROUND_END":
        return f"{prefix} {label} {sequence}: round {payload['round']}"

    return f"{prefix} {label} {sequence}"


def publish_event(client: mqtt.Client, event: dict) -> None:
    """Pubblica un singolo evento sul topic MQTT BoardHub."""
    message = json.dumps(event)
    result = client.publish(TOPIC, payload=message, qos=1)
    result.wait_for_publish(timeout=5)
    print(describe_event(event))


def main() -> None:
    """Apre la connessione MQTT, pubblica la demo session e chiude il client."""
    client = mqtt.Client(
        mqtt.CallbackAPIVersion.VERSION2,
        client_id="boardhub-simulator"
    )

    client.connect(BROKER_HOST, BROKER_PORT, keepalive=60)
    client.loop_start()

    print(f"{styled('Publishing demo session on topic:', Style.DIM)} {styled(TOPIC, Style.CYAN)}")
    for event in build_demo_session_events():
        publish_event(client, event)

    client.loop_stop()
    client.disconnect()
    print(styled("Demo session published.", Style.GREEN))


if __name__ == "__main__":
    main()
