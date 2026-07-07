import type { BoardToken, GameEvent } from "../types";

export function formatDateTime(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("it-IT", {
    dateStyle: "short",
    timeStyle: "medium",
  }).format(date);
}

export function readPayloadText(payload: Record<string, unknown>) {
  const compact = JSON.stringify(payload);
  return compact === "{}" ? "-" : compact;
}

export function sortEvents(events: GameEvent[]) {
  return [...events].sort((left, right) => left.sequenceNumber - right.sequenceNumber);
}

export function getEventTypes(events: GameEvent[]) {
  const counts = new Map<string, number>();

  for (const event of events) {
    counts.set(event.eventType, (counts.get(event.eventType) ?? 0) + 1);
  }

  return [...counts.entries()].sort(([left], [right]) => left.localeCompare(right));
}

export function getBoardTokens(events: GameEvent[]): BoardToken[] {
  const tokens = new Map<string, BoardToken>();

  for (const event of events) {
    const characterId = event.payload.characterId;
    const destination = event.payload.to;
    if (typeof characterId === "string" && typeof destination === "string") {
      tokens.set(characterId, {
        id: characterId,
        cell: destination,
        kind: "character",
      });
    }

    const monsterId = event.payload.monsterId;
    const monsterCell = event.payload.position ?? event.payload.cell ?? event.payload.at;
    if (typeof monsterId === "string" && typeof monsterCell === "string") {
      tokens.set(monsterId, {
        id: monsterId,
        cell: monsterCell,
        kind: "monster",
      });
    }
  }

  return [...tokens.values()].sort((left, right) => left.id.localeCompare(right.id));
}
