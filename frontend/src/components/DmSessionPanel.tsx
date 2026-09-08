import {
  AlertCircle,
  Check,
  Clock3,
  Heart,
  LoaderCircle,
  LogOut,
  MapPin,
  RefreshCw,
  Shield,
  UserRoundCheck,
  Users,
  X,
} from "lucide-react";
import * as React from "react";
import {
  acceptDmJoinRequest,
  closeDmSession,
  rejectDmJoinRequest,
} from "../api/boardhubApi";
import { useDmSessionData } from "../hooks/useDmSessionData";
import {
  AlertBanner,
  Button,
  PageHeaderIdentity,
  StatusChip,
  SummaryCard,
} from "./ui";
import { DmControlPanel } from "./DmControlPanel";
import { TurnTracker } from "./TurnTracker";

type DmSessionPanelProps = {
  sessionId: string;
  sessionTitle: string;
  dmToken: string;
  onLeave: () => void;
  onSessionClosed: () => void;
};

export function DmSessionPanel({
  sessionId,
  sessionTitle,
  dmToken,
  onLeave,
  onSessionClosed,
}: DmSessionPanelProps) {
  const {
    requests,
    participants,
    characters,
    pieces,
    pendingTrapResolutions,
    error,
    isLoading,
    liveState,
    setError,
    refresh,
  } = useDmSessionData(sessionId, dmToken);
  const [busyRequestId, setBusyRequestId] = React.useState<string | null>(null);
  const [isClosing, setIsClosing] = React.useState(false);

  async function resolveRequest(requestId: string, accepted: boolean) {
    setBusyRequestId(requestId);
    setError(null);
    try {
      if (accepted) {
        await acceptDmJoinRequest(sessionId, requestId, dmToken);
      } else {
        await rejectDmJoinRequest(sessionId, requestId, dmToken);
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

  async function closeSession() {
    if (!window.confirm("Concludere la sessione e disattivare il tavolo?"))
      return;
    setIsClosing(true);
    setError(null);
    try {
      await closeDmSession(sessionId, dmToken);
      onSessionClosed();
    } catch (closeError) {
      setError(
        closeError instanceof Error
          ? closeError.message
          : "Impossibile concludere la sessione.",
      );
      setIsClosing(false);
    }
  }

  const players = participants.filter(
    (participant) => participant.role === "PLAYER",
  );

  return (
    <main className="min-h-screen bg-transparent px-4 py-5 text-ink sm:px-6 sm:py-8">
      <header className="bh-surface mx-auto flex w-full max-w-7xl flex-col gap-4 p-4 sm:flex-row sm:items-center sm:justify-between sm:p-5">
        <PageHeaderIdentity
          accentClassName="bg-warning"
          eyebrow="BoardHub · Area Dungeon Master"
          icon={<Shield size={20} />}
          title={sessionTitle}
        />
        <div className="flex flex-wrap gap-2">
          <Button
            size="compact"
            variant="secondary"
            icon={
              <RefreshCw
                className={isLoading ? "animate-spin" : ""}
                size={17}
                aria-hidden="true"
              />
            }
            onClick={() => void refresh()}
            disabled={isLoading}
          >
            Aggiorna
          </Button>
          <Button
            size="compact"
            variant="secondary"
            icon={<LogOut size={17} aria-hidden="true" />}
            onClick={onLeave}
          >
            Esci dal pannello
          </Button>
          <Button
            size="compact"
            variant="danger"
            icon={
              isClosing ? (
                <LoaderCircle
                  className="animate-spin"
                  size={17}
                  aria-hidden="true"
                />
              ) : (
                <X size={17} aria-hidden="true" />
              )
            }
            onClick={() => void closeSession()}
            disabled={isClosing}
          >
            Concludi sessione
          </Button>
        </div>
      </header>

      <section className="mx-auto mt-4 grid w-full max-w-7xl gap-3 sm:grid-cols-2 xl:grid-cols-5">
        <SummaryCard accent="lime" icon={<Shield size={18} />} label="Stato">
          <StatusChip tone="success" dot>
            Sessione attiva
          </StatusChip>
        </SummaryCard>
        <SummaryCard
          accent="pink"
          icon={<Clock3 size={18} />}
          label="Richieste in attesa"
        >
          <strong className="text-xl font-extrabold leading-tight">
            {requests.length}
          </strong>
        </SummaryCard>
        <SummaryCard
          accent="cyan"
          icon={<Users size={18} />}
          label="Giocatori attivi"
        >
          <strong className="text-xl font-extrabold leading-tight">
            {players.length} / 8
          </strong>
        </SummaryCard>
        <SummaryCard
          accent="yellow"
          icon={<AlertCircle size={18} />}
          label="Trappole in attesa"
        >
          <strong className="text-xl font-extrabold leading-tight">
            {pendingTrapResolutions.length}
          </strong>
        </SummaryCard>
        <SummaryCard
          accent="purple"
          icon={<RefreshCw size={18} />}
          label="Aggiornamenti live"
        >
          <StatusChip
            tone={liveState === "LIVE" ? "success" : "warning"}
            dot={liveState === "LIVE"}
          >
            {liveState === "LIVE" ? "Connessi" : "Riconnessione"}
          </StatusChip>
        </SummaryCard>
      </section>

      {error ? (
        <AlertBanner
          className="mx-auto mt-4 w-full max-w-7xl"
          tone="danger"
          icon={<AlertCircle size={18} />}
          role="alert"
        >
          <span>{error}</span>
        </AlertBanner>
      ) : null}

      <section className="mx-auto grid w-full max-w-7xl gap-4 py-4 lg:grid-cols-2">
        <div className="bh-surface p-4 sm:p-5">
          <div className="-mx-4 -mt-4 flex min-h-14 items-center gap-3 border-b-2 border-ink bg-primary/40 px-4 py-3 sm:-mx-5 sm:-mt-5 sm:px-5">
            <span className="bh-card-icon">
              <Clock3 size={18} />
            </span>
            <h2 className="text-xl">Richieste di partecipazione</h2>
          </div>
          {requests.length === 0 ? (
            <p className="bh-empty-state mt-4 px-3 py-5 text-sm text-muted">
              Nessuna richiesta in attesa. La lista viene aggiornata
              automaticamente.
            </p>
          ) : (
            <ul className="mt-4 divide-y-2 divide-ink">
              {requests.map((request) => (
                <li
                  className="flex flex-col gap-3 py-4 sm:flex-row sm:items-center sm:justify-between"
                  key={request.requestId}
                >
                  <div>
                    <p className="font-medium text-ink">
                      {request.displayName}
                    </p>
                    <p className="mt-1 text-xs text-muted">
                      {request.playerReference}
                    </p>
                  </div>
                  <div className="flex gap-2">
                    <Button
                      size="compact"
                      variant="success"
                      icon={<Check size={17} aria-hidden="true" />}
                      disabled={busyRequestId === request.requestId}
                      onClick={() =>
                        void resolveRequest(request.requestId, true)
                      }
                    >
                      Accetta
                    </Button>
                    <Button
                      size="compact"
                      variant="secondary"
                      icon={<X size={17} aria-hidden="true" />}
                      disabled={busyRequestId === request.requestId}
                      onClick={() =>
                        void resolveRequest(request.requestId, false)
                      }
                    >
                      Rifiuta
                    </Button>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="bh-surface p-4 sm:p-5">
          <div className="-mx-4 -mt-4 flex min-h-14 items-center gap-3 border-b-2 border-ink bg-info/40 px-4 py-3 sm:-mx-5 sm:-mt-5 sm:px-5">
            <span className="bh-card-icon">
              <Users size={18} />
            </span>
            <h2 className="text-xl">Giocatori accettati</h2>
          </div>
          {players.length === 0 ? (
            <p className="bh-empty-state mt-4 px-3 py-5 text-sm text-muted">
              Non ci sono ancora giocatori associati alla sessione.
            </p>
          ) : (
            <ul className="mt-4 divide-y-2 divide-ink">
              {players.map((participant) => (
                <li
                  className="flex items-center gap-3 py-4"
                  key={participant.participantId}
                >
                  <UserRoundCheck
                    className="text-success"
                    size={20}
                    aria-hidden="true"
                  />
                  <div>
                    <p className="font-medium text-ink">
                      {participant.displayName}
                    </p>
                    <p className="mt-1 text-xs text-muted">
                      {participant.status}
                    </p>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </section>

      <section className="bh-surface mx-auto mb-4 w-full max-w-7xl p-4 sm:p-5">
        <div className="-mx-4 -mt-4 flex min-h-14 items-center gap-3 border-b-2 border-ink bg-warning/40 px-4 py-3 sm:-mx-5 sm:-mt-5 sm:px-5">
          <span className="bh-card-icon">
            <AlertCircle size={18} />
          </span>
          <div>
            <h2 className="text-xl">Trappole in attesa</h2>
            <p className="mt-0.5 text-sm leading-4 text-muted">
              Il giocatore proprietario risolve il tiro, oppure il DM può
              assumerne temporaneamente il controllo qui sotto.
            </p>
          </div>
        </div>
        {pendingTrapResolutions.length === 0 ? (
          <p className="bh-empty-state mt-4 px-3 py-5 text-sm text-muted">
            Nessun movimento è fermo su una trappola.
          </p>
        ) : (
          <ul className="mt-4 divide-y-2 divide-ink">
            {pendingTrapResolutions.map((resolution) => {
              const character = characters.find(
                (item) => item.characterId === resolution.characterId,
              );
              return (
                <li
                  className="flex flex-wrap items-center justify-between gap-3 py-3"
                  key={resolution.resolutionId}
                >
                  <div>
                    <p className="font-semibold">
                      {character?.name ?? "Personaggio"}
                    </p>
                    <p className="mt-1 text-xs text-muted">
                      Trappola in {resolution.triggerCell} · destinazione{" "}
                      {resolution.requestedDestination}
                    </p>
                  </div>
                  <StatusChip tone="warning">
                    {resolution.status === "AWAITING_ROLL"
                      ? "Tiro richiesto"
                      : "Prosecuzione"}
                  </StatusChip>
                </li>
              );
            })}
          </ul>
        )}
      </section>

      <DmControlPanel
        sessionId={sessionId}
        dmToken={dmToken}
        characters={characters}
        pieces={pieces}
        pendingTrapResolutions={pendingTrapResolutions}
        onRefresh={() => refresh(false)}
      />

      <TurnTracker sessionId={sessionId} characters={characters} />

      <section className="mx-auto grid w-full max-w-7xl gap-4 pb-8 lg:grid-cols-2">
        <div className="bh-surface p-4 sm:p-5">
          <div className="-mx-4 -mt-4 flex min-h-14 items-center gap-3 border-b-2 border-ink bg-warning px-4 py-3 sm:-mx-5 sm:-mt-5 sm:px-5">
            <span className="bh-card-icon">
              <Heart size={18} />
            </span>
            <h2 className="text-xl">Personaggi della sessione</h2>
          </div>
          {characters.length === 0 ? (
            <p className="bh-empty-state mt-4 px-3 py-5 text-sm text-muted">
              I giocatori non hanno ancora creato personaggi.
            </p>
          ) : (
            <ul className="mt-4 divide-y-2 divide-ink">
              {characters.map((character) => (
                <li
                  className="flex items-center justify-between gap-3 py-4"
                  key={character.characterId}
                >
                  <div>
                    <p className="font-medium text-ink">
                      {character.name}
                    </p>
                    <p className="mt-1 text-xs text-muted">
                      {character.species} · {character.className} · Livello{" "}
                      {character.level}
                    </p>
                  </div>
                  <div className="text-right text-xs text-muted">
                    <p>
                      PF {character.hpCurrent}/{character.hpMax}
                    </p>
                    <p className="mt-1">CA {character.armorClass}</p>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="bh-surface p-4 sm:p-5">
          <div className="-mx-4 -mt-4 flex min-h-14 items-center gap-3 border-b-2 border-ink bg-accent/40 px-4 py-3 sm:-mx-5 sm:-mt-5 sm:px-5">
            <span className="bh-card-icon">
              <MapPin size={18} />
            </span>
            <h2 className="text-xl">Pedine sulla griglia</h2>
          </div>
          {pieces.length === 0 ? (
            <p className="bh-empty-state mt-4 px-3 py-5 text-sm text-muted">
              Non ci sono ancora pedine associate alla griglia.
            </p>
          ) : (
            <ul className="mt-4 divide-y-2 divide-ink">
              {pieces.map((piece) => {
                const character = characters.find(
                  (item) => item.characterId === piece.characterId,
                );
                return (
                  <li
                    className="flex items-center justify-between gap-3 py-4"
                    key={piece.sessionPieceId}
                  >
                    <div>
                      <p className="font-medium text-ink">
                        {character?.name ?? "Personaggio"}
                      </p>
                      <p className="mt-1 text-xs text-muted">
                        {piece.representationMode.toLowerCase()} · versione{" "}
                        {piece.version}
                      </p>
                    </div>
                    <StatusChip tone="info">
                      Cella {piece.currentCell}
                    </StatusChip>
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      </section>
    </main>
  );
}
