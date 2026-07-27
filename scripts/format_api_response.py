#!/usr/bin/env python3
"""Render BoardHub API responses for terminal use without changing their JSON contracts."""

from __future__ import annotations

import json
import os
import shutil
import subprocess
import sys
from datetime import datetime
from typing import Any, Iterable


USE_COLOR = sys.stdout.isatty() and not os.environ.get("NO_COLOR")
RESET = "\033[0m" if USE_COLOR else ""
BOLD = "\033[1m" if USE_COLOR else ""
DIM = "\033[2m" if USE_COLOR else ""
GREEN = "\033[38;5;42m" if USE_COLOR else ""
YELLOW = "\033[38;5;214m" if USE_COLOR else ""
RED = "\033[1;31m" if USE_COLOR else ""
CYAN = "\033[38;5;39m" if USE_COLOR else ""
PURPLE = "\033[38;5;141m" if USE_COLOR else ""


def style(value: Any, color: str) -> str:
    return f"{color}{value}{RESET}" if color else str(value)


def text(value: Any, fallback: str = "-") -> str:
    if value is None or value == "":
        return fallback
    if isinstance(value, bool):
        return "SI" if value else "NO"
    return str(value)


def local_date(value: Any) -> str:
    if not value:
        return "-"
    raw = str(value)
    try:
        parsed = datetime.fromisoformat(raw.replace("Z", "+00:00"))
        return parsed.astimezone().strftime("%d/%m/%Y %H:%M:%S")
    except ValueError:
        return raw


def field(label: str, value: Any, color: str = "") -> None:
    print(f"  {style(label + ':', BOLD)} {style(text(value), color)}")


def section(title: str) -> None:
    print(f"\n{style(title, BOLD)}")


def shorten(value: Any, limit: int) -> str:
    rendered = text(value)
    if len(rendered) <= limit:
        return rendered
    return rendered[: max(1, limit - 1)] + "…"


def table(headers: list[str], rows: Iterable[Iterable[Any]], limits: list[int] | None = None) -> None:
    materialized = [[text(cell) for cell in row] for row in rows]
    if not materialized:
        print(style("  Nessun elemento.", DIM))
        return

    column_count = len(headers)
    terminal_width = shutil.get_terminal_size((120, 24)).columns
    available = max(60, terminal_width - (column_count - 1) * 3)
    default_limit = max(12, available // column_count)
    effective_limits = limits or [default_limit] * column_count

    widths: list[int] = []
    for index, header in enumerate(headers):
        widest = max([len(header)] + [len(row[index]) for row in materialized])
        widths.append(min(widest, effective_limits[index]))

    def render(row: list[str]) -> str:
        cells = [shorten(row[index], widths[index]).ljust(widths[index]) for index in range(column_count)]
        return "  " + "   ".join(cells).rstrip()

    print(style(render(headers), BOLD))
    print(style("  " + "   ".join("-" * width for width in widths), DIM))
    for row in materialized:
        print(render(row))


def normalized_table_status(data: dict[str, Any]) -> str:
    raw = text(data.get("status") or data.get("availability"))
    return {
        "DISABLED": "DISABILITATO",
        "CLAIMABLE": "PRONTO",
        "IN_SESSION": "IN SESSIONE",
    }.get(raw, raw)


def render_error(data: dict[str, Any]) -> None:
    print(style("[ERRORE API]", RED))
    field("Codice", data.get("code"))
    field("Motivo", data.get("message"))


def render_health(data: dict[str, Any]) -> None:
    status = text(data.get("status"))
    color = GREEN if status == "UP" else RED
    print(f"{style('●', color)} Backend {style('operativo', color) if status == 'UP' else style(status, color)}")
    field("Indirizzo", "http://localhost:8082")


def render_venue_tables(data: list[dict[str, Any]]) -> None:
    rows = []
    counts: dict[str, int] = {}
    for item in data:
        status = normalized_table_status(item)
        counts[status] = counts.get(status, 0) + 1
        session = item.get("activeSession") or {}
        if session:
            detail = f"{text(session.get('title'))} · {text(session.get('sessionId'))}"
        elif item.get("claimExpiresAt"):
            detail = f"abilitato fino al {local_date(item.get('claimExpiresAt'))}"
        else:
            detail = "-"
        rows.append(
            [
                item.get("tableNumber"),
                item.get("tableDisplayName"),
                status,
                detail,
            ]
        )

    table(["N.", "TAVOLO", "STATO", "SESSIONE / DISPONIBILITA"], rows, [3, 14, 16, 64])
    print()
    active = counts.get("IN SESSIONE", 0)
    ready = counts.get("PRONTO", 0)
    disabled = counts.get("DISABILITATO", 0)
    summary = " · ".join(
        [
            f"{active} in sessione",
            f"{ready} {'pronto' if ready == 1 else 'pronti'}",
            f"{disabled} {'disabilitato' if disabled == 1 else 'disabilitati'}",
        ]
    )
    print(style(f"  {summary}", DIM))


def render_table_status(data: dict[str, Any]) -> None:
    status = normalized_table_status(data)
    color = GREEN if status == "PRONTO" else CYAN if status == "IN SESSIONE" else DIM
    field("Tavolo", data.get("tableDisplayName"))
    field("Identificativo QR", data.get("tablePublicId"))
    field("Stato", status, color)
    if data.get("claimExpiresAt"):
        field("Avvio consentito fino alle", local_date(data.get("claimExpiresAt")))
    session = data.get("activeSession")
    if session:
        section("Sessione attiva")
        render_active_session(session)


def render_active_session(data: dict[str, Any]) -> None:
    field("Titolo", data.get("title"))
    field("Sessione", data.get("sessionId"))
    field("Tavolo", data.get("tableDisplayName"))
    field("Gioco", data.get("gameType"))
    field("Descrizione", data.get("publicSummary"))


def render_created_session(data: dict[str, Any]) -> None:
    print(style("● Sessione creata correttamente", GREEN))
    field("Titolo", data.get("title"))
    field("Sessione", data.get("sessionId"))
    field("Tavolo", data.get("tableDisplayName"))
    field("Stato", data.get("status"), GREEN)
    field("Griglia", f"{text(data.get('gridWidth'))} × {text(data.get('gridHeight'))}")
    field("Creata il", local_date(data.get("createdAt")))
    token = data.get("dmAccessToken")
    if token:
        section("Credenziale DM")
        print(f"  {token}")
        print(style(f"  export BOARDHUB_DM_TOKEN='{token}'", DIM))


def render_join_request(data: dict[str, Any]) -> None:
    status = text(data.get("status"))
    color = YELLOW if status == "PENDING" else GREEN if status == "ACCEPTED" else RED
    field("Giocatore", data.get("displayName"))
    field("Riferimento dispositivo", data.get("playerReference"))
    field("Richiesta", data.get("requestId"))
    field("Sessione", data.get("sessionId"))
    field("Stato", status, color)
    field("Scadenza", local_date(data.get("expiresAt")))
    if data.get("resolvedAt"):
        field("Risolta il", local_date(data.get("resolvedAt")))


def render_join_requests(data: list[dict[str, Any]]) -> None:
    rows = [
        [
            item.get("displayName"),
            item.get("status"),
            item.get("requestId"),
            local_date(item.get("expiresAt")),
        ]
        for item in data
    ]
    table(["GIOCATORE", "STATO", "REQUEST ID", "SCADENZA"], rows, [18, 12, 38, 20])
    print(style(f"\n  Totale richieste: {len(data)}", DIM))


def render_join_resolution(data: dict[str, Any]) -> None:
    print(style("● Richiesta accettata", GREEN))
    request = data.get("request") or {}
    participant = data.get("participant") or {}
    field("Giocatore", participant.get("displayName") or request.get("displayName"))
    field("Partecipante", participant.get("participantId"))
    field("Ruolo", participant.get("role"))
    field("Stato", participant.get("status"), GREEN)
    token = data.get("accessToken")
    if token:
        section("Credenziale giocatore")
        print(f"  {token}")
        print(style(f"  export BOARDHUB_PLAYER_TOKEN='{token}'", DIM))


def render_participants(data: list[dict[str, Any]]) -> None:
    rows = [
        [
            item.get("displayName"),
            item.get("role"),
            item.get("status"),
            item.get("participantId"),
            local_date(item.get("joinedAt")),
        ]
        for item in data
    ]
    table(["NOME", "RUOLO", "STATO", "PARTICIPANT ID", "INGRESSO"], rows, [18, 10, 12, 38, 20])
    print(style(f"\n  Giocatori attivi: {len(data)}", DIM))


def character_row(item: dict[str, Any]) -> list[Any]:
    return [
        item.get("name"),
        f"{text(item.get('species'))} / {text(item.get('className'))}",
        item.get("level"),
        f"{text(item.get('hpCurrent'))}/{text(item.get('hpMax'))}",
        item.get("armorClass"),
        item.get("speedCells"),
        item.get("characterId"),
    ]


def render_character(data: dict[str, Any]) -> None:
    print(style("● Personaggio creato correttamente", GREEN))
    table(
        ["NOME", "SPECIE / CLASSE", "LV", "PF", "CA", "MOV.", "CHARACTER ID"],
        [character_row(data)],
        [16, 24, 4, 9, 4, 5, 38],
    )
    field("Visibilita", data.get("partyVisibility"))


def render_characters(data: list[dict[str, Any]]) -> None:
    table(
        ["NOME", "SPECIE / CLASSE", "LV", "PF", "CA", "MOV.", "CHARACTER ID"],
        [character_row(item) for item in data],
        [16, 24, 4, 9, 4, 5, 38],
    )
    print(style(f"\n  Totale personaggi: {len(data)}", DIM))


def render_closed_session(data: dict[str, Any]) -> None:
    print(style("● Sessione conclusa", GREEN))
    field("Sessione", data.get("sessionId"))
    field("Stato", data.get("status"), GREEN)
    field("Conclusa il", local_date(data.get("endedAt")))


def render_movement(data: dict[str, Any]) -> None:
    cells = data.get("reachableCells") or []
    rows = [
        [
            item.get("cell"),
            item.get("cost"),
            " → ".join(item.get("path") or []),
            ", ".join(item.get("trapsOnPath") or []) or "-",
        ]
        for item in cells
    ]
    field("Personaggio", data.get("characterId"))
    table(["CELLA", "COSTO", "PERCORSO", "TRAPPOLE NOTE"], rows, [8, 7, 48, 24])
    print(style(f"\n  Celle raggiungibili: {len(cells)}", DIM))


def event_detail(item: dict[str, Any]) -> str:
    payload = item.get("payload") or {}
    if item.get("eventType") == "MOVE":
        return (
            f"{text(payload.get('characterId'))}: "
            f"{text(payload.get('from'))} → {text(payload.get('to'))}"
        )
    return ", ".join(f"{key}={text(value)}" for key, value in payload.items()) or "-"


def render_events(data: list[dict[str, Any]]) -> None:
    rows = [
        [
            item.get("sequenceNumber"),
            item.get("eventType"),
            local_date(item.get("occurredAt")),
            event_detail(item),
            item.get("eventId"),
        ]
        for item in data
    ]
    table(["SEQ", "TIPO", "DATA", "DETTAGLIO", "EVENT ID"], rows, [12, 16, 20, 34, 42])
    print(style(f"\n  Eventi persistiti: {len(data)}", DIM))


def pretty_json(raw: str, data: Any) -> None:
    jq = shutil.which("jq")
    if jq:
        completed = subprocess.run([jq, "-C", "."], input=raw, text=True, check=False)
        if completed.returncode == 0:
            return
    print(json.dumps(data, ensure_ascii=False, indent=2))


def render(profile: str, data: Any) -> None:
    if isinstance(data, dict) and "code" in data and "message" in data:
        render_error(data)
        return

    renderers = {
        "health": render_health,
        "venue-tables": render_venue_tables,
        "table-status": render_table_status,
        "active-session": render_active_session,
        "created-session": render_created_session,
        "join-request": render_join_request,
        "join-requests": render_join_requests,
        "join-resolution": render_join_resolution,
        "participants": render_participants,
        "character": render_character,
        "characters": render_characters,
        "closed-session": render_closed_session,
        "movement": render_movement,
        "events": render_events,
    }
    renderer = renderers.get(profile)
    if renderer is None:
        print(json.dumps(data, ensure_ascii=False, indent=2))
        return
    renderer(data)


def main() -> int:
    profile = sys.argv[1] if len(sys.argv) > 1 else "json"
    raw = sys.stdin.read()
    if not raw.strip():
        return 0
    try:
        data = json.loads(raw)
    except json.JSONDecodeError:
        print(raw, end="" if raw.endswith("\n") else "\n")
        return 0

    if os.environ.get("BOARDHUB_OUTPUT", "summary").lower() == "json":
        pretty_json(raw, data)
    else:
        render(profile, data)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
