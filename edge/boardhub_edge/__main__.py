import argparse
import json
import logging
import sys

from .config import EdgeConfig
from .outbox import Outbox
from .runner import EdgeNode
from .sensor import movement_observations


def _build_node(config: EdgeConfig) -> tuple[EdgeNode, Outbox]:
    outbox = Outbox(config.database_path)
    return EdgeNode(config, outbox), outbox


def command_run(config: EdgeConfig) -> int:
    node, outbox = _build_node(config)
    print(f"Edge {config.edge_id} avviato. Coda locale: {config.database_path}")
    print(f"Eventi   -> {config.events_topic}")
    print(f"Ack      <- {config.acks_topic}")
    print(f"Comandi  <- {config.commands_topic}")
    try:
        node.run_forever()
    finally:
        outbox.close()
    return 0


def command_observe(config: EdgeConfig, args) -> int:
    """Accoda un'osservazione senza toccare la rete.

    Funziona identicamente con broker acceso o spento: e la dimostrazione
    dell'invariante secondo cui la scrittura locale precede l'invio.
    """
    node, outbox = _build_node(config)
    try:
        payload = json.loads(args.payload) if args.payload else {}
        event = node.observe(args.session, args.type, payload)
        print(json.dumps(event, indent=2, ensure_ascii=False))
    finally:
        outbox.close()
    return 0


def command_simulate(config: EdgeConfig, args) -> int:
    node, outbox = _build_node(config)
    try:
        count = 0
        for event_type, payload in movement_observations(args.character):
            node.observe(args.session, event_type, payload)
            count += 1
        print(f"Accodate {count} osservazioni per la sessione {args.session}.")
        counters = outbox.counters()
        print(f"In coda: {counters['PENDING']} da inviare, {counters['PUBLISHED']} in attesa di conferma.")
    finally:
        outbox.close()
    return 0


def command_status(config: EdgeConfig) -> int:
    node, outbox = _build_node(config)
    try:
        print(json.dumps(node.status_snapshot(), indent=2, ensure_ascii=False))
    finally:
        outbox.close()
    return 0


def command_list(config: EdgeConfig) -> int:
    outbox = Outbox(config.database_path)
    try:
        rows = outbox.all_items()
        if not rows:
            print("Coda vuota.")
            return 0
        print(f"{'SEQ':>4}  {'STATO':<13} {'TENT':>4}  {'EVENTO':<42} SESSIONE")
        for row in rows:
            print(
                f"{row['sequence_number']:>4}  {row['status']:<13} {row['attempt_count']:>4}  "
                f"{row['event_id']:<42} {row['session_id']}"
            )
        counters = outbox.counters()
        print("\n" + "  ".join(f"{key}={value}" for key, value in counters.items()))
    finally:
        outbox.close()
    return 0


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        prog="boardhub_edge",
        description="Nodo edge BoardHub: coda locale persistente e consegna al backend.",
    )
    parser.add_argument("--verbose", action="store_true", help="Log di dettaglio.")
    subparsers = parser.add_subparsers(dest="command", required=True)

    subparsers.add_parser("run", help="Avvia il nodo edge e consegna la coda.")

    observe = subparsers.add_parser("observe", help="Accoda una singola osservazione.")
    observe.add_argument("--session", required=True)
    observe.add_argument("--type", default="MOVE")
    observe.add_argument("--payload", default="")

    simulate = subparsers.add_parser("simulate", help="Accoda un percorso dimostrativo.")
    simulate.add_argument("--session", required=True)
    simulate.add_argument("--character", default="adv-01")

    subparsers.add_parser("status", help="Mostra lo stato tecnico del nodo.")
    subparsers.add_parser("list", help="Elenca il contenuto della coda locale.")

    args = parser.parse_args(argv)
    logging.basicConfig(
        level=logging.DEBUG if args.verbose else logging.INFO,
        format="%(asctime)s  %(levelname)-7s %(message)s",
        datefmt="%H:%M:%S",
    )

    config = EdgeConfig.from_environment()

    if args.command == "run":
        return command_run(config)
    if args.command == "observe":
        return command_observe(config, args)
    if args.command == "simulate":
        return command_simulate(config, args)
    if args.command == "status":
        return command_status(config)
    if args.command == "list":
        return command_list(config)
    return 1


if __name__ == "__main__":
    sys.exit(main())
