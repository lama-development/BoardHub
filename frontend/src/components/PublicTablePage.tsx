import * as React from "react";
import {
  AlertCircle,
  ArrowLeft,
  CheckCircle2,
  Clock3,
  Dices,
  DoorOpen,
  Eye,
  EyeOff,
  KeyRound,
  LoaderCircle,
  QrCode,
  RefreshCw,
  Shield,
  Users,
  XCircle,
} from "lucide-react";
import {
  createTableSession,
  fetchDmJoinRequests,
  fetchPlayerJoinStatus,
  fetchPublicTable,
  requestSessionJoin,
} from "../api/boardhubApi";
import type { CreatedSession, JoinRequest, PublicTableStatus } from "../types";
import {
  clearDmAccess,
  readDmAccess,
  saveDmAccess,
} from "../utils/dmSessionStorage";
import {
  clearPlayerJoinAccess,
  type PlayerJoinAccess,
  readPlayerJoinAccess,
  savePlayerJoinAccess,
} from "../utils/playerJoinStorage";
import { createUuid } from "../utils/uuid";
import { DmSessionPanel } from "./DmSessionPanel";

type PublicTablePageProps = {
  tablePublicId: string;
};

function leavePage() {
  if (window.history.length > 1) {
    window.history.back();
  } else {
    window.location.assign("/");
  }
}

function playerReference(tablePublicId: string) {
  const key = `boardhub-player-${tablePublicId}`;
  const existing = window.localStorage.getItem(key);
  if (existing) {
    return existing;
  }
  const created = `web-${createUuid()}`;
  window.localStorage.setItem(key, created);
  return created;
}

export function PublicTablePage({ tablePublicId }: PublicTablePageProps) {
  const dmKeyInputRef = React.useRef<HTMLInputElement>(null);
  const [table, setTable] = React.useState<PublicTableStatus | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const [isLoading, setIsLoading] = React.useState(true);
  const [showDmForm, setShowDmForm] = React.useState(false);
  const [showDmAccessForm, setShowDmAccessForm] = React.useState(false);
  const [showDmKey, setShowDmKey] = React.useState(false);
  const [dmKey, setDmKey] = React.useState("");
  const [dmSession, setDmSession] = React.useState<CreatedSession | null>(null);
  const [sessionTitle, setSessionTitle] = React.useState("Nuova avventura");
  const [playerName, setPlayerName] = React.useState("");
  const [joinRequest, setJoinRequest] = React.useState<JoinRequest | null>(null);
  const [playerJoinAccess, setPlayerJoinAccess] = React.useState<PlayerJoinAccess | null>(null);
  const [isSubmitting, setIsSubmitting] = React.useState(false);

  const refreshPlayerJoin = React.useCallback(async (access: PlayerJoinAccess) => {
    const result = await fetchPlayerJoinStatus(
      access.sessionId,
      access.request.requestId,
      access.joinClaim,
    );
    const updated: PlayerJoinAccess = {
      ...access,
      request: result.request,
      participant: result.participant,
      accessToken: result.accessToken,
    };
    savePlayerJoinAccess(updated);
    setPlayerJoinAccess(updated);
    setJoinRequest(result.request);
    return updated;
  }, []);

  const loadTable = React.useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const loadedTable = await fetchPublicTable(tablePublicId);
      setTable(loadedTable);

      const storedAccess = readDmAccess(tablePublicId);
      const activeSession = loadedTable.activeSession;
      if (storedAccess) {
        if (!activeSession || activeSession.sessionId !== storedAccess.session.sessionId) {
          clearDmAccess(tablePublicId);
          setDmKey("");
          setDmSession(null);
        } else {
          try {
            await fetchDmJoinRequests(activeSession.sessionId, storedAccess.dmKey);
            setDmKey(storedAccess.dmKey);
            setDmSession({
              sessionId: activeSession.sessionId,
              tablePublicId: loadedTable.tablePublicId,
              tableDisplayName: loadedTable.tableDisplayName,
              title: activeSession.title,
              status: "ACTIVE",
            });
            return;
          } catch {
            clearDmAccess(tablePublicId);
            setDmKey("");
            setDmSession(null);
          }
        }
      }

      const storedJoin = readPlayerJoinAccess(tablePublicId);
      if (!storedJoin) {
        setPlayerJoinAccess(null);
        setJoinRequest(null);
        return;
      }
      if (!activeSession || activeSession.sessionId !== storedJoin.sessionId) {
        clearPlayerJoinAccess(tablePublicId);
        setPlayerJoinAccess(null);
        setJoinRequest(null);
        return;
      }

      setPlayerName(storedJoin.request.displayName);
      setPlayerJoinAccess(storedJoin);
      setJoinRequest(storedJoin.request);
      try {
        await refreshPlayerJoin(storedJoin);
      } catch {
        // Mantiene lo stato locale durante una perdita di rete e riprova con il polling.
      }
    } catch (loadError) {
      setTable(null);
      setError(loadError instanceof Error ? loadError.message : "Impossibile leggere il tavolo.");
    } finally {
      setIsLoading(false);
    }
  }, [refreshPlayerJoin, tablePublicId]);

  React.useEffect(() => {
    void loadTable();
  }, [loadTable]);

  React.useEffect(() => {
    const normalizedDmKey = dmKey.trim();
    if (dmSession && normalizedDmKey) {
      saveDmAccess(tablePublicId, dmSession, normalizedDmKey);
    }
  }, [dmKey, dmSession, tablePublicId]);

  React.useEffect(() => {
    if (!playerJoinAccess || joinRequest?.status !== "PENDING") return;

    const timer = window.setInterval(() => {
      void refreshPlayerJoin(playerJoinAccess).catch(() => {
        // Un errore transitorio non cancella la richiesta posseduta dal dispositivo.
      });
    }, 3000);
    return () => window.clearInterval(timer);
  }, [joinRequest?.status, playerJoinAccess, refreshPlayerJoin]);

  async function startSession(event: React.FormEvent) {
    event.preventDefault();
    if (!table) return;
    setIsSubmitting(true);
    setError(null);
    try {
      const normalizedDmKey = dmKey.trim();
      const createdSession = await createTableSession(table, sessionTitle.trim(), normalizedDmKey);
      saveDmAccess(table.tablePublicId, createdSession, normalizedDmKey);
      setDmKey(normalizedDmKey);
      setDmSession(createdSession);
      setShowDmForm(false);
      setShowDmKey(false);
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "Creazione non riuscita.");
    } finally {
      setIsSubmitting(false);
    }
  }

  async function accessDmPanel(event: React.FormEvent) {
    event.preventDefault();
    const activeSession = table?.activeSession;
    if (!table || !activeSession) return;
    setIsSubmitting(true);
    setError(null);
    try {
      const normalizedDmKey = dmKey.trim();
      await fetchDmJoinRequests(activeSession.sessionId, normalizedDmKey);
      const restoredSession: CreatedSession = {
        sessionId: activeSession.sessionId,
        tablePublicId: table.tablePublicId,
        tableDisplayName: table.tableDisplayName,
        title: activeSession.title,
        status: "ACTIVE",
      };
      saveDmAccess(table.tablePublicId, restoredSession, normalizedDmKey);
      setDmKey(normalizedDmKey);
      setDmSession(restoredSession);
      setShowDmAccessForm(false);
      setShowDmKey(false);
    } catch (accessError) {
      setError(
        accessError instanceof Error ? accessError.message : "Accesso alla console DM non riuscito.",
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  async function joinSession(event: React.FormEvent) {
    event.preventDefault();
    const session = table?.activeSession;
    if (!session) return;
    setIsSubmitting(true);
    setError(null);
    try {
      const joinClaim = createUuid();
      const request = await requestSessionJoin(
        session.sessionId,
        playerName.trim(),
        playerReference(table.tablePublicId),
        joinClaim,
      );
      const access: PlayerJoinAccess = {
        version: 1,
        tablePublicId: table.tablePublicId,
        sessionId: session.sessionId,
        joinClaim,
        request,
        participant: null,
        accessToken: null,
      };
      savePlayerJoinAccess(access);
      setPlayerJoinAccess(access);
      setJoinRequest(request);
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : "Richiesta non riuscita.");
    } finally {
      setIsSubmitting(false);
    }
  }

  function resetJoinRequest() {
    clearPlayerJoinAccess(tablePublicId);
    setPlayerJoinAccess(null);
    setJoinRequest(null);
    setError(null);
  }

  if (dmSession) {
    return (
      <DmSessionPanel
        sessionId={dmSession.sessionId}
        sessionTitle={dmSession.title}
        tableDisplayName={dmSession.tableDisplayName}
        dmKey={dmKey.trim()}
        onLeave={() => {
          clearDmAccess(tablePublicId);
          setDmSession(null);
          setDmKey("");
          setShowDmKey(false);
          void loadTable();
        }}
      />
    );
  }

  return (
    <main className="min-h-screen bg-slate-50 px-4 py-5 text-slate-900 sm:px-6 sm:py-8">
      <header className="mx-auto flex w-full max-w-180 items-center justify-between border-b border-slate-200 pb-4">
        <div className="flex items-center gap-3">
          <span className="grid h-10 w-10 place-items-center rounded-md border border-slate-200 bg-white text-slate-800">
            <QrCode size={20} aria-hidden="true" />
          </span>
          <div>
            <p className="text-xs text-slate-500">BoardHub</p>
            <h1 className="text-lg font-semibold text-slate-950">Accesso al tavolo</h1>
          </div>
        </div>
        <button
          className="inline-flex h-9 cursor-pointer items-center gap-2 rounded-md border border-slate-300 bg-white px-3 text-sm font-medium hover:bg-slate-100"
          type="button"
          onClick={leavePage}
        >
          <ArrowLeft size={17} aria-hidden="true" />
          Esci
        </button>
      </header>

      <section className="mx-auto w-full max-w-180 py-8">
        {isLoading ? (
          <div className="flex min-h-64 items-center justify-center gap-3 text-slate-600">
            <LoaderCircle className="animate-spin" size={22} aria-hidden="true" />
            Verifica del tavolo in corso
          </div>
        ) : null}

        {!isLoading && error && !table ? (
          <div className="border-l-4 border-red-500 bg-white px-5 py-6">
            <div className="flex items-start gap-3">
              <AlertCircle className="mt-0.5 shrink-0 text-red-600" size={22} aria-hidden="true" />
              <div>
                <h2 className="text-lg font-semibold text-slate-950">QR del tavolo non valido</h2>
                <p className="mt-2 text-sm leading-6 text-slate-600">{error}</p>
                <button
                  className="mt-5 inline-flex h-9 cursor-pointer items-center gap-2 rounded-md bg-slate-900 px-3 text-sm font-medium text-white hover:bg-slate-700"
                  type="button"
                  onClick={leavePage}
                >
                  <ArrowLeft size={17} aria-hidden="true" />
                  Torna indietro
                </button>
              </div>
            </div>
          </div>
        ) : null}

        {!isLoading && table ? (
          <>
            <div className="flex flex-col gap-3 border-b border-slate-200 pb-6 sm:flex-row sm:items-end sm:justify-between">
              <div>
                <p className="text-sm text-slate-500">Locale BoardHub</p>
                <h2 className="mt-1 text-3xl font-semibold text-slate-950">{table.tableDisplayName}</h2>
              </div>
              <span
                className={`inline-flex w-fit items-center gap-2 rounded-md px-3 py-1.5 text-sm font-medium ${
                  table.status === "AVAILABLE"
                    ? "bg-emerald-50 text-emerald-700"
                    : "bg-blue-50 text-blue-700"
                }`}
              >
                <span className="h-2 w-2 rounded-full bg-current" />
                {table.status === "AVAILABLE" ? "Tavolo libero" : "Sessione attiva"}
              </span>
            </div>

            {table.status === "AVAILABLE" ? (
              <div className="py-8">
                <DoorOpen className="text-emerald-600" size={28} aria-hidden="true" />
                <h3 className="mt-4 text-xl font-semibold text-slate-950">
                  Non ci sono sessioni attive su questo tavolo
                </h3>
                <p className="mt-2 max-w-150 text-sm leading-6 text-slate-600">
                  Puoi uscire oppure avviare una nuova partita come Dungeon Master. La chiave DM
                  impedisce a un visitatore del QR di ottenere privilegi amministrativi.
                </p>

                {!showDmForm ? (
                  <div className="mt-6 flex flex-col gap-2 sm:flex-row">
                    <button
                      className="inline-flex h-10 cursor-pointer items-center justify-center gap-2 rounded-md bg-slate-900 px-4 text-sm font-medium text-white hover:bg-slate-700"
                      type="button"
                      onClick={() => setShowDmForm(true)}
                    >
                      <Shield size={18} aria-hidden="true" />
                      Diventa DM e avvia
                    </button>
                    <button
                      className="inline-flex h-10 cursor-pointer items-center justify-center gap-2 rounded-md border border-slate-300 bg-white px-4 text-sm font-medium hover:bg-slate-100"
                      type="button"
                      onClick={leavePage}
                    >
                      <ArrowLeft size={18} aria-hidden="true" />
                      Esci
                    </button>
                  </div>
                ) : (
                  <form className="mt-7 max-w-120 border-t border-slate-200 pt-6" onSubmit={startSession}>
                    <label className="block text-sm font-medium text-slate-700" htmlFor="sessionTitle">
                      Titolo della sessione
                    </label>
                    <input
                      className="mt-2 h-10 w-full rounded-md border border-slate-300 bg-white px-3 outline-none focus:border-slate-600"
                      id="sessionTitle"
                      value={sessionTitle}
                      onChange={(event) => setSessionTitle(event.target.value)}
                      required
                      maxLength={120}
                    />
                    <label className="mt-4 block text-sm font-medium text-slate-700" htmlFor="dmKey">
                      Chiave DM locale
                    </label>
                    <div className="mt-2 flex h-10 items-center gap-2 rounded-md border border-slate-300 bg-white px-3 focus-within:border-slate-600">
                      <KeyRound className="text-slate-500" size={17} aria-hidden="true" />
                      <input
                        className="min-w-0 flex-1 border-0 bg-transparent outline-none"
                        id="dmKey"
                        ref={dmKeyInputRef}
                        type={showDmKey ? "text" : "password"}
                        value={dmKey}
                        onChange={(event) => setDmKey(event.target.value)}
                        required
                        autoComplete="off"
                        autoCapitalize="none"
                        autoCorrect="off"
                        spellCheck={false}
                      />
                      <button
                        className="grid h-8 w-8 shrink-0 cursor-pointer place-items-center text-slate-500 hover:text-slate-950"
                        type="button"
                        onPointerDown={(event) => event.preventDefault()}
                        onClick={() => {
                          setShowDmKey((current) => !current);
                          dmKeyInputRef.current?.focus({ preventScroll: true });
                        }}
                        aria-pressed={showDmKey}
                        aria-label={showDmKey ? "Nascondi chiave DM" : "Mostra chiave DM"}
                        title={showDmKey ? "Nascondi chiave DM" : "Mostra chiave DM"}
                      >
                        {showDmKey ? (
                          <EyeOff size={18} aria-hidden="true" />
                        ) : (
                          <Eye size={18} aria-hidden="true" />
                        )}
                      </button>
                    </div>
                    <div className="mt-5 flex gap-2">
                      <button
                        className="inline-flex h-10 cursor-pointer items-center gap-2 rounded-md bg-slate-900 px-4 text-sm font-medium text-white hover:bg-slate-700 disabled:cursor-progress disabled:opacity-60"
                        type="submit"
                        disabled={isSubmitting}
                      >
                        {isSubmitting ? <LoaderCircle className="animate-spin" size={18} /> : <Dices size={18} />}
                        Crea sessione
                      </button>
                      <button
                        className="h-10 cursor-pointer rounded-md border border-slate-300 bg-white px-4 text-sm font-medium hover:bg-slate-100"
                        type="button"
                        onClick={() => setShowDmForm(false)}
                      >
                        Annulla
                      </button>
                    </div>
                  </form>
                )}
              </div>
            ) : null}

            {table.status === "IN_SESSION" && table.activeSession ? (
              <div className="py-8">
                <div className="flex items-center gap-2 text-blue-700">
                  <Users size={21} aria-hidden="true" />
                  <span className="text-sm font-medium">{table.activeSession.gameType}</span>
                </div>
                <h3 className="mt-3 text-2xl font-semibold text-slate-950">
                  {table.activeSession.title}
                </h3>
                <p className="mt-2 max-w-150 text-sm leading-6 text-slate-600">
                  {table.activeSession.publicSummary ?? "La sessione accetta richieste di ingresso."}
                </p>

                {joinRequest ? (
                  <div
                    className={`mt-7 border-l-4 bg-white px-5 py-5 ${
                      joinRequest.status === "ACCEPTED"
                        ? "border-emerald-500"
                        : joinRequest.status === "PENDING"
                          ? "border-blue-500"
                          : joinRequest.status === "EXPIRED"
                            ? "border-amber-500"
                            : "border-red-500"
                    }`}
                  >
                    <div className="flex gap-3">
                      {joinRequest.status === "ACCEPTED" ? (
                        <CheckCircle2 className="shrink-0 text-emerald-600" size={22} aria-hidden="true" />
                      ) : joinRequest.status === "PENDING" ? (
                        <LoaderCircle className="shrink-0 animate-spin text-blue-600" size={22} aria-hidden="true" />
                      ) : joinRequest.status === "EXPIRED" ? (
                        <Clock3 className="shrink-0 text-amber-600" size={22} aria-hidden="true" />
                      ) : (
                        <XCircle className="shrink-0 text-red-600" size={22} aria-hidden="true" />
                      )}
                      <div>
                        <h4 className="font-semibold text-slate-950">
                          {joinRequest.status === "ACCEPTED"
                            ? "Ingresso accettato"
                            : joinRequest.status === "PENDING"
                              ? "Richiesta inviata al DM"
                              : joinRequest.status === "EXPIRED"
                                ? "Richiesta scaduta"
                                : "Richiesta rifiutata"}
                        </h4>
                        <p className="mt-1 text-sm leading-6 text-slate-600">
                          {joinRequest.status === "ACCEPTED"
                            ? `Sei entrato come ${joinRequest.displayName}. Il dispositivo ha ricevuto la credenziale della sessione.`
                            : joinRequest.status === "PENDING"
                              ? "La pagina controlla automaticamente la decisione del Dungeon Master. Puoi anche ricaricarla senza perdere la richiesta."
                              : joinRequest.status === "EXPIRED"
                                ? "Il Dungeon Master non ha risposto entro il tempo previsto. Puoi inviare una nuova richiesta."
                                : "Il Dungeon Master non ha accettato questa richiesta. Puoi riprovare se ti autorizza."}
                        </p>
                        {joinRequest.status === "REJECTED" || joinRequest.status === "EXPIRED" ? (
                          <button
                            className="mt-4 inline-flex h-9 cursor-pointer items-center gap-2 rounded-md border border-slate-300 bg-white px-3 text-sm font-medium hover:bg-slate-100"
                            type="button"
                            onClick={resetJoinRequest}
                          >
                            <RefreshCw size={16} aria-hidden="true" />
                            Invia una nuova richiesta
                          </button>
                        ) : null}
                      </div>
                    </div>
                  </div>
                ) : (
                  <form className="mt-7 max-w-120 border-t border-slate-200 pt-6" onSubmit={joinSession}>
                    <label className="block text-sm font-medium text-slate-700" htmlFor="playerName">
                      Il tuo nome
                    </label>
                    <input
                      className="mt-2 h-10 w-full rounded-md border border-slate-300 bg-white px-3 outline-none focus:border-slate-600"
                      id="playerName"
                      value={playerName}
                      onChange={(event) => setPlayerName(event.target.value)}
                      placeholder="Es. Andrea"
                      required
                      maxLength={80}
                    />
                    <button
                      className="mt-4 inline-flex h-10 cursor-pointer items-center gap-2 rounded-md bg-slate-900 px-4 text-sm font-medium text-white hover:bg-slate-700 disabled:cursor-progress disabled:opacity-60"
                      type="submit"
                      disabled={isSubmitting}
                    >
                      {isSubmitting ? <LoaderCircle className="animate-spin" size={18} /> : <Users size={18} />}
                      Richiedi di partecipare
                    </button>
                  </form>
                )}

                <div className="mt-8 max-w-120 border-t border-slate-200 pt-6">
                  {!showDmAccessForm ? (
                    <button
                      className="inline-flex h-9 cursor-pointer items-center gap-2 text-sm font-medium text-slate-600 hover:text-slate-950"
                      type="button"
                      onClick={() => setShowDmAccessForm(true)}
                    >
                      <Shield size={17} aria-hidden="true" />
                      Accedi come Dungeon Master
                    </button>
                  ) : (
                    <form onSubmit={accessDmPanel}>
                      <label className="block text-sm font-medium text-slate-700" htmlFor="activeDmKey">
                        Chiave DM locale
                      </label>
                      <div className="mt-2 flex h-10 items-center gap-2 rounded-md border border-slate-300 bg-white px-3 focus-within:border-slate-600">
                        <KeyRound className="text-slate-500" size={17} aria-hidden="true" />
                        <input
                          className="min-w-0 flex-1 border-0 bg-transparent outline-none"
                          id="activeDmKey"
                          ref={dmKeyInputRef}
                          type={showDmKey ? "text" : "password"}
                          value={dmKey}
                          onChange={(event) => setDmKey(event.target.value)}
                          required
                          autoComplete="off"
                          autoCapitalize="none"
                          autoCorrect="off"
                          spellCheck={false}
                        />
                        <button
                          className="grid h-8 w-8 shrink-0 cursor-pointer place-items-center text-slate-500 hover:text-slate-950"
                          type="button"
                          onPointerDown={(event) => event.preventDefault()}
                          onClick={() => {
                            setShowDmKey((current) => !current);
                            dmKeyInputRef.current?.focus({ preventScroll: true });
                          }}
                          aria-pressed={showDmKey}
                          aria-label={showDmKey ? "Nascondi chiave DM" : "Mostra chiave DM"}
                          title={showDmKey ? "Nascondi chiave DM" : "Mostra chiave DM"}
                        >
                          {showDmKey ? (
                            <EyeOff size={18} aria-hidden="true" />
                          ) : (
                            <Eye size={18} aria-hidden="true" />
                          )}
                        </button>
                      </div>
                      <div className="mt-4 flex gap-2">
                        <button
                          className="inline-flex h-10 cursor-pointer items-center gap-2 rounded-md bg-slate-900 px-4 text-sm font-medium text-white hover:bg-slate-700 disabled:cursor-progress disabled:opacity-60"
                          type="submit"
                          disabled={isSubmitting}
                        >
                          {isSubmitting ? (
                            <LoaderCircle className="animate-spin" size={18} />
                          ) : (
                            <Shield size={18} />
                          )}
                          Apri console DM
                        </button>
                        <button
                          className="h-10 cursor-pointer rounded-md border border-slate-300 bg-white px-4 text-sm font-medium hover:bg-slate-100"
                          type="button"
                          onClick={() => {
                            setShowDmAccessForm(false);
                            setDmKey("");
                            setShowDmKey(false);
                          }}
                        >
                          Annulla
                        </button>
                      </div>
                    </form>
                  )}
                </div>
              </div>
            ) : null}

            {error ? (
              <div className="mt-2 flex items-start gap-2 border-l-4 border-red-500 bg-white px-4 py-3 text-sm text-red-700" role="alert">
                <AlertCircle className="mt-0.5 shrink-0" size={18} aria-hidden="true" />
                <span>{error}</span>
              </div>
            ) : null}

            <button
              className="mt-2 inline-flex h-9 cursor-pointer items-center gap-2 text-sm font-medium text-slate-600 hover:text-slate-950"
              type="button"
              onClick={() => void loadTable()}
            >
              <RefreshCw size={17} aria-hidden="true" />
              Aggiorna stato tavolo
            </button>
          </>
        ) : null}
      </section>
    </main>
  );
}
