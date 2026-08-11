import type { ReactNode } from "react";
import type { GameEvent } from "../types";
import { formatDateTime, readPayloadText } from "../utils/events";
import { EventTypeChip } from "./EventTypeChip";

type EventLogProps = {
  events: GameEvent[];
  isLoading: boolean;
};

function describeEvent(event: GameEvent) {
  const payload = event.payload;

  if (event.eventType === "SESSION_START" && typeof payload.title === "string") {
    return payload.title;
  }
  if (
    event.eventType === "MOVE" &&
    typeof payload.characterId === "string" &&
    typeof payload.from === "string" &&
    typeof payload.to === "string"
  ) {
    return `${payload.characterId}: ${payload.from} -> ${payload.to}`;
  }
  if (event.eventType === "SPAWN_MONSTER" && typeof payload.monsterId === "string" && typeof payload.position === "string") {
    return `${payload.monsterId} in ${payload.position}`;
  }
  if (event.eventType === "ATTACK" && typeof payload.attackerId === "string" && typeof payload.targetId === "string") {
    return `${payload.attackerId} attacca ${payload.targetId}`;
  }
  if (event.eventType === "DAMAGE" && typeof payload.targetId === "string" && typeof payload.amount === "number") {
    return `${payload.targetId} subisce ${payload.amount} danni`;
  }
  if (event.eventType === "ROUND_END" && typeof payload.round === "number") {
    return `Fine round ${payload.round}`;
  }

  return event.source;
}

export function EventLog({ events, isLoading }: EventLogProps) {
  if (!isLoading && events.length === 0) {
    return (
      <div className="flex min-h-56 items-center justify-center px-4 text-sm font-normal text-slate-500">
        Nessun evento salvato per questa sessione.
      </div>
    );
  }

  return (
    <div className="max-h-[360px] overflow-auto">
      <div className="grid gap-2 p-3 sm:hidden">
        {events.map((event) => (
          <article className="rounded-[2px] border-2 border-[#111111] bg-white" key={event.eventId}>
            <div className="flex items-start justify-between gap-3 p-3">
              <div className="min-w-0">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-bold text-slate-500">#{event.sequenceNumber}</span>
                  <EventTypeChip type={event.eventType} />
                </div>
                <p className="mt-2 truncate text-sm font-medium text-slate-900">{describeEvent(event)}</p>
                <p className="mt-1 text-xs text-slate-500">{formatDateTime(event.occurredAt)}</p>
              </div>
              <span className="shrink-0 text-xs text-slate-500">{event.source}</span>
            </div>

            <details className="border-t border-slate-100 px-3 py-2">
              <summary className="cursor-pointer text-xs text-slate-500">Payload</summary>
              <code className="bh-code-block mt-2 block max-h-24 overflow-auto p-2.5 text-xs leading-relaxed text-slate-700">
                {readPayloadText(event.payload)}
              </code>
            </details>
          </article>
        ))}
      </div>

      <table className="hidden w-full table-fixed border-collapse sm:table">
        <thead>
          <tr>
            <TableHead className="w-14">Seq</TableHead>
            <TableHead className="w-36">Tipo</TableHead>
            <TableHead className="w-32">Sorgente</TableHead>
            <TableHead className="hidden w-44 md:table-cell">Ora</TableHead>
            <TableHead>Payload</TableHead>
          </tr>
        </thead>
        <tbody>
          {events.map((event) => (
            <tr key={event.eventId}>
              <TableCell className="font-bold">{event.sequenceNumber}</TableCell>
              <TableCell>
                <EventTypeChip type={event.eventType} />
              </TableCell>
              <TableCell>{event.source}</TableCell>
              <TableCell className="hidden md:table-cell">{formatDateTime(event.occurredAt)}</TableCell>
              <TableCell>
                <code className="bh-code-block block max-h-24 overflow-auto p-2.5 text-xs leading-relaxed text-slate-700">
                  {readPayloadText(event.payload)}
                </code>
              </TableCell>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function TableHead({
  children,
  className = "",
}: {
  children: ReactNode;
  className?: string;
}) {
  return (
    <th
      className={`sticky top-0 z-10 border-b-2 border-[#111111] bg-[#ffd400] px-3 py-2 text-left text-xs font-extrabold uppercase tracking-wide text-[#111111] ${className}`}
    >
      {children}
    </th>
  );
}

function TableCell({
  children,
  className = "",
}: {
  children: ReactNode;
  className?: string;
}) {
  return <td className={`border-b border-[#111111] px-3 py-2 align-top text-sm text-slate-700 ${className}`}>{children}</td>;
}
