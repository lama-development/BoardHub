import type { BoardToken, GameEvent } from "../types";
import { formatDateTime } from "../utils/events";
import { InfoBlock } from "./InfoBlock";

type SessionInspectorProps = {
  latestEvent?: GameEvent;
  eventTypes: Array<[string, number]>;
  tokens: BoardToken[];
};

export function SessionInspector({ latestEvent, eventTypes, tokens }: SessionInspectorProps) {
  return (
    <aside className="overflow-hidden rounded-md border border-slate-200 bg-white">
      <div className="flex min-h-12 items-center justify-between gap-3 border-b border-slate-200 px-4 py-3">
        <h2 className="text-base font-medium text-slate-950">Stato</h2>
      </div>

      <InfoBlock title="Ultimo evento">
        {latestEvent ? (
          <>
            <strong className="block wrap-break-word font-medium">{latestEvent.eventType}</strong>
            <span className="block wrap-break-word text-slate-600">{formatDateTime(latestEvent.occurredAt)}</span>
            <code className="mt-2 block wrap-break-word font-mono text-xs text-slate-600">{latestEvent.eventId}</code>
          </>
        ) : (
          <span>-</span>
        )}
      </InfoBlock>

      <InfoBlock title="Tipi evento">
        {eventTypes.length > 0 ? (
          <div className="flex flex-wrap gap-2">
            {eventTypes.map(([type, count]) => (
              <span
                className="inline-flex min-h-6 items-center gap-2 rounded-md border border-slate-200 bg-white px-2 py-0.5 text-xs font-normal text-slate-700"
                key={type}
              >
                {type} <b className="font-medium text-slate-950">{count}</b>
              </span>
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
                className="flex min-h-8 items-center justify-between gap-3 rounded-md bg-slate-50 px-2.5 py-1.5"
                key={token.id}
              >
                <b className="wrap-break-word font-medium">{token.id}</b>
                <code className="font-mono text-sm font-medium text-slate-950">{token.cell}</code>
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
