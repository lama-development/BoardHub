import { Gauge } from "lucide-react";
import type { BoardToken, GameEvent } from "../types";
import { formatDateTime } from "../utils/events";
import { EventTypeChip } from "./EventTypeChip";
import { InfoBlock } from "./InfoBlock";

type SessionInspectorProps = {
  latestEvent?: GameEvent;
  eventTypes: Array<[string, number]>;
  tokens: BoardToken[];
};

export function SessionInspector({ latestEvent, eventTypes, tokens }: SessionInspectorProps) {
  return (
    <aside className="bh-surface overflow-hidden">
      <div className="flex min-h-14 items-center gap-3 border-b-2 border-[#111111] bg-[#ffd400] px-4 py-3">
        <span className="bh-card-icon"><Gauge size={18} /></span>
        <h2 className="text-xl">Riepilogo</h2>
      </div>

      <InfoBlock title="Ultimo evento">
        {latestEvent ? (
          <>
            <EventTypeChip className="mb-2" type={latestEvent.eventType} />
            <span className="block wrap-break-word text-slate-600">{formatDateTime(latestEvent.occurredAt)}</span>
            <code className="mt-2 block wrap-break-word font-[inherit] text-xs leading-relaxed text-slate-600">{latestEvent.eventId}</code>
          </>
        ) : (
          <span>-</span>
        )}
      </InfoBlock>

      <InfoBlock title="Tipi evento">
        {eventTypes.length > 0 ? (
          <div className="flex flex-wrap gap-2">
            {eventTypes.map(([type, count]) => (
              <EventTypeChip count={count} key={type} type={type} />
            ))}
          </div>
        ) : (
          <span>-</span>
        )}
      </InfoBlock>

      <InfoBlock title="Pedine note">
        {tokens.length > 0 ? (
          <div className="grid gap-2">
            {tokens.map((token) => (
              <span
                className="flex min-h-9 items-center justify-between gap-3 rounded-[2px] border-2 border-[#111111] bg-[#fbfaf6] px-3 py-2"
                key={token.id}
              >
                <b className="wrap-break-word font-medium">{token.id}</b>
                <code className="font-[inherit] text-sm font-bold text-slate-950">{token.cell}</code>
              </span>
            ))}
          </div>
        ) : (
          <span>-</span>
        )}
      </InfoBlock>
    </aside>
  );
}
