import * as React from "react";
import { AlertCircle, Clock, LayoutDashboard, RefreshCw, Search, History, ClipboardList, Grid2X2 } from "lucide-react";
import { fetchBackendHealth, fetchSessionEvents } from "./api/boardhubApi";
import { BackendStatus } from "./components/BackendStatus";
import { Metric } from "./components/Metric";
import { SessionInspector } from "./components/SessionInspector";
import { SessionTabs } from "./components/SessionTabs";
import { DEFAULT_SESSION_ID } from "./config";
import type { GameEvent, HealthStatus } from "./types";
import { formatDateTime, getBoardTokens, getEventTypes, sortEvents } from "./utils/events";

export function App() {
  const [sessionId, setSessionId] = React.useState(DEFAULT_SESSION_ID);
  const [events, setEvents] = React.useState<GameEvent[]>([]);
  const [backendStatus, setBackendStatus] = React.useState<HealthStatus>("UNKNOWN");
  const [isLoading, setIsLoading] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [lastUpdatedAt, setLastUpdatedAt] = React.useState<string | null>(null);

  const sortedEvents = React.useMemo(() => sortEvents(events), [events]);
  const latestEvent = sortedEvents.at(-1);
  const eventTypes = React.useMemo(() => getEventTypes(sortedEvents), [sortedEvents]);
  const tokens = React.useMemo(() => getBoardTokens(sortedEvents), [sortedEvents]);

  const refreshHealth = React.useCallback(async () => {
    setBackendStatus(await fetchBackendHealth());
  }, []);

  const fetchEvents = React.useCallback(async (targetSessionId = sessionId) => {
    const normalizedSessionId = targetSessionId.trim();
    if (!normalizedSessionId) {
      setError("Inserisci un sessionId valido.");
      setEvents([]);
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const [status, data] = await Promise.all([fetchBackendHealth(), fetchSessionEvents(normalizedSessionId)]);
      setBackendStatus(status);
      setEvents(data);
      setLastUpdatedAt(new Date().toISOString());
    } catch (fetchError) {
      await refreshHealth();
      setEvents([]);
      setError(fetchError instanceof Error ? fetchError.message : "Errore inatteso durante il caricamento.");
    } finally {
      setIsLoading(false);
    }
  }, [refreshHealth, sessionId]);

  React.useEffect(() => {
    void fetchEvents(DEFAULT_SESSION_ID);
  }, []);

  React.useEffect(() => {
    const intervalId = window.setInterval(() => {
      void refreshHealth();
    }, 5000);

    return () => window.clearInterval(intervalId);
  }, [refreshHealth]);

  return (
    <main className="min-h-screen bg-slate-50 px-3 py-4 text-slate-900 sm:px-6">
      <section className="mx-auto flex w-full max-w-295 flex-col gap-4 border-b border-slate-200 pb-4 lg:flex-row lg:items-end lg:justify-between">
        <div className="flex items-center gap-3">
          <span className="grid h-10 w-10 shrink-0 place-items-center rounded-md border border-slate-200 bg-white text-slate-700">
            <LayoutDashboard size={20} aria-hidden="true" />
          </span>
          <div className="flex h-10 flex-col justify-center">
            <p className="text-[11px] font-normal leading-none text-slate-500">BoardHub</p>
            <h1 className="mt-1 text-xl font-medium leading-none text-slate-950">Session Monitor</h1>
          </div>
        </div>

        <div className="flex flex-col items-start gap-2 sm:flex-row sm:items-center">
          <BackendStatus status={backendStatus} />
          <form
            className="grid w-full gap-2 sm:w-auto sm:grid-cols-[auto_minmax(240px,360px)_auto] sm:items-center"
            onSubmit={(event) => {
              event.preventDefault();
              void fetchEvents();
            }}
          >
            <label className="text-sm font-normal text-slate-600" htmlFor="sessionId">
              Sessione
            </label>
            <div className="flex h-9 items-center gap-2 rounded-md border border-slate-300 bg-white px-3">
              <Search className="shrink-0 text-slate-500" size={18} aria-hidden="true" />
              <input
                className="w-full min-w-0 border-0 bg-transparent text-slate-900 outline-none"
                id="sessionId"
                value={sessionId}
                onChange={(event) => setSessionId(event.target.value)}
                placeholder={DEFAULT_SESSION_ID}
              />
            </div>
            <button
              className="inline-flex h-9 cursor-pointer items-center justify-center gap-2 rounded-md bg-slate-900 px-3 text-sm font-medium text-white transition-colors hover:bg-slate-700 disabled:cursor-progress disabled:opacity-60"
              type="submit"
              disabled={isLoading}
              title="Aggiorna eventi"
            >
              <RefreshCw size={18} aria-hidden="true" />
              <span>{isLoading ? "Aggiorno" : "Aggiorna"}</span>
            </button>
          </form>
        </div>
      </section>

      <section className="mx-auto my-3 grid w-full max-w-295 gap-2 sm:grid-cols-2 xl:grid-cols-4">
        <Metric icon={<ClipboardList size={19} />} label="Eventi" value={String(sortedEvents.length)} />
        <Metric icon={<Grid2X2 size={19} />} label="Pedine" value={String(tokens.length)} />
        <Metric icon={<History size={19} />} label="Ultimo evento" value={latestEvent?.eventType ?? "-"} />
        <Metric
          icon={<Clock size={19} />}
          label="Aggiornato"
          value={lastUpdatedAt ? formatDateTime(lastUpdatedAt) : "-"}
        />
      </section>

      {error ? (
        <section
          className="mx-auto mb-3 flex min-h-10 w-full max-w-295 items-center gap-2 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm font-normal text-red-700"
          role="alert"
        >
          <AlertCircle size={20} aria-hidden="true" />
          <span>{error}</span>
        </section>
      ) : null}

      <section className="mx-auto grid w-full max-w-295 items-start gap-3 xl:grid-cols-[minmax(0,1fr)_340px]">
        <SessionTabs events={sortedEvents} isLoading={isLoading} tokens={tokens} />
        <SessionInspector latestEvent={latestEvent} eventTypes={eventTypes} tokens={tokens} />
      </section>
    </main>
  );
}
