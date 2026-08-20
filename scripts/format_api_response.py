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


def render_health(
    data: dict[str, Any],
    address: str = "http://localhost:8082",
    service_name: str = "Backend",
) -> None:
    status = text(data.get("status"))
    color = GREEN if status == "UP" else RED
    state = style("operativo", color) if status == "UP" else style(status, color)
    print(f"{style('●', color)} {service_name} {state}")
    field("Indirizzo", address)


def render_stats_health(data: dict[str, Any]) -> None:
    render_health(data, "http://localhost:8083", "Servizio statistiche")


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


def piece_row(item: dict[str, Any]) -> list[Any]:
    return [
        item.get("currentCell"),
        item.get("representationMode"),
        item.get("controlMode"),
        item.get("characterId"),
        item.get("sessionPieceId"),
        item.get("version"),
    ]


def render_piece(data: dict[str, Any]) -> None:
    print(style("● Pedina virtuale posizionata", GREEN))
    table(
        ["CELLA", "MODALITA", "CONTROLLO", "CHARACTER ID", "PIECE ID", "VER."],
        [piece_row(data)],
        [8, 10, 18, 38, 38, 5],
    )


def render_pieces(data: list[dict[str, Any]]) -> None:
    table(
        ["CELLA", "MODALITA", "CONTROLLO", "CHARACTER ID", "PIECE ID", "VER."],
        [piece_row(item) for item in data],
        [8, 10, 18, 38, 38, 5],
    )
    print(style(f"\n  Totale pedine: {len(data)}", DIM))


def movement_rows(cells: list[dict[str, Any]]) -> list[list[Any]]:
    return [
        [
            item.get("cell"),
            item.get("cost"),
            " → ".join(item.get("path") or []),
            ", ".join(item.get("trapsOnPath") or []) or "-",
        ]
        for item in cells
    ]


def render_piece_reachability(data: dict[str, Any]) -> None:
    cells = data.get("reachableCells") or []
    field("Pedina", data.get("sessionPieceId"))
    field("Posizione corrente", data.get("currentCell"), CYAN)
    field("Punti movimento", data.get("movementPoints"))
    field("Versione", data.get("version"))
    table(
        ["CELLA", "COSTO", "PERCORSO", "TRAPPOLE NOTE"],
        movement_rows(cells),
        [8, 7, 48, 24],
    )
    print(style(f"\n  Celle raggiungibili: {len(cells)}", DIM))


def render_piece_move(data: dict[str, Any]) -> None:
    status = text(data.get("status"))
    pending = status == "TRAP_PENDING"
    print(style("● Movimento interrotto da una trappola" if pending else "● Movimento confermato", YELLOW if pending else GREEN))
    field("Stato", status, YELLOW if pending else GREEN)
    field("Pedina", data.get("sessionPieceId"))
    field("Spostamento", f"{text(data.get('from'))} → {text(data.get('to'))}", CYAN)
    field("Percorso", " → ".join(data.get("path") or []))
    field("Costo", data.get("cost"))
    field("Nuova versione", data.get("version"))
    field("Trappole note", ", ".join(data.get("visibleTrapsOnPath") or []) or "-")
    field("Command ID", data.get("commandId"))
    field("Event ID", data.get("eventId"))
    if data.get("resolutionId"):
        section("Risoluzione richiesta")
        field("Resolution ID", data.get("resolutionId"), YELLOW)
        field("Destinazione richiesta", data.get("requestedDestination"))
        field("Movimento rimanente", data.get("movementRemaining"))


def render_trap_resolution(data: dict[str, Any]) -> None:
    field("Risoluzione", data.get("resolutionId"))
    field("Stato", data.get("status"), YELLOW)
    field("Pedina", data.get("sessionPieceId"))
    field("Personaggio", data.get("characterId"))
    field("Destinazione richiesta", data.get("requestedDestination"))
    field("Cella di attivazione", data.get("triggerCell"), CYAN)
    field("Movimento rimanente", data.get("movementRemaining"))
    field("Versione", data.get("version"))


def render_trap_resolutions(data: list[dict[str, Any]]) -> None:
    rows = [
        [
            item.get("status"),
            item.get("triggerCell"),
            item.get("requestedDestination"),
            item.get("movementRemaining"),
            item.get("resolutionId"),
            item.get("version"),
        ]
        for item in data
    ]
    table(
        ["STATO", "TRIGGER", "DESTINAZIONE", "MOV.", "RESOLUTION ID", "VER."],
        rows,
        [18, 9, 14, 6, 38, 5],
    )
    print(style(f"\n  Risoluzioni in attesa: {len(data)}", DIM))


def render_trap_roll(data: dict[str, Any]) -> None:
    print(style("● Tiro salvezza risolto", GREEN))
    field("Risoluzione", data.get("resolutionId"))
    dice = [data.get("d20First")]
    if data.get("d20Second") is not None:
        dice.append(data.get("d20Second"))
    field("Dadi d20", ", ".join(text(value) for value in dice))
    field("Dado selezionato", data.get("selectedD20"))
    field("Bonus salvezza", data.get("saveBonus"))
    field("Totale", data.get("saveTotal"))
    field("Esito", "SUCCESSO" if data.get("success") else "FALLIMENTO", GREEN if data.get("success") else RED)
    field("Dadi danno", ", ".join(text(value) for value in data.get("damageRolls") or []) or "-")
    field("Danno totale", data.get("damageTotal"))
    field("Punti ferita rimasti", data.get("hpCurrent"))
    field("Stato tattico", data.get("tacticalStatus"))
    field("Decisione movimento", data.get("movementDecision"))
    field("Movimento rimanente", data.get("movementRemaining"))
    field("Versione", data.get("version"))


def render_character_control(data: dict[str, Any]) -> None:
    print(style("● Controllo temporaneo assegnato al DM", GREEN))
    field("Sessione", data.get("sessionId"))
    field("Personaggio", data.get("characterId"))
    field("DM", data.get("dmParticipantId"))
    field("Versione controllo", data.get("version"))
    field("Assunto il", local_date(data.get("assumedAt")))


def render_closed_session(data: dict[str, Any]) -> None:
    print(style("● Sessione conclusa", GREEN))
    field("Sessione", data.get("sessionId"))
    field("Stato", data.get("status"), GREEN)
    field("Conclusa il", local_date(data.get("endedAt")))


def render_movement(data: dict[str, Any]) -> None:
    cells = data.get("reachableCells") or []
    field("Personaggio", data.get("characterId"))
    table(
        ["CELLA", "COSTO", "PERCORSO", "TRAPPOLE NOTE"],
        movement_rows(cells),
        [8, 7, 48, 24],
    )
    print(style(f"\n  Celle raggiungibili: {len(cells)}", DIM))


def event_detail(item: dict[str, Any]) -> str:
    payload = item.get("payload") or {}
    if item.get("eventType") in {"MOVE", "MOVE_CONFIRMED"}:
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


def render_session_result(data: dict[str, Any]) -> None:
    print(style(f"\n  {text(data.get('title'))}", BOLD))
    print(style(
        f"  {text(data.get('sessionId'))}  ·  {text(data.get('gameType'))}"
        f"  ·  {text(data.get('durationMinutes'))} minuti",
        DIM,
    ))
    rows = [
        [
            item.get("displayName"),
            f"{text(item.get('characterName'))} ({text(item.get('className'))})",
            "si" if item.get("survived") else "no",
            item.get("movesConfirmed"),
            item.get("cellsTravelled"),
            f"{text(item.get('savesSucceeded'))}/{text(item.get('savesFailed'))}",
            item.get("points"),
        ]
        for item in data.get("participants", [])
    ]
    table(
        ["GIOCATORE", "PERSONAGGIO", "VIVO", "MOSSE", "CELLE", "TS OK/KO", "PUNTI"],
        rows,
        [14, 26, 6, 7, 7, 10, 7],
    )


def render_session_results(data: list[dict[str, Any]]) -> None:
    rows = [
        [
            item.get("title"),
            item.get("sessionId"),
            item.get("gameType"),
            item.get("durationMinutes"),
            local_date(item.get("endedAt")),
        ]
        for item in data
    ]
    table(["TITOLO", "SESSIONE", "GIOCO", "MINUTI", "CONCLUSA"], rows, [24, 32, 8, 8, 20])
    print(style(f"\n  Sessioni concluse: {len(data)}", DIM))


def render_player_statistics(data: dict[str, Any]) -> None:
    print(style(f"\n  {text(data.get('displayName'))}", BOLD))
    print(style(f"  {text(data.get('playerReference'))}", DIM))
    table(
        ["PARTITE", "SOPRAVVISSUTO", "% SOPRAVV.", "PUNTI", "CELLE", "TS OK", "DANNI"],
        [[
            data.get("sessionsPlayed"),
            data.get("sessionsSurvived"),
            data.get("survivalRate"),
            data.get("totalPoints"),
            data.get("totalCellsTravelled"),
            data.get("totalSavesSucceeded"),
            data.get("totalDamageTaken"),
        ]],
        [9, 15, 12, 8, 8, 8, 8],
    )


def render_tournament(data: dict[str, Any]) -> None:
    print(style(f"\n  {text(data.get('name'))}", BOLD))
    print(style(
        f"  {text(data.get('tournamentId'))}  ·  {text(data.get('gameType'))}"
        f"  ·  {text(data.get('venueId'))}",
        DIM,
    ))


def render_tournaments(data: list[dict[str, Any]]) -> None:
    rows = [
        [item.get("name"), item.get("gameType"), item.get("venueId"), item.get("tournamentId")]
        for item in data
    ]
    table(["NOME", "GIOCO", "LOCALE", "TOURNAMENT ID"], rows, [24, 8, 12, 38])
    print(style(f"\n  Tornei: {len(data)}", DIM))


def render_leaderboard(data: list[dict[str, Any]]) -> None:
    rows = [
        [
            item.get("position"),
            item.get("displayName"),
            item.get("sessionsPlayed"),
            item.get("points"),
            item.get("savesSucceeded"),
            item.get("cellsTravelled"),
            item.get("timesDowned"),
        ]
        for item in data
    ]
    table(
        ["POS", "GIOCATORE", "PARTITE", "PUNTI", "TS OK", "CELLE", "ABBATTUTO"],
        rows,
        [5, 16, 8, 7, 7, 7, 10],
    )
    print(style("\n  Punteggio: sessione +3, sopravvissuto +2, tiro salvezza +1, abbattuto -1", DIM))


def render(profile: str, data: Any) -> None:
    if isinstance(data, dict) and "code" in data and "message" in data:
        render_error(data)
        return

    renderers = {
        "health": render_health,
        "stats-health": render_stats_health,
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
        "piece": render_piece,
        "pieces": render_pieces,
        "piece-reachability": render_piece_reachability,
        "piece-move": render_piece_move,
        "trap-resolution": render_trap_resolution,
        "trap-resolutions": render_trap_resolutions,
        "trap-roll": render_trap_roll,
        "character-control": render_character_control,
        "closed-session": render_closed_session,
        "movement": render_movement,
        "events": render_events,
        "session-result": render_session_result,
        "session-results": render_session_results,
        "player-statistics": render_player_statistics,
        "tournament": render_tournament,
        "tournaments": render_tournaments,
        "leaderboard": render_leaderboard,
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
