import type { ReactNode } from "react";
import type { GameEvent } from "../types";
import { formatDateTime, readPayloadText } from "../utils/events";

type EventLogProps = {
  events: GameEvent[];
  isLoading: boolean;
};

function eventTypeClassName(eventType: string) {
  return {
    SESSION_START: "border-cyan-200 bg-cyan-50 text-cyan-700",
    MOVE: "border-emerald-200 bg-emerald-50 text-emerald-700",
    SPAWN_MONSTER: "border-fuchsia-200 bg-fuchsia-50 text-fuchsia-700",
    ATTACK: "border-amber-200 bg-amber-50 text-amber-700",
    DAMAGE: "border-red-200 bg-red-50 text-red-700",
    ROUND_END: "border-blue-200 bg-blue-50 text-blue-700",
  }[eventType] ?? "border-slate-200 bg-slate-100 text-slate-700";
}

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
          <article className="rounded-md border border-slate-200 bg-white" key={event.eventId}>
            <div className="flex items-start justify-between gap-3 p-3">
              <div className="min-w-0">
                <div className="flex items-center gap-2">
                  <span className="font-mono text-xs text-slate-400">#{event.sequenceNumber}</span>
                  <span className={`rounded-md border px-2 py-0.5 text-xs ${eventTypeClassName(event.eventType)}`}>
                    {event.eventType}
                  </span>
                </div>
                <p className="mt-2 truncate text-sm font-medium text-slate-900">{describeEvent(event)}</p>
                <p className="mt-1 text-xs text-slate-500">{formatDateTime(event.occurredAt)}</p>
              </div>
              <span className="shrink-0 text-xs text-slate-500">{event.source}</span>
            </div>

            <details className="border-t border-slate-100 px-3 py-2">
              <summary className="cursor-pointer text-xs text-slate-500">Payload</summary>
              <code className="mt-2 block max-h-24 overflow-auto rounded-md bg-slate-50 p-2 font-mono text-xs text-slate-600">
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
              <TableCell className="font-mono">{event.sequenceNumber}</TableCell>
              <TableCell>
                <span
                  className={`inline-flex min-h-6 items-center rounded-md border px-2 py-0.5 text-xs font-normal ${eventTypeClassName(event.eventType)}`}
                >
                  {event.eventType}
                </span>
              </TableCell>
              <TableCell>{event.source}</TableCell>
              <TableCell className="hidden md:table-cell">{formatDateTime(event.occurredAt)}</TableCell>
              <TableCell className="wrap-break-word font-mono text-xs text-slate-600">
                {readPayloadText(event.payload)}
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
      className={`sticky top-0 z-10 border-b border-slate-200 bg-white px-3 py-2 text-left text-xs font-normal uppercase text-slate-500 ${className}`}
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
  return <td className={`border-b border-slate-100 px-3 py-2 align-top text-sm text-slate-700 ${className}`}>{children}</td>;
}
