import * as React from "react";
import {
  AlertCircle,
  LoaderCircle,
  MapPin,
  RefreshCw,
  Shield,
} from "lucide-react";
import {
  assumeDmCharacterControl,
  continueDmTrapResolution,
  fetchDmPieceReachability,
  fetchDmTrapResolution,
  moveDmPiece,
  releaseDmCharacterControl,
  rollDmTrapResolution,
} from "../api/boardhubApi";
import type {
  PieceReachability,
  PlayerCharacter,
  SessionPiece,
  TrapResolution,
  TrapRollResult,
} from "../types";
import { createUuid } from "../utils/uuid";
import { BoardGrid } from "./BoardGrid";
import { TrapResolutionCard } from "./TrapResolutionCard";
import { AlertBanner, Button, StatusChip } from "./ui";

type DmControlPanelProps = {
  sessionId: string;
  dmToken: string;
  characters: PlayerCharacter[];
  pieces: SessionPiece[];
  pendingTrapResolutions: TrapResolution[];
  onRefresh: () => Promise<void>;
};

export function DmControlPanel({
  sessionId,
  dmToken,
  characters,
  pieces,
  pendingTrapResolutions,
  onRefresh,
}: DmControlPanelProps) {
  const [busyCharacterId, setBusyCharacterId] = React.useState<string | null>(null);
  const [busyPieceId, setBusyPieceId] = React.useState<string | null>(null);
  const [reachability, setReachability] = React.useState<PieceReachability | null>(null);
  const [trapResolution, setTrapResolution] = React.useState<TrapResolution | null>(null);
  const [trapRoll, setTrapRoll] = React.useState<TrapRollResult | null>(null);
  const [isResolvingTrap, setIsResolvingTrap] = React.useState(false);
  const [message, setMessage] = React.useState<string | null>(null);
  const [error, setError] = React.useState<string | null>(null);

  const characterById = React.useMemo(
    () => new Map(characters.map((character) => [character.characterId, character])),
    [characters],
  );
  const controlledPieces = pieces.filter(
    (piece) => piece.controlMode === "DM_CONTROLLED",
  );
  const boardTokens = pieces.map((piece) => ({
    id: characterById.get(piece.characterId)?.name ?? "Personaggio",
    cell: piece.currentCell,
    kind: "character" as const,
  }));

  async function changeControl(character: PlayerCharacter, isControlled: boolean) {
    setBusyCharacterId(character.characterId);
    setError(null);
    setMessage(null);
    try {
      if (isControlled) {
        await releaseDmCharacterControl(sessionId, character.characterId, dmToken);
        if (reachability?.sessionPieceId === pieces.find((piece) => piece.characterId === character.characterId)?.sessionPieceId) {
          setReachability(null);
        }
        if (trapResolution?.characterId === character.characterId) {
          setTrapResolution(null);
          setTrapRoll(null);
        }
        setMessage(`Controllo restituito a ${character.name}.`);
      } else {
        await assumeDmCharacterControl(sessionId, character.characterId, dmToken);
        setMessage(`Il DM ora controlla ${character.name}.`);
      }
      await onRefresh();
    } catch (controlError) {
      setError(
        controlError instanceof Error
          ? controlError.message
          : "Impossibile aggiornare il controllo del personaggio.",
      );
    } finally {
      setBusyCharacterId(null);
    }
  }

  async function showReachableCells(piece: SessionPiece) {
    setBusyPieceId(piece.sessionPieceId);
    setError(null);
    setMessage(null);
    try {
      setReachability(
        await fetchDmPieceReachability(sessionId, piece.sessionPieceId, dmToken),
      );
    } catch (movementError) {
      setError(
        movementError instanceof Error
          ? movementError.message
          : "Calcolo delle celle raggiungibili non riuscito.",
      );
    } finally {
      setBusyPieceId(null);
    }
  }

  async function moveTo(destination: string) {
    if (!reachability) return;
    setBusyPieceId(reachability.sessionPieceId);
    setError(null);
    setMessage(null);
    try {
      const result = await moveDmPiece(
        sessionId,
        reachability.sessionPieceId,
        dmToken,
        destination,
        reachability.version,
        createUuid(),
      );
      setReachability(null);
      if (result.status === "TRAP_PENDING" && result.resolutionId) {
        setTrapResolution(
          await fetchDmTrapResolution(sessionId, result.resolutionId, dmToken),
        );
        setTrapRoll(null);
      } else {
        setMessage(`Movimento confermato: ${result.from} → ${result.to}.`);
      }
      await onRefresh();
    } catch (movementError) {
      setReachability(null);
      setError(
        movementError instanceof Error
          ? movementError.message
          : "Movimento della pedina non riuscito.",
      );
      await onRefresh();
    } finally {
      setBusyPieceId(null);
    }
  }

  async function openTrapResolution(resolution: TrapResolution) {
    setIsResolvingTrap(true);
    setError(null);
    try {
      setTrapResolution(
        await fetchDmTrapResolution(sessionId, resolution.resolutionId, dmToken),
      );
      setTrapRoll(null);
    } catch (trapError) {
      setError(
        trapError instanceof Error
          ? trapError.message
          : "Impossibile aprire la risoluzione della trappola.",
      );
    } finally {
      setIsResolvingTrap(false);
    }
  }

  async function rollTrapResolution() {
    if (!trapResolution) return;
    setIsResolvingTrap(true);
    setError(null);
    try {
      const result = await rollDmTrapResolution(
        sessionId,
        trapResolution.resolutionId,
        dmToken,
        trapResolution.version,
        createUuid(),
      );
      setTrapRoll(result);
      setTrapResolution((current) =>
        current
          ? {
              ...current,
              status: result.status,
              version: result.version,
              movementRemaining: result.movementRemaining,
            }
          : current,
      );
      await onRefresh();
    } catch (trapError) {
      setError(trapError instanceof Error ? trapError.message : "Tiro salvezza non riuscito.");
      await onRefresh();
    } finally {
      setIsResolvingTrap(false);
    }
  }

  async function continueTrapResolution() {
    if (!trapResolution) return;
    setIsResolvingTrap(true);
    setError(null);
    try {
      const result = await continueDmTrapResolution(
        sessionId,
        trapResolution.resolutionId,
        dmToken,
        trapResolution.version,
        createUuid(),
      );
      if (result.status === "TRAP_PENDING" && result.resolutionId) {
        setTrapResolution(
          await fetchDmTrapResolution(sessionId, result.resolutionId, dmToken),
        );
        setTrapRoll(null);
      } else {
        setTrapResolution(null);
        setTrapRoll(null);
        setMessage(`Movimento confermato: ${result.from} → ${result.to}.`);
      }
      await onRefresh();
    } catch (trapError) {
      setError(
        trapError instanceof Error
          ? trapError.message
          : "Prosecuzione del movimento non riuscita.",
      );
      await onRefresh();
    } finally {
      setIsResolvingTrap(false);
    }
  }

  return (
    <section className="bh-surface mx-auto mb-4 w-full max-w-7xl p-4 sm:p-5">
      <div className="-mx-4 -mt-4 flex min-h-14 items-center gap-3 border-b-2 border-ink bg-accent/40 px-4 py-3 sm:-mx-5 sm:-mt-5 sm:px-5">
        <span className="bh-card-icon"><Shield size={18} /></span>
        <div>
          <h2 className="text-xl">Controllo temporaneo del DM</h2>
          <p className="mt-0.5 text-sm leading-4 text-muted">Assumi una pedina solo quando il giocatore non può operare; restituisci il controllo appena possibile.</p>
        </div>
      </div>

      {error ? <AlertBanner className="mt-4" tone="danger" icon={<AlertCircle size={18} />}>{error}</AlertBanner> : null}
      {message ? <AlertBanner className="mt-4" tone="success" icon={<Shield size={18} />}>{message}</AlertBanner> : null}

      {characters.length === 0 ? (
        <p className="bh-empty-state mt-4 px-3 py-5 text-sm text-muted">Non ci sono personaggi disponibili da gestire.</p>
      ) : (
        <ul className="mt-4 divide-y-2 divide-ink">
          {characters.map((character) => {
            const piece = pieces.find((item) => item.characterId === character.characterId);
            const isControlled = piece?.controlMode === "DM_CONTROLLED";
            return (
              <li className="flex flex-wrap items-center justify-between gap-3 py-3" key={character.characterId}>
                <div>
                  <p className="font-semibold">{character.name}</p>
                  <p className="mt-1 text-xs text-muted">{piece ? `Pedina in ${piece.currentCell}` : "Nessuna pedina associata"}</p>
                </div>
                <div className="flex items-center gap-2">
                  <StatusChip tone={isControlled ? "warning" : "neutral"}>{isControlled ? "DM controlla" : "Giocatore controlla"}</StatusChip>
                  <Button size="compact" variant={isControlled ? "secondary" : "primary"} disabled={busyCharacterId === character.characterId} onClick={() => void changeControl(character, isControlled)} icon={busyCharacterId === character.characterId ? <LoaderCircle className="animate-spin" size={16} /> : <Shield size={16} />}>
                    {isControlled ? "Restituisci" : "Assumi"}
                  </Button>
                </div>
              </li>
            );
          })}
        </ul>
      )}

      {controlledPieces.length > 0 ? (
        <div className="mt-5 border-t-2 border-ink pt-4">
          <h3 className="text-lg">Pedine sotto controllo DM</h3>
          <div className="mt-3 flex flex-wrap gap-2">
            {controlledPieces.map((piece) => (
              <Button key={piece.sessionPieceId} size="compact" variant="secondary" disabled={busyPieceId === piece.sessionPieceId} onClick={() => void showReachableCells(piece)} icon={busyPieceId === piece.sessionPieceId ? <LoaderCircle className="animate-spin" size={16} /> : <MapPin size={16} />}>
                Muovi {characterById.get(piece.characterId)?.name ?? "pedina"}
              </Button>
            ))}
          </div>
        </div>
      ) : null}

      {reachability ? (
        <div className="bh-surface-flat mt-5 bg-canvas p-4">
          <div className="flex items-start justify-between gap-3">
            <div><h3 className="text-lg">Scegli la destinazione</h3><p className="mt-1 text-sm text-muted">{reachability.movementPoints} punti movimento disponibili.</p></div>
            <Button size="compact" variant="ghost" onClick={() => setReachability(null)}>Chiudi</Button>
          </div>
          <BoardGrid tokens={boardTokens} selectableCells={reachability.reachableCells.map((cell) => cell.cell)} onSelectCell={(cell) => void moveTo(cell)} selectionHint="Clicca una cella evidenziata per confermare il movimento del DM." />
        </div>
      ) : null}

      {pendingTrapResolutions.some((resolution) => pieces.find((piece) => piece.characterId === resolution.characterId)?.controlMode === "DM_CONTROLLED") ? (
        <div className="mt-5 border-t-2 border-ink pt-4">
          <h3 className="text-lg">Trappole risolvibili dal DM</h3>
          <div className="mt-3 flex flex-wrap gap-2">
            {pendingTrapResolutions.filter((resolution) => pieces.find((piece) => piece.characterId === resolution.characterId)?.controlMode === "DM_CONTROLLED").map((resolution) => (
              <Button key={resolution.resolutionId} size="compact" variant="danger" disabled={isResolvingTrap} onClick={() => void openTrapResolution(resolution)} icon={isResolvingTrap ? <LoaderCircle className="animate-spin" size={16} /> : <RefreshCw size={16} />}>
                Risolvi {characterById.get(resolution.characterId)?.name ?? "trappola"}
              </Button>
            ))}
          </div>
        </div>
      ) : null}

      {trapResolution ? <TrapResolutionCard isBusy={isResolvingTrap} resolution={trapResolution} roll={trapRoll} onRoll={() => void rollTrapResolution()} onContinue={() => void continueTrapResolution()} /> : null}
    </section>
  );
}
