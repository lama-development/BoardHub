import * as React from "react";
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
import {
  acceptDmJoinRequest,
  closeDmSession,
  fetchDmJoinRequests,
  fetchDmCharacters,
  fetchDmParticipants,
  fetchDmPendingTrapResolutions,
  fetchDmPieces,
  rejectDmJoinRequest,
} from "../api/boardhubApi";
import type {
  JoinRequest,
  Participant,
  PlayerCharacter,
  SessionPiece,
  TrapResolution,
} from "../types";
import {
  AlertBanner,
  Button,
  PageHeaderIdentity,
  StatusChip,
  SummaryCard,
} from "./ui";

type DmSessionPanelProps = {
  sessionId: string;
  sessionTitle: string;
  tableDisplayName: string;
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
  const [requests, setRequests] = React.useState<JoinRequest[]>([]);
  const [participants, setParticipants] = React.useState<Participant[]>([]);
  const [characters, setCharacters] = React.useState<PlayerCharacter[]>([]);
  const [pieces, setPieces] = React.useState<SessionPiece[]>([]);
  const [pendingTrapResolutions, setPendingTrapResolutions] = React.useState<
    TrapResolution[]
  >([]);
  const [error, setError] = React.useState<string | null>(null);
  const [isLoading, setIsLoading] = React.useState(true);
  const [busyRequestId, setBusyRequestId] = React.useState<string | null>(null);
  const [isClosing, setIsClosing] = React.useState(false);

  const refresh = React.useCallback(
    async (showLoading = true) => {
      if (showLoading) setIsLoading(true);
      try {
        const [
          pendingRequests,
          activeParticipants,
          sessionCharacters,
          sessionPieces,
          pendingTraps,
        ] = await Promise.all([
          fetchDmJoinRequests(sessionId, dmToken),
          fetchDmParticipants(sessionId, dmToken),
          fetchDmCharacters(sessionId, dmToken),
          fetchDmPieces(sessionId, dmToken),
          fetchDmPendingTrapResolutions(sessionId, dmToken),
        ]);
        setRequests(pendingRequests);
        setParticipants(activeParticipants);
        setCharacters(sessionCharacters);
        setPieces(sessionPieces);
        setPendingTrapResolutions(pendingTraps);
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
    },
    [dmToken, sessionId],
  );

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
    <main className="min-h-screen bg-transparent px-4 py-5 text-[#111111] sm:px-6 sm:py-8">
      <header className="bh-surface mx-auto flex w-full max-w-7xl flex-col gap-4 p-4 sm:flex-row sm:items-center sm:justify-between sm:p-5">
        <PageHeaderIdentity
          accentClassName="bg-[#ffd400]"
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

      <section className="mx-auto mt-4 grid w-full max-w-7xl gap-3 sm:grid-cols-4">
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
          <div className="-mx-4 -mt-4 flex min-h-14 items-center gap-3 border-b-2 border-[#111111] bg-[#ff8bc7] px-4 py-3 sm:-mx-5 sm:-mt-5 sm:px-5">
            <span className="bh-card-icon">
              <Clock3 size={18} />
            </span>
            <h2 className="text-xl">Richieste di partecipazione</h2>
          </div>
          {requests.length === 0 ? (
            <p className="bh-empty-state mt-4 px-3 py-5 text-sm text-slate-700">
              Nessuna richiesta in attesa. La lista viene aggiornata
              automaticamente.
            </p>
          ) : (
            <ul className="mt-4 divide-y-2 divide-[#111111]">
              {requests.map((request) => (
                <li
                  className="flex flex-col gap-3 py-4 sm:flex-row sm:items-center sm:justify-between"
                  key={request.requestId}
                >
                  <div>
                    <p className="font-medium text-slate-950">
                      {request.displayName}
                    </p>
                    <p className="mt-1 text-xs text-slate-500">
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
          <div className="-mx-4 -mt-4 flex min-h-14 items-center gap-3 border-b-2 border-[#111111] bg-[#8fe8f4] px-4 py-3 sm:-mx-5 sm:-mt-5 sm:px-5">
            <span className="bh-card-icon">
              <Users size={18} />
            </span>
            <h2 className="text-xl">Giocatori accettati</h2>
          </div>
          {players.length === 0 ? (
            <p className="bh-empty-state mt-4 px-3 py-5 text-sm text-slate-700">
              Non ci sono ancora giocatori associati alla sessione.
            </p>
          ) : (
            <ul className="mt-4 divide-y-2 divide-[#111111]">
              {players.map((participant) => (
                <li
                  className="flex items-center gap-3 py-4"
                  key={participant.participantId}
                >
                  <UserRoundCheck
                    className="text-emerald-700"
                    size={20}
                    aria-hidden="true"
                  />
                  <div>
                    <p className="font-medium text-slate-950">
                      {participant.displayName}
                    </p>
                    <p className="mt-1 text-xs text-slate-500">
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
        <div className="-mx-4 -mt-4 flex min-h-14 items-center gap-3 border-b-2 border-[#111111] bg-[#fff2a7] px-4 py-3 sm:-mx-5 sm:-mt-5 sm:px-5">
          <span className="bh-card-icon bg-[#ffd400]">
            <AlertCircle size={18} />
          </span>
          <div>
            <h2 className="text-xl">Trappole in attesa</h2>
            <p className="mt-1 text-sm text-slate-700">
              Il giocatore proprietario risolve il tiro; questa lista viene
              aggiornata automaticamente.
            </p>
          </div>
        </div>
        {pendingTrapResolutions.length === 0 ? (
          <p className="bh-empty-state mt-4 px-3 py-5 text-sm text-slate-700">
            Nessun movimento è fermo su una trappola.
          </p>
        ) : (
          <ul className="mt-4 divide-y-2 divide-[#111111]">
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
                    <p className="mt-1 text-xs text-slate-600">
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

      <section className="mx-auto grid w-full max-w-7xl gap-4 pb-8 lg:grid-cols-2">
        <div className="bh-surface p-4 sm:p-5">
          <div className="-mx-4 -mt-4 flex min-h-14 items-center gap-3 border-b-2 border-[#111111] bg-[#ffd400] px-4 py-3 sm:-mx-5 sm:-mt-5 sm:px-5">
            <span className="bh-card-icon">
              <Heart size={18} />
            </span>
            <h2 className="text-xl">Personaggi della sessione</h2>
          </div>
          {characters.length === 0 ? (
            <p className="bh-empty-state mt-4 px-3 py-5 text-sm text-slate-700">
              I giocatori non hanno ancora creato personaggi.
            </p>
          ) : (
            <ul className="mt-4 divide-y-2 divide-[#111111]">
              {characters.map((character) => (
                <li
                  className="flex items-center justify-between gap-3 py-4"
                  key={character.characterId}
                >
                  <div>
                    <p className="font-medium text-slate-950">
                      {character.name}
                    </p>
                    <p className="mt-1 text-xs text-slate-500">
                      {character.species} · {character.className} · Livello{" "}
                      {character.level}
                    </p>
                  </div>
                  <div className="text-right text-xs text-slate-500">
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
          <div className="-mx-4 -mt-4 flex min-h-14 items-center gap-3 border-b-2 border-[#111111] bg-[#c8b1ff] px-4 py-3 sm:-mx-5 sm:-mt-5 sm:px-5">
            <span className="bh-card-icon">
              <MapPin size={18} />
            </span>
            <h2 className="text-xl">Pedine sulla griglia</h2>
          </div>
          {pieces.length === 0 ? (
            <p className="bh-empty-state mt-4 px-3 py-5 text-sm text-slate-700">
              Non ci sono ancora pedine associate alla griglia.
            </p>
          ) : (
            <ul className="mt-4 divide-y-2 divide-[#111111]">
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
                      <p className="font-medium text-slate-950">
                        {character?.name ?? "Personaggio"}
                      </p>
                      <p className="mt-1 text-xs text-slate-500">
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
