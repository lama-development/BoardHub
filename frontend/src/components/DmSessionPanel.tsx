import * as React from "react";
import {
  AlertCircle,
  Check,
  Clock3,
  LogOut,
  RefreshCw,
  Shield,
  UserRoundCheck,
  Users,
  X,
} from "lucide-react";
import {
  acceptDmJoinRequest,
  fetchDmJoinRequests,
  fetchDmParticipants,
  rejectDmJoinRequest,
} from "../api/boardhubApi";
import type { JoinRequest, Participant } from "../types";

type DmSessionPanelProps = {
  sessionId: string;
  sessionTitle: string;
  tableDisplayName: string;
  dmKey: string;
  onLeave: () => void;
};

export function DmSessionPanel({
  sessionId,
  sessionTitle,
  tableDisplayName,
  dmKey,
  onLeave,
}: DmSessionPanelProps) {
  const [requests, setRequests] = React.useState<JoinRequest[]>([]);
  const [participants, setParticipants] = React.useState<Participant[]>([]);
  const [error, setError] = React.useState<string | null>(null);
  const [isLoading, setIsLoading] = React.useState(true);
  const [busyRequestId, setBusyRequestId] = React.useState<string | null>(null);

  const refresh = React.useCallback(async (showLoading = true) => {
    if (showLoading) setIsLoading(true);
    try {
      const [pendingRequests, activeParticipants] = await Promise.all([
        fetchDmJoinRequests(sessionId, dmKey),
        fetchDmParticipants(sessionId, dmKey),
      ]);
      setRequests(pendingRequests);
      setParticipants(activeParticipants);
      setError(null);
    } catch (refreshError) {
      setError(
        refreshError instanceof Error
          ? refreshError.message
          : "Impossibile aggiornare la console DM.",
      );
    } finally {
      if (showLoading) setIsLoading(false);
    }
  }, [dmKey, sessionId]);

  React.useEffect(() => {
    void refresh();
    const intervalId = window.setInterval(() => void refresh(false), 4000);
    return () => window.clearInterval(intervalId);
  }, [refresh]);

  async function resolveRequest(requestId: string, accepted: boolean) {
    setBusyRequestId(requestId);
    setError(null);
    try {
      if (accepted) {
        await acceptDmJoinRequest(sessionId, requestId, dmKey);
      } else {
        await rejectDmJoinRequest(sessionId, requestId, dmKey);
      }
      await refresh(false);
    } catch (resolveError) {
      setError(
        resolveError instanceof Error
          ? resolveError.message
          : "Impossibile gestire la richiesta.",
      );
    } finally {
      setBusyRequestId(null);
    }
  }

  return (
    <main className="min-h-screen bg-slate-50 px-4 py-5 text-slate-900 sm:px-6 sm:py-8">
      <header className="mx-auto flex w-full max-w-240 flex-col gap-4 border-b border-slate-200 pb-5 sm:flex-row sm:items-end sm:justify-between">
        <div className="flex items-start gap-3">
          <span className="grid h-10 w-10 shrink-0 place-items-center rounded-md bg-slate-900 text-white">
            <Shield size={20} aria-hidden="true" />
          </span>
          <div>
            <p className="text-xs text-slate-500">Console Dungeon Master · {tableDisplayName}</p>
            <h1 className="mt-1 text-2xl font-semibold text-slate-950">{sessionTitle}</h1>
            <p className="mt-1 text-xs text-slate-500">{sessionId}</p>
          </div>
        </div>
        <div className="flex gap-2">
          <button
            className="inline-flex h-9 cursor-pointer items-center gap-2 rounded-md border border-slate-300 bg-white px-3 text-sm font-medium hover:bg-slate-100"
            type="button"
            onClick={() => void refresh()}
            disabled={isLoading}
          >
            <RefreshCw className={isLoading ? "animate-spin" : ""} size={17} aria-hidden="true" />
            Aggiorna
          </button>
          <button
            className="inline-flex h-9 cursor-pointer items-center gap-2 rounded-md border border-slate-300 bg-white px-3 text-sm font-medium hover:bg-slate-100"
            type="button"
            onClick={onLeave}
          >
            <LogOut size={17} aria-hidden="true" />
            Esci dal pannello
          </button>
        </div>
      </header>

      <section className="mx-auto grid w-full max-w-240 gap-3 border-b border-slate-200 py-5 sm:grid-cols-3">
        <div>
          <p className="text-xs font-medium uppercase text-slate-500">Stato</p>
          <p className="mt-1 text-lg font-semibold text-emerald-700">Sessione attiva</p>
        </div>
        <div>
          <p className="text-xs font-medium uppercase text-slate-500">Richieste in attesa</p>
          <p className="mt-1 text-lg font-semibold text-slate-950">{requests.length}</p>
        </div>
        <div>
          <p className="text-xs font-medium uppercase text-slate-500">Giocatori attivi</p>
          <p className="mt-1 text-lg font-semibold text-slate-950">
            {participants.length} / 8
          </p>
        </div>
      </section>

      {error ? (
        <section className="mx-auto mt-4 flex w-full max-w-240 items-start gap-2 border-l-4 border-red-500 bg-white px-4 py-3 text-sm text-red-700" role="alert">
          <AlertCircle className="mt-0.5 shrink-0" size={18} aria-hidden="true" />
          <span>{error}</span>
        </section>
      ) : null}

      <section className="mx-auto grid w-full max-w-240 gap-8 py-7 lg:grid-cols-2">
        <div>
          <div className="flex items-center gap-2">
            <Clock3 size={19} aria-hidden="true" />
            <h2 className="text-lg font-semibold text-slate-950">Richieste di partecipazione</h2>
          </div>
          {requests.length === 0 ? (
            <p className="mt-4 border-t border-slate-200 py-5 text-sm text-slate-500">
              Nessuna richiesta in attesa. La lista viene aggiornata automaticamente.
            </p>
          ) : (
            <ul className="mt-4 divide-y divide-slate-200 border-y border-slate-200">
              {requests.map((request) => (
                <li className="flex flex-col gap-3 py-4 sm:flex-row sm:items-center sm:justify-between" key={request.requestId}>
                  <div>
                    <p className="font-medium text-slate-950">{request.displayName}</p>
                    <p className="mt-1 text-xs text-slate-500">{request.playerReference}</p>
                  </div>
                  <div className="flex gap-2">
                    <button
                      className="inline-flex h-9 cursor-pointer items-center gap-2 rounded-md bg-emerald-700 px-3 text-sm font-medium text-white hover:bg-emerald-600 disabled:cursor-progress disabled:opacity-60"
                      type="button"
                      disabled={busyRequestId === request.requestId}
                      onClick={() => void resolveRequest(request.requestId, true)}
                    >
                      <Check size={17} aria-hidden="true" />
                      Accetta
                    </button>
                    <button
                      className="inline-flex h-9 cursor-pointer items-center gap-2 rounded-md border border-slate-300 bg-white px-3 text-sm font-medium hover:bg-slate-100 disabled:cursor-progress disabled:opacity-60"
                      type="button"
                      disabled={busyRequestId === request.requestId}
                      onClick={() => void resolveRequest(request.requestId, false)}
                    >
                      <X size={17} aria-hidden="true" />
                      Rifiuta
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div>
          <div className="flex items-center gap-2">
            <Users size={19} aria-hidden="true" />
            <h2 className="text-lg font-semibold text-slate-950">Giocatori accettati</h2>
          </div>
          {participants.length === 0 ? (
            <p className="mt-4 border-t border-slate-200 py-5 text-sm text-slate-500">
              Non ci sono ancora giocatori associati alla sessione.
            </p>
          ) : (
            <ul className="mt-4 divide-y divide-slate-200 border-y border-slate-200">
              {participants.map((participant) => (
                <li className="flex items-center gap-3 py-4" key={participant.participantId}>
                  <UserRoundCheck className="text-emerald-700" size={20} aria-hidden="true" />
                  <div>
                    <p className="font-medium text-slate-950">{participant.displayName}</p>
                    <p className="mt-1 text-xs text-slate-500">{participant.status}</p>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>
    </main>
  );
}
