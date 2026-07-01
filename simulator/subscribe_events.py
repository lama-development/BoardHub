import argparse
import json
import threading

import paho.mqtt.client as mqtt


"""
Subscriber MQTT minimale per BoardHub.

Lo script ascolta gli eventi D&D pubblicati dal simulatore e li stampa in forma
leggibile, cosi il flusso MQTT e piu facile da capire durante debug e demo.
"""


BROKER_HOST = "localhost"
BROKER_PORT = 1883

VENUE_ID = "venue-01"
TABLE_ID = "table-04"

TOPIC = f"boardhub/v1/venues/{VENUE_ID}/tables/{TABLE_ID}/events"


class Style:
    """Codici ANSI per leggibilità migliorata."""

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


def describe_event(event: dict) -> str:
    """Trasforma un evento MQTT BoardHub in una frase leggibile."""
    event_type = event.get("eventType", "UNKNOWN")
    sequence_number = event.get("sequenceNumber", "?")
    payload = event.get("payload", {})
    sequence = styled(f"[{sequence_number}]", Style.DIM)
    label = styled(event_type, event_color(event_type), Style.BOLD)

    if event_type == "SESSION_START":
        return f"{sequence} {label}: session started - {payload.get('title')}"
    if event_type == "MOVE":
        return (
            f"{sequence} {label}: "
            f"{payload.get('characterId')} {payload.get('from')} -> {payload.get('to')}"
        )
    if event_type == "SPAWN_MONSTER":
        return (
            f"{sequence} {label}: "
            f"{payload.get('monsterId')} at {payload.get('position')}"
        )
    if event_type == "ATTACK":
        return (
            f"{sequence} {label}: "
            f"{payload.get('attackerId')} attacks {payload.get('targetId')}"
        )
    if event_type == "DAMAGE":
        return (
            f"{sequence} {label}: "
            f"{payload.get('targetId')} takes {payload.get('amount')} damage"
        )
    if event_type == "ROUND_END":
        return f"{sequence} {label}: round {payload.get('round')} ended"

    return f"{sequence} {label}: {payload}"


def parse_message(message: mqtt.MQTTMessage) -> dict | None:
    """Converte il payload MQTT JSON in dizionario Python."""
    try:
        return json.loads(message.payload.decode("utf-8"))
    except json.JSONDecodeError:
        print(f"Invalid JSON received on topic {message.topic}")
        return None


def main() -> None:
    """Ascolta un numero finito di eventi e poi termina."""
    parser = argparse.ArgumentParser(description="BoardHub MQTT event subscriber")
    parser.add_argument("--count", type=int, default=6, help="Number of events to read")
    parser.add_argument("--timeout", type=int, default=30, help="Maximum wait time in seconds")
    args = parser.parse_args()

    received = {"count": 0}
    state = {"listening_printed": False}
    finished = threading.Event()

    def on_connect(client: mqtt.Client, userdata, flags, reason_code, properties) -> None:
        client.subscribe(TOPIC, qos=1)
        if not state["listening_printed"]:
            print(f"{styled('Listening on topic:', Style.DIM)} {styled(TOPIC, Style.CYAN)}")
            state["listening_printed"] = True

    def on_message(client: mqtt.Client, userdata, message: mqtt.MQTTMessage) -> None:
        event = parse_message(message)
        if event is None:
            return

        received["count"] += 1
        print(describe_event(event))

        if received["count"] >= args.count:
            finished.set()

    client = mqtt.Client(
        mqtt.CallbackAPIVersion.VERSION2,
        client_id="boardhub-readable-subscriber",
    )
    client.on_connect = on_connect
    client.on_message = on_message

    client.connect(BROKER_HOST, BROKER_PORT, keepalive=60)
    client.loop_start()

    completed = finished.wait(timeout=args.timeout)

    client.loop_stop()
    client.disconnect()

    if completed:
        print(styled(f"Read {received['count']} events. Subscriber stopped.", Style.GREEN))
    else:
        print(styled(f"Timeout reached after {args.timeout}s. Read {received['count']} events.", Style.YELLOW))


if __name__ == "__main__":
    main()
