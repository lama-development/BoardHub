import * as React from "react";
import {
  AlertCircle,
  Clock,
  LayoutDashboard,
  RefreshCw,
  Search,
  History,
  ClipboardList,
  Grid2X2,
} from "lucide-react";
import { fetchBackendHealth, fetchSessionEvents } from "./api/boardhubApi";
import { BackendStatus } from "./components/BackendStatus";
import { Metric } from "./components/Metric";
import { SessionInspector } from "./components/SessionInspector";
import { SessionTabs } from "./components/SessionTabs";
import { PublicTablePage } from "./components/PublicTablePage";
import { AlertBanner, Button, PageHeaderIdentity } from "./components/ui";
import { DEFAULT_SESSION_ID } from "./config";
import type { GameEvent, HealthStatus } from "./types";
import {
  formatDateTime,
  getBoardTokens,
  getEventTypes,
  sortEvents,
} from "./utils/events";

export function App() {
  const tableRoute = window.location.pathname.match(/^\/t\/([^/]+)\/?$/);
  if (tableRoute) {
    return (
      <PublicTablePage tablePublicId={decodeURIComponent(tableRoute[1])} />
    );
  }

  return <SessionMonitor />;
}

function SessionMonitor() {
  const [sessionId, setSessionId] = React.useState(DEFAULT_SESSION_ID);
  const [events, setEvents] = React.useState<GameEvent[]>([]);
  const [backendStatus, setBackendStatus] =
    React.useState<HealthStatus>("UNKNOWN");
  const [isLoading, setIsLoading] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [lastUpdatedAt, setLastUpdatedAt] = React.useState<string | null>(null);

  const sortedEvents = React.useMemo(() => sortEvents(events), [events]);
  const latestEvent = sortedEvents.at(-1);
  const eventTypes = React.useMemo(
    () => getEventTypes(sortedEvents),
    [sortedEvents],
  );
  const tokens = React.useMemo(
    () => getBoardTokens(sortedEvents),
    [sortedEvents],
  );

  const refreshHealth = React.useCallback(async () => {
    setBackendStatus(await fetchBackendHealth());
  }, []);

  const fetchEvents = React.useCallback(
    async (targetSessionId = sessionId) => {
      const normalizedSessionId = targetSessionId.trim();
      if (!normalizedSessionId) {
        setError("Inserisci un sessionId valido.");
        setEvents([]);
        return;
      }

      setIsLoading(true);
      setError(null);

      try {
        const [status, data] = await Promise.all([
          fetchBackendHealth(),
          fetchSessionEvents(normalizedSessionId),
        ]);
        setBackendStatus(status);
        setEvents(data);
        setLastUpdatedAt(new Date().toISOString());
      } catch (fetchError) {
        await refreshHealth();
        setEvents([]);
        setError(
          fetchError instanceof Error
            ? fetchError.message
            : "Errore inatteso durante il caricamento.",
        );
      } finally {
        setIsLoading(false);
      }
    },
    [refreshHealth, sessionId],
  );

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
    <main className="min-h-screen bg-transparent px-4 py-5 text-[#111111] sm:px-6 sm:py-7">
      <section className="bh-surface mx-auto flex w-full max-w-7xl flex-col gap-4 p-4 lg:flex-row lg:items-center lg:justify-between sm:p-5">
        <PageHeaderIdentity
          accentClassName="bg-[#ff3b9d]"
          eyebrow="BoardHub · Panoramica"
          icon={<LayoutDashboard size={20} />}
          title="Dashboard di gioco"
        />

        <div className="flex flex-col items-start gap-2 sm:flex-row sm:items-center">
          <BackendStatus status={backendStatus} />
          <form
            className="grid w-full gap-2 sm:w-auto sm:grid-cols-[minmax(240px,360px)_auto] sm:items-center"
            onSubmit={(event) => {
              event.preventDefault();
              void fetchEvents();
            }}
          >
            <label className="sr-only" htmlFor="sessionId">
              ID sessione
            </label>
            <div className="bh-input flex h-10 items-center gap-2 px-3">
              <Search
                className="shrink-0 text-slate-500"
                size={18}
                aria-hidden="true"
              />
              <input
                className="w-full min-w-0 border-0 bg-transparent text-slate-900 outline-none"
                id="sessionId"
                value={sessionId}
                onChange={(event) => setSessionId(event.target.value)}
                placeholder={DEFAULT_SESSION_ID}
              />
            </div>
            <Button
              variant="primary"
              type="submit"
              disabled={isLoading}
              title="Aggiorna eventi"
              icon={
                <RefreshCw
                  className={isLoading ? "animate-spin" : ""}
                  size={17}
                  aria-hidden="true"
                />
              }
            >
              <span>{isLoading ? "Aggiorno" : "Aggiorna"}</span>
            </Button>
          </form>
        </div>
      </section>

      <section className="mx-auto my-4 grid w-full max-w-7xl gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <Metric
          accent="pink"
          icon={<ClipboardList size={19} />}
          label="Eventi"
          value={String(sortedEvents.length)}
        />
        <Metric
          accent="cyan"
          icon={<Grid2X2 size={19} />}
          label="Pedine"
          value={String(tokens.length)}
        />
        <Metric
          accent="yellow"
          icon={<History size={19} />}
          label="Ultimo evento"
          value={latestEvent?.eventType ?? "-"}
        />
        <Metric
          accent="purple"
          icon={<Clock size={19} />}
          label="Aggiornato"
          value={lastUpdatedAt ? formatDateTime(lastUpdatedAt) : "-"}
        />
      </section>

      {error ? (
        <AlertBanner
          className="mx-auto mb-4 w-full max-w-7xl"
          tone="danger"
          icon={<AlertCircle size={18} />}
          role="alert"
        >
          <span>{error}</span>
        </AlertBanner>
      ) : null}

      <section className="mx-auto grid w-full max-w-7xl items-start gap-4 xl:grid-cols-[minmax(0,1fr)_370px]">
        <SessionTabs
          events={sortedEvents}
          isLoading={isLoading}
          tokens={tokens}
        />
        <SessionInspector
          latestEvent={latestEvent}
          eventTypes={eventTypes}
          tokens={tokens}
        />
      </section>
    </main>
  );
}
