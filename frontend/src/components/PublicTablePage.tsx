import * as React from "react";
import {
  AlertCircle,
  ArrowLeft,
  CheckCircle2,
  Clock3,
  Dices,
  DoorClosed,
  DoorOpen,
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
  if (existing) return existing;

  const created = `web-${createUuid()}`;
  window.localStorage.setItem(key, created);
  return created;
}

function claimExpiryLabel(claimExpiresAt: string | null) {
  if (!claimExpiresAt) return null;
  const expiration = new Date(claimExpiresAt);
  if (Number.isNaN(expiration.getTime())) return null;
  return expiration.toLocaleTimeString("it-IT", {
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function PublicTablePage({ tablePublicId }: PublicTablePageProps) {
  const [table, setTable] = React.useState<PublicTableStatus | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const [isLoading, setIsLoading] = React.useState(true);
  const [showDmForm, setShowDmForm] = React.useState(false);
  const [dmToken, setDmToken] = React.useState("");
  const [dmSession, setDmSession] = React.useState<CreatedSession | null>(null);
  const [sessionTitle, setSessionTitle] = React.useState("Nuova avventura");
  const [playerName, setPlayerName] = React.useState("");
  const [joinRequest, setJoinRequest] = React.useState<JoinRequest | null>(null);
  const [playerJoinAccess, setPlayerJoinAccess] =
    React.useState<PlayerJoinAccess | null>(null);
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

  const loadTable = React.useCallback(
    async (showLoading = true) => {
      if (showLoading) setIsLoading(true);
      setError(null);
      try {
        const loadedTable = await fetchPublicTable(tablePublicId);
        setTable(loadedTable);
        const activeSession = loadedTable.activeSession;

        const storedDmAccess = readDmAccess(tablePublicId);
        if (storedDmAccess) {
          if (
            !activeSession ||
            activeSession.sessionId !== storedDmAccess.session.sessionId
          ) {
            clearDmAccess(tablePublicId);
            setDmToken("");
            setDmSession(null);
          } else {
            try {
              await fetchDmJoinRequests(
                activeSession.sessionId,
                storedDmAccess.dmToken,
              );
              setDmToken(storedDmAccess.dmToken);
              setDmSession({
                ...storedDmAccess.session,
                dmAccessToken: storedDmAccess.dmToken,
              });
              return;
            } catch {
              clearDmAccess(tablePublicId);
              setDmToken("");
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
          // Lo stato locale resta disponibile durante errori di rete transitori.
        }
      } catch (loadError) {
        setTable(null);
        setError(
          loadError instanceof Error
            ? loadError.message
            : "Impossibile leggere il tavolo.",
        );
      } finally {
        if (showLoading) setIsLoading(false);
      }
    },
    [refreshPlayerJoin, tablePublicId],
  );

  React.useEffect(() => {
    void loadTable();
  }, [loadTable]);

  React.useEffect(() => {
    if (dmSession) return;
    const timer = window.setInterval(() => void loadTable(false), 5000);
    return () => window.clearInterval(timer);
  }, [dmSession, loadTable]);

  React.useEffect(() => {
    if (!playerJoinAccess || joinRequest?.status !== "PENDING") return;
    const timer = window.setInterval(() => {
      void refreshPlayerJoin(playerJoinAccess).catch(() => {
        // La richiesta posseduta dal dispositivo non viene persa per un errore transitorio.
      });
    }, 3000);
    return () => window.clearInterval(timer);
  }, [joinRequest?.status, playerJoinAccess, refreshPlayerJoin]);

  async function startSession(event: React.FormEvent) {
    event.preventDefault();
    if (!table || table.status !== "CLAIMABLE") return;
    setIsSubmitting(true);
    setError(null);
    try {
      const createdSession = await createTableSession(table, sessionTitle.trim());
      saveDmAccess(table.tablePublicId, createdSession);
      setDmToken(createdSession.dmAccessToken);
      setDmSession(createdSession);
      setShowDmForm(false);
    } catch (submitError) {
      setError(
        submitError instanceof Error
          ? submitError.message
          : "Creazione non riuscita.",
      );
      await loadTable(false);
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
      setError(
        submitError instanceof Error
          ? submitError.message
          : "Richiesta non riuscita.",
      );
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

  function clearClosedSession() {
    clearDmAccess(tablePublicId);
    clearPlayerJoinAccess(tablePublicId);
    setDmSession(null);
    setDmToken("");
    setJoinRequest(null);
    setPlayerJoinAccess(null);
    void loadTable();
  }

  if (dmSession && dmToken) {
    return (
      <DmSessionPanel
        sessionId={dmSession.sessionId}
        sessionTitle={dmSession.title}
        tableDisplayName={dmSession.tableDisplayName}
        dmToken={dmToken}
        onLeave={() => setDmSession(null)}
        onSessionClosed={clearClosedSession}
      />
    );
  }

  const expiryLabel = claimExpiryLabel(table?.claimExpiresAt ?? null);

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
              <AlertCircle
                className="mt-0.5 shrink-0 text-red-600"
                size={22}
                aria-hidden="true"
              />
              <div>
                <h2 className="text-lg font-semibold text-slate-950">
                  QR del tavolo non valido
                </h2>
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
                <h2 className="mt-1 text-3xl font-semibold text-slate-950">
                  {table.tableDisplayName}
                </h2>
              </div>
              <span
                className={`inline-flex w-fit items-center gap-2 rounded-md px-3 py-1.5 text-sm font-medium ${
                  table.status === "DISABLED"
                    ? "bg-slate-200 text-slate-700"
                    : table.status === "CLAIMABLE"
                      ? "bg-emerald-50 text-emerald-700"
                      : "bg-blue-50 text-blue-700"
                }`}
              >
                <span className="h-2 w-2 rounded-full bg-current" />
                {table.status === "DISABLED"
                  ? "Tavolo non abilitato"
                  : table.status === "CLAIMABLE"
                    ? "Tavolo pronto"
                    : "Sessione attiva"}
              </span>
            </div>

            {table.status === "DISABLED" ? (
              <div className="py-8">
                <DoorClosed className="text-slate-500" size={28} aria-hidden="true" />
                <h3 className="mt-4 text-xl font-semibold text-slate-950">
                  Il tavolo non è ancora attivo
                </h3>
                <p className="mt-2 max-w-150 text-sm leading-6 text-slate-600">
                  Il QR è corretto, ma il personale del locale deve abilitare questo
                  tavolo prima che possa iniziare una nuova partita. La pagina controlla
                  automaticamente lo stato.
                </p>
              </div>
            ) : null}

            {table.status === "CLAIMABLE" ? (
              <div className="py-8">
                <DoorOpen className="text-emerald-600" size={28} aria-hidden="true" />
                <h3 className="mt-4 text-xl font-semibold text-slate-950">
                  Il tavolo è pronto per una nuova sessione
                </h3>
                <p className="mt-2 max-w-150 text-sm leading-6 text-slate-600">
                  Il locale ha autorizzato l&apos;avvio
                  {expiryLabel ? ` fino alle ${expiryLabel}` : ""}. Chi crea ora la
                  sessione diventa Dungeon Master su questo dispositivo.
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
                  <form
                    className="mt-7 max-w-120 border-t border-slate-200 pt-6"
                    onSubmit={startSession}
                  >
                    <label
                      className="block text-sm font-medium text-slate-700"
                      htmlFor="sessionTitle"
                    >
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
                    <p className="mt-3 text-xs leading-5 text-slate-500">
                      L&apos;accesso DM viene associato a questo browser e resta
                      disponibile dopo un aggiornamento della pagina.
                    </p>
                    <div className="mt-5 flex gap-2">
                      <button
                        className="inline-flex h-10 cursor-pointer items-center gap-2 rounded-md bg-slate-900 px-4 text-sm font-medium text-white hover:bg-slate-700 disabled:cursor-progress disabled:opacity-60"
                        type="submit"
                        disabled={isSubmitting}
                      >
                        {isSubmitting ? (
                          <LoaderCircle className="animate-spin" size={18} />
                        ) : (
                          <Dices size={18} />
                        )}
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
                  <span className="text-sm font-medium">
                    {table.activeSession.gameType}
                  </span>
                </div>
                <h3 className="mt-3 text-2xl font-semibold text-slate-950">
                  {table.activeSession.title}
                </h3>
                <p className="mt-2 max-w-150 text-sm leading-6 text-slate-600">
                  {table.activeSession.publicSummary ??
                    "La sessione accetta richieste di ingresso."}
                </p>

                {dmToken ? (
                  <button
                    className="mt-6 inline-flex h-10 cursor-pointer items-center gap-2 rounded-md bg-slate-900 px-4 text-sm font-medium text-white hover:bg-slate-700"
                    type="button"
                    onClick={() => {
                      const storedAccess = readDmAccess(tablePublicId);
                      if (storedAccess) {
                        setDmSession({
                          ...storedAccess.session,
                          dmAccessToken: storedAccess.dmToken,
                        });
                      }
                    }}
                  >
                    <Shield size={18} aria-hidden="true" />
                    Riapri console DM
                  </button>
                ) : joinRequest ? (
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
                        <CheckCircle2
                          className="shrink-0 text-emerald-600"
                          size={22}
                          aria-hidden="true"
                        />
                      ) : joinRequest.status === "PENDING" ? (
                        <LoaderCircle
                          className="shrink-0 animate-spin text-blue-600"
                          size={22}
                          aria-hidden="true"
                        />
                      ) : joinRequest.status === "EXPIRED" ? (
                        <Clock3
                          className="shrink-0 text-amber-600"
                          size={22}
                          aria-hidden="true"
                        />
                      ) : (
                        <XCircle
                          className="shrink-0 text-red-600"
                          size={22}
                          aria-hidden="true"
                        />
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
                              ? "La pagina controlla automaticamente la decisione del Dungeon Master. Puoi ricaricarla senza perdere la richiesta."
                              : joinRequest.status === "EXPIRED"
                                ? "Il Dungeon Master non ha risposto in tempo. Puoi inviare una nuova richiesta."
                                : "Il Dungeon Master non ha accettato questa richiesta."}
                        </p>
                        {joinRequest.status === "REJECTED" ||
                        joinRequest.status === "EXPIRED" ? (
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
                  <form
                    className="mt-7 max-w-120 border-t border-slate-200 pt-6"
                    onSubmit={joinSession}
                  >
                    <label
                      className="block text-sm font-medium text-slate-700"
                      htmlFor="playerName"
                    >
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
                      {isSubmitting ? (
                        <LoaderCircle className="animate-spin" size={18} />
                      ) : (
                        <Users size={18} />
                      )}
                      Richiedi di partecipare
                    </button>
                  </form>
                )}
              </div>
            ) : null}

            {error ? (
              <div
                className="mt-2 flex items-start gap-2 border-l-4 border-red-500 bg-white px-4 py-3 text-sm text-red-700"
                role="alert"
              >
                <AlertCircle
                  className="mt-0.5 shrink-0"
                  size={18}
                  aria-hidden="true"
                />
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
