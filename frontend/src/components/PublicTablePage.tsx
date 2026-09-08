import * as React from "react";
import {
  AlertCircle,
  ArrowLeft,
  CheckCircle2,
  Clock3,
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
import type {
  CreatedSession,
  JoinRequest,
  PublicTableStatus,
  SessionGridConfiguration,
} from "../types";
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
import { DmSessionSetupPage } from "./DmSessionSetupPage";
import { PlayerSessionPanel } from "./PlayerSessionPanel";
import { AlertBanner, Button, PageHeaderIdentity, StatusChip } from "./ui";

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
  const [joinRequest, setJoinRequest] = React.useState<JoinRequest | null>(
    null,
  );
  const [playerJoinAccess, setPlayerJoinAccess] =
    React.useState<PlayerJoinAccess | null>(null);
  const [isSubmitting, setIsSubmitting] = React.useState(false);

  const refreshPlayerJoin = React.useCallback(
    async (access: PlayerJoinAccess) => {
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
    },
    [],
  );

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
        if (
          !activeSession ||
          activeSession.sessionId !== storedJoin.sessionId
        ) {
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

  async function startSession(grid: SessionGridConfiguration) {
    if (!table || table.status !== "CLAIMABLE") return;
    setIsSubmitting(true);
    setError(null);
    try {
      const createdSession = await createTableSession(
        table,
        sessionTitle.trim(),
        grid,
      );
      saveDmAccess(table.tablePublicId, createdSession);
      setDmToken(createdSession.dmAccessToken);
      setDmSession(createdSession);
      setShowDmForm(false);
    } catch (submitError) {
      const message =
        submitError instanceof Error
          ? submitError.message
          : "Creazione non riuscita.";
      await loadTable(false);
      setError(message);
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
        dmToken={dmToken}
        onLeave={() => setDmSession(null)}
        onSessionClosed={clearClosedSession}
      />
    );
  }

  if (showDmForm && table?.status === "CLAIMABLE") {
    return (
      <DmSessionSetupPage
        tableDisplayName={table.tableDisplayName}
        sessionTitle={sessionTitle}
        error={error}
        isSubmitting={isSubmitting}
        onSessionTitleChange={setSessionTitle}
        onCancel={() => {
          setShowDmForm(false);
          setError(null);
        }}
        onSubmit={startSession}
      />
    );
  }

  if (
    table?.status === "IN_SESSION" &&
    table.activeSession &&
    playerJoinAccess?.request.status === "ACCEPTED" &&
    playerJoinAccess.accessToken
  ) {
    return (
      <PlayerSessionPanel
        sessionId={table.activeSession.sessionId}
        sessionTitle={table.activeSession.title}
        playerToken={playerJoinAccess.accessToken}
        onLeave={leavePage}
      />
    );
  }

  const expiryLabel = claimExpiryLabel(table?.claimExpiresAt ?? null);

  return (
    <main className="min-h-screen bg-transparent px-3 py-4 text-ink sm:px-6 sm:py-7">
      <header className="bh-surface mx-auto flex w-full max-w-295 items-center justify-between gap-4 p-3 sm:p-4">
        <PageHeaderIdentity
          accentClassName="bg-primary"
          eyebrow="BoardHub"
          icon={<QrCode size={20} />}
          title="Entra al tavolo"
        />
        <Button
          aria-label="Esci dalla pagina del tavolo"
          size="compact"
          variant="secondary"
          icon={<ArrowLeft size={17} aria-hidden="true" />}
          onClick={leavePage}
        >
          <span className="hidden sm:inline">Esci</span>
        </Button>
      </header>

      <section className="bh-surface mx-auto mt-4 w-full max-w-295 overflow-hidden">
        {isLoading ? (
          <div className="flex min-h-56 flex-col items-center justify-center gap-4 bg-warning/40 px-5 py-12 text-center">
            <span className="bh-card-icon h-12 w-12 bg-warning">
              <LoaderCircle
                className="animate-spin"
                size={23}
                aria-hidden="true"
              />
            </span>
            <div>
              <h2 className="text-2xl">Controllo del tavolo</h2>
              <p className="mt-2 text-sm font-semibold text-muted">
                Verifica dello stato in corso
              </p>
            </div>
          </div>
        ) : null}

        {!isLoading && error && !table ? (
          <div className="grid min-h-64 gap-0 sm:grid-cols-[minmax(0,1fr)_18rem]">
            <div className="px-5 py-8 sm:p-8">
              <span className="bh-card-icon bg-primary/40 h-12 w-12">
                <AlertCircle size={23} aria-hidden="true" />
              </span>
              <p className="bh-kicker mt-6 bg-primary/40">
                Collegamento non riuscito
              </p>
              <h2 className="mt-4 text-3xl sm:text-4xl">QR non riconosciuto</h2>
              <p className="mt-3 max-w-2xl text-base leading-7 text-muted">
                {error}
              </p>
            </div>
            <div className="flex items-end border-t-2 border-ink bg-primary/20 p-5 sm:border-l-2 sm:border-t-0 sm:p-6">
              <Button
                className="w-full"
                variant="primary"
                icon={<ArrowLeft size={17} aria-hidden="true" />}
                onClick={leavePage}
              >
                Torna indietro
              </Button>
            </div>
          </div>
        ) : null}

        {!isLoading && table ? (
          <>
            <div className="flex flex-col gap-4 border-b-2 border-ink bg-muted/10 px-5 py-5 sm:flex-row sm:items-center sm:justify-between sm:px-7">
              <div className="min-w-0">
                <div className="flex flex-wrap items-center gap-2 text-xs font-extrabold uppercase tracking-[0.045em] text-muted">
                  <span>Locale BoardHub</span>
                  <span aria-hidden="true">/</span>
                  <span className="font-mono normal-case tracking-normal">
                    {table.tablePublicId}
                  </span>
                </div>
                <h2 className="mt-2 text-4xl sm:text-5xl">
                  {table.tableDisplayName}
                </h2>
              </div>
              <StatusChip
                dot
                className="shrink-0"
                tone={
                  table.status === "DISABLED"
                    ? "warning"
                    : table.status === "CLAIMABLE"
                      ? "success"
                      : "info"
                }
              >
                {table.status === "DISABLED"
                  ? "Tavolo non abilitato"
                  : table.status === "CLAIMABLE"
                    ? "Tavolo pronto"
                    : "Sessione attiva"}
              </StatusChip>
            </div>

            {table.status === "DISABLED" ? (
              <div className="grid lg:grid-cols-[minmax(0,1.35fr)_minmax(19rem,0.65fr)]">
                <div className="px-5 py-8 sm:p-8 lg:p-10">
                  <span className="bh-card-icon h-12 w-12 bg-warning">
                    <DoorClosed size={23} aria-hidden="true" />
                  </span>
                  <p className="bh-kicker mt-6 bg-warning">
                    In attesa del locale
                  </p>
                  <h3 className="mt-4 max-w-3xl text-3xl sm:text-4xl">
                    Tavolo non ancora abilitato
                  </h3>
                  <p className="mt-4 max-w-2xl text-base leading-7 text-muted">
                    Il QR è valido. Prima di iniziare, il personale deve rendere
                    disponibile questo tavolo dalla console del locale.
                  </p>
                  <div className="mt-7 flex flex-wrap items-center gap-x-4 gap-y-3">
                    <Button
                      variant="primary"
                      icon={<RefreshCw size={17} aria-hidden="true" />}
                      onClick={() => void loadTable()}
                    >
                      Aggiorna ora
                    </Button>
                    <span className="inline-flex items-center gap-2 text-sm font-bold text-muted">
                      <Clock3 size={16} aria-hidden="true" />
                      Controllo automatico ogni 5 secondi
                    </span>
                  </div>
                </div>

                <aside className="border-t-2 border-ink bg-warning/40 p-5 sm:p-7 lg:border-l-2 lg:border-t-0 lg:p-8">
                  <p className="text-xs font-extrabold uppercase tracking-[0.055em] text-muted">
                    Prossimi passaggi
                  </p>
                  <h3 className="mt-3 text-2xl">Cosa succede ora</h3>
                  <ol className="mt-6 space-y-5">
                    <li className="grid grid-cols-[2rem_minmax(0,1fr)] gap-3">
                      <span className="grid h-8 w-8 place-items-center border-2 border-ink bg-surface font-black shadow-[2px_2px_0_var(--color-ink)]">
                        1
                      </span>
                      <div>
                        <p className="font-extrabold">
                          Il locale abilita il tavolo
                        </p>
                        <p className="mt-1 text-sm leading-5 text-muted">
                          L&apos;autorizzazione è temporanea e vale solo per
                          l&apos;avvio.
                        </p>
                      </div>
                    </li>
                    <li className="grid grid-cols-[2rem_minmax(0,1fr)] gap-3">
                      <span className="grid h-8 w-8 place-items-center border-2 border-ink bg-surface font-black shadow-[2px_2px_0_var(--color-ink)]">
                        2
                      </span>
                      <div>
                        <p className="font-extrabold">La pagina si aggiorna</p>
                        <p className="mt-1 text-sm leading-5 text-muted">
                          Non serve scansionare di nuovo il codice.
                        </p>
                      </div>
                    </li>
                    <li className="grid grid-cols-[2rem_minmax(0,1fr)] gap-3">
                      <span className="grid h-8 w-8 place-items-center border-2 border-ink bg-surface font-black shadow-[2px_2px_0_var(--color-ink)]">
                        3
                      </span>
                      <div>
                        <p className="font-extrabold">
                          Il primo dispositivo diventa DM
                        </p>
                        <p className="mt-1 text-sm leading-5 text-muted">
                          Gli altri giocatori potranno poi chiedere di entrare.
                        </p>
                      </div>
                    </li>
                  </ol>
                </aside>
              </div>
            ) : null}

            {table.status === "CLAIMABLE" ? (
              <div className="grid lg:grid-cols-[minmax(0,1.35fr)_minmax(19rem,0.65fr)]">
                <div className="px-5 py-8 sm:p-8 lg:p-10">
                  <span className="bh-card-icon h-12 w-12 bg-success/40">
                    <DoorOpen size={23} aria-hidden="true" />
                  </span>
                  <p className="bh-kicker mt-6 bg-success/40">
                    Autorizzato dal locale
                  </p>
                  <h3 className="mt-4 max-w-3xl text-3xl sm:text-4xl">
                    Apri una nuova avventura
                  </h3>
                  <p className="mt-4 max-w-2xl text-base leading-7 text-muted">
                    Il locale ha autorizzato l&apos;avvio
                    {expiryLabel ? ` fino alle ${expiryLabel}` : ""}. Chi crea
                    ora la sessione diventa Dungeon Master su questo
                    dispositivo.
                  </p>

                  <div className="mt-7 flex flex-col gap-3 sm:flex-row">
                    <Button
                      variant="primary"
                      icon={<Shield size={18} aria-hidden="true" />}
                      onClick={() => setShowDmForm(true)}
                    >
                      Diventa DM e prepara
                    </Button>
                    <Button
                      variant="secondary"
                      icon={<ArrowLeft size={18} aria-hidden="true" />}
                      onClick={leavePage}
                    >
                      Esci
                    </Button>
                  </div>
                </div>

                <aside className="border-t-2 border-ink bg-success/20 p-5 sm:p-7 lg:border-l-2 lg:border-t-0 lg:p-8">
                  <p className="text-xs font-extrabold uppercase tracking-[0.055em] text-muted">
                    Ruolo iniziale
                  </p>
                  <h3 className="mt-3 text-2xl">Il primo accesso è il DM</h3>
                  <p className="mt-4 text-sm leading-6 text-muted">
                    La console del Dungeon Master resterà associata a questo
                    browser anche dopo un aggiornamento della pagina.
                  </p>
                  {expiryLabel ? (
                    <div className="mt-6 border-2 border-ink bg-surface p-4 shadow-[3px_3px_0_var(--color-ink)]">
                      <p className="text-xs font-extrabold uppercase tracking-[0.045em] text-muted">
                        Avvio consentito entro
                      </p>
                      <p className="mt-2 text-2xl font-black">{expiryLabel}</p>
                    </div>
                  ) : null}
                </aside>
              </div>
            ) : null}

            {table.status === "IN_SESSION" && table.activeSession ? (
              <div className="grid lg:grid-cols-[minmax(0,1.35fr)_minmax(19rem,0.65fr)]">
                <div className="px-5 py-8 sm:p-8 lg:p-10">
                  <div className="flex items-center gap-2 font-extrabold uppercase tracking-[0.045em] text-ink">
                    <Users size={21} aria-hidden="true" />
                    <span className="text-sm">
                      {table.activeSession.gameType}
                    </span>
                  </div>
                  <h3 className="mt-4 max-w-3xl text-3xl sm:text-4xl">
                    {table.activeSession.title}
                  </h3>
                  <p className="mt-4 max-w-2xl text-base leading-7 text-muted">
                    {table.activeSession.publicSummary ??
                      "La sessione accetta richieste di ingresso."}
                  </p>

                  {dmToken ? (
                    <Button
                      className="mt-7"
                      variant="primary"
                      icon={<Shield size={18} aria-hidden="true" />}
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
                      Riapri console DM
                    </Button>
                  ) : joinRequest ? (
                    <div
                      className={`mt-7 border-2 border-ink px-5 py-5 shadow-[3px_3px_0_var(--color-ink)] ${
                        joinRequest.status === "ACCEPTED"
                          ? "bg-success/20"
                          : joinRequest.status === "PENDING"
                            ? "bg-info/20"
                            : joinRequest.status === "EXPIRED"
                              ? "bg-warning/40"
                              : "bg-primary/20"
                      }`}
                    >
                      <div className="flex gap-3">
                        {joinRequest.status === "ACCEPTED" ? (
                          <CheckCircle2
                            className="shrink-0 text-success"
                            size={22}
                            aria-hidden="true"
                          />
                        ) : joinRequest.status === "PENDING" ? (
                          <LoaderCircle
                            className="shrink-0 animate-spin text-info"
                            size={22}
                            aria-hidden="true"
                          />
                        ) : joinRequest.status === "EXPIRED" ? (
                          <Clock3
                            className="shrink-0 text-warning"
                            size={22}
                            aria-hidden="true"
                          />
                        ) : (
                          <XCircle
                            className="shrink-0 text-danger"
                            size={22}
                            aria-hidden="true"
                          />
                        )}
                        <div>
                          <h4 className="font-semibold text-ink">
                            {joinRequest.status === "ACCEPTED"
                              ? "Ingresso accettato"
                              : joinRequest.status === "PENDING"
                                ? "Richiesta inviata al DM"
                                : joinRequest.status === "EXPIRED"
                                  ? "Richiesta scaduta"
                                  : "Richiesta rifiutata"}
                          </h4>
                          <p className="mt-1 text-sm leading-6 text-muted">
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
                            <Button
                              className="mt-4"
                              size="compact"
                              variant="secondary"
                              icon={<RefreshCw size={16} aria-hidden="true" />}
                              onClick={resetJoinRequest}
                            >
                              Invia una nuova richiesta
                            </Button>
                          ) : null}
                        </div>
                      </div>
                    </div>
                  ) : (
                    <form
                      className="mt-8 max-w-xl border-t-2 border-ink pt-6"
                      onSubmit={joinSession}
                    >
                      <label
                        className="block text-sm font-medium text-muted"
                        htmlFor="playerName"
                      >
                        Il tuo nome
                      </label>
                      <input
                        className="bh-input mt-2 h-10 w-full px-3"
                        id="playerName"
                        value={playerName}
                        onChange={(event) => setPlayerName(event.target.value)}
                        placeholder="Es. Andrea"
                        required
                        maxLength={80}
                      />
                      <Button
                        className="mt-4"
                        variant="primary"
                        type="submit"
                        disabled={isSubmitting}
                        icon={
                          isSubmitting ? (
                            <LoaderCircle className="animate-spin" size={18} />
                          ) : (
                            <Users size={18} />
                          )
                        }
                      >
                        Richiedi di partecipare
                      </Button>
                    </form>
                  )}
                </div>

                <aside className="border-t-2 border-ink bg-info/20 p-5 sm:p-7 lg:border-l-2 lg:border-t-0 lg:p-8">
                  <p className="text-xs font-extrabold uppercase tracking-[0.055em] text-muted">
                    Ingresso giocatore
                  </p>
                  <h3 className="mt-3 text-2xl">Chiedi di partecipare</h3>
                  <ol className="mt-6 space-y-4 text-sm leading-5 text-muted">
                    <li className="flex gap-3">
                      <strong className="text-ink">01</strong>
                      <span>Inserisci il nome con cui il DM ti riconosce.</span>
                    </li>
                    <li className="flex gap-3">
                      <strong className="text-ink">02</strong>
                      <span>Invia la richiesta e attendi la conferma.</span>
                    </li>
                    <li className="flex gap-3">
                      <strong className="text-ink">03</strong>
                      <span>
                        Dopo l&apos;accettazione potrai creare personaggio e
                        pedina.
                      </span>
                    </li>
                  </ol>
                </aside>
              </div>
            ) : null}

            {error ? (
              <AlertBanner
                className="m-5 sm:m-7"
                tone="danger"
                icon={<AlertCircle size={18} />}
                role="alert"
              >
                <span>{error}</span>
              </AlertBanner>
            ) : null}

            {table.status !== "DISABLED" ? (
              <div className="flex flex-col gap-3 border-t-2 border-ink bg-muted/10 px-5 py-4 sm:flex-row sm:items-center sm:justify-between sm:px-7">
                <span className="inline-flex items-center gap-2 text-sm font-bold text-muted">
                  <Clock3 size={16} aria-hidden="true" />
                  Stato controllato automaticamente
                </span>
                <Button
                  size="compact"
                  variant="ghost"
                  icon={<RefreshCw size={17} aria-hidden="true" />}
                  onClick={() => void loadTable()}
                >
                  Aggiorna ora
                </Button>
              </div>
            ) : null}
          </>
        ) : null}
      </section>
    </main>
  );
}
