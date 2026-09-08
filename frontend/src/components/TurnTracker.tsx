import * as React from "react";
import {
  ChevronRight,
  CirclePause,
  CirclePlay,
  Dices,
  RotateCcw,
  Swords,
  Trophy,
  UserPlus,
  X,
} from "lucide-react";
import type { PlayerCharacter } from "../types";
import { createUuid } from "../utils/uuid";
import { AlertBanner, Button, StatusChip } from "./ui";

type TurnEntry = {
  characterId: string;
  initiative: number | null;
  label?: string;
};

type TurnTrackerState = {
  version: 1;
  status: "SETUP" | "ACTIVE" | "PAUSED";
  round: number;
  currentIndex: number;
  entries: TurnEntry[];
};

type TurnTrackerProps = {
  sessionId: string;
  characters: PlayerCharacter[];
};

function storageKey(sessionId: string) {
  return `boardhub-turns-${sessionId}`;
}

function emptyState(): TurnTrackerState {
  return {
    version: 1,
    status: "SETUP",
    round: 1,
    currentIndex: 0,
    entries: [],
  };
}

function readState(sessionId: string): TurnTrackerState {
  try {
    const value = window.localStorage.getItem(storageKey(sessionId));
    if (!value) return emptyState();
    const parsed = JSON.parse(value) as TurnTrackerState;
    if (parsed.version !== 1 || !Array.isArray(parsed.entries)) return emptyState();
    return parsed;
  } catch {
    return emptyState();
  }
}

export function TurnTracker({ sessionId, characters }: TurnTrackerProps) {
  const [tracker, setTracker] = React.useState<TurnTrackerState>(() =>
    readState(sessionId),
  );
  const [customCombatant, setCustomCombatant] = React.useState("");
  const characterById = React.useMemo(
    () => new Map(characters.map((character) => [character.characterId, character])),
    [characters],
  );

  React.useEffect(() => {
    setTracker((current) => {
      if (current.status !== "SETUP") return current;
      const knownIds = new Set(current.entries.map((entry) => entry.characterId));
      const additions = characters
        .filter((character) => !knownIds.has(character.characterId))
        .map((character) => ({
          characterId: character.characterId,
          initiative: null,
        }));
      if (additions.length === 0) return current;
      return { ...current, entries: [...current.entries, ...additions] };
    });
  }, [characters]);

  React.useEffect(() => {
    window.localStorage.setItem(storageKey(sessionId), JSON.stringify(tracker));
  }, [sessionId, tracker]);

  const currentEntry = tracker.entries[tracker.currentIndex] ?? null;
  const canStart =
    tracker.entries.length > 0 &&
    tracker.entries.every((entry) => entry.initiative !== null);

  function entryName(entry: TurnEntry) {
    return (
      entry.label ??
      characterById.get(entry.characterId)?.name ??
      "Personaggio"
    );
  }

  function addCustomCombatant(event: React.FormEvent) {
    event.preventDefault();
    const label = customCombatant.trim();
    if (!label) return;
    setTracker((current) => ({
      ...current,
      entries: [
        ...current.entries,
        {
          characterId: `custom-${createUuid()}`,
          initiative: null,
          label,
        },
      ],
    }));
    setCustomCombatant("");
  }

  function removeCustomCombatant(characterId: string) {
    setTracker((current) => ({
      ...current,
      entries: current.entries.filter(
        (entry) => entry.characterId !== characterId,
      ),
    }));
  }

  function updateInitiative(characterId: string, value: string) {
    const initiative = value === "" ? null : Number(value);
    setTracker((current) => ({
      ...current,
      entries: current.entries.map((entry) =>
        entry.characterId === characterId ? { ...entry, initiative } : entry,
      ),
    }));
  }

  function startEncounter() {
    if (!canStart) return;
    setTracker((current) => ({
      ...current,
      status: "ACTIVE",
      round: 1,
      currentIndex: 0,
      entries: [...current.entries].sort(
        (left, right) => (right.initiative ?? 0) - (left.initiative ?? 0),
      ),
    }));
  }

  function advanceTurn() {
    setTracker((current) => {
      if (current.entries.length === 0) return current;
      const wraps = current.currentIndex >= current.entries.length - 1;
      return {
        ...current,
        currentIndex: wraps ? 0 : current.currentIndex + 1,
        round: wraps ? current.round + 1 : current.round,
      };
    });
  }

  function resetEncounter() {
    if (!window.confirm("Preparare un nuovo ordine di iniziativa?")) return;
    setTracker((current) => ({
      ...emptyState(),
      entries: current.entries.map((entry) => ({
        ...entry,
        initiative: null,
      })),
    }));
  }

  return (
    <section className="bh-surface mx-auto mb-4 w-full max-w-7xl p-4 sm:p-5">
      <div className="-mx-4 -mt-4 flex min-h-14 flex-col gap-3 border-b-2 border-ink bg-success/40 px-4 py-3 sm:-mx-5 sm:-mt-5 sm:flex-row sm:items-center sm:justify-between sm:px-5">
        <div className="flex items-center gap-3">
          <span className="bh-card-icon"><Swords size={18} /></span>
          <div>
            <h2 className="text-xl">Turni e iniziativa</h2>
            <p className="mt-0.5 text-sm leading-4 text-muted">
              Ordine del combattimento salvato su questo dispositivo DM.
            </p>
          </div>
        </div>
        <div className="flex gap-2">
          <StatusChip tone="warning">Round {tracker.round}</StatusChip>
          <StatusChip
            dot
            tone={tracker.status === "ACTIVE" ? "success" : tracker.status === "PAUSED" ? "warning" : "neutral"}
          >
            {tracker.status === "ACTIVE" ? "In corso" : tracker.status === "PAUSED" ? "In pausa" : "Da preparare"}
          </StatusChip>
        </div>
      </div>

      {tracker.status === "SETUP" ? (
        <div className="mt-4 grid gap-5 lg:grid-cols-[minmax(0,1fr)_20rem]">
          <div>
            <p className="text-sm leading-6 text-muted">
              Inserisci il risultato dell&apos;iniziativa per personaggi, mostri e
              PNG. Al via, l&apos;ordine viene calcolato dal valore più alto al basso.
            </p>
            <form className="mt-4 flex gap-2" onSubmit={addCustomCombatant}>
              <label className="sr-only" htmlFor="customCombatant">Nome mostro o PNG</label>
              <input
                className="bh-input h-10 min-w-0 flex-1 px-3"
                id="customCombatant"
                maxLength={60}
                placeholder="Aggiungi mostro o PNG"
                value={customCombatant}
                onChange={(event) => setCustomCombatant(event.target.value)}
              />
              <Button
                type="submit"
                variant="secondary"
                disabled={!customCombatant.trim()}
                icon={<UserPlus size={17} />}
              >
                Aggiungi
              </Button>
            </form>
            {tracker.entries.length === 0 ? (
              <div className="bh-empty-state mt-4 px-4 py-6 text-center">
                <Dices className="mx-auto" size={23} />
                <p className="mt-3 font-bold">Nessun combattente</p>
                <p className="mt-1 text-sm text-muted">
                  I personaggi dei giocatori compariranno automaticamente.
                </p>
              </div>
            ) : (
            <ol className="mt-3 divide-y-2 divide-ink border-y-2 border-ink">
              {tracker.entries.map((entry) => (
                <li className="flex items-center justify-between gap-4 py-3" key={entry.characterId}>
                  <div className="min-w-0 flex-1">
                    <p className="font-extrabold">
                      {entryName(entry)}
                    </p>
                    <p className="mt-1 text-xs text-muted">
                      {entry.label
                        ? "Mostro / PNG"
                        : characterById.get(entry.characterId)?.className ?? "Personaggio giocante"}
                    </p>
                  </div>
                  <label className="flex items-center gap-2 text-xs font-extrabold uppercase tracking-[0.04em]">
                    Iniziativa
                    <input
                      aria-label={`Iniziativa di ${entryName(entry)}`}
                      className="bh-input h-10 w-20 px-2 text-center text-base"
                      max={99}
                      min={-10}
                      type="number"
                      value={entry.initiative ?? ""}
                      onChange={(event) => updateInitiative(entry.characterId, event.target.value)}
                    />
                  </label>
                  {entry.label ? (
                    <button
                      aria-label={`Rimuovi ${entry.label}`}
                      className="grid h-9 w-9 shrink-0 place-items-center border-2 border-ink bg-surface shadow-[2px_2px_0_var(--color-ink)] hover:bg-primary/40"
                      onClick={() => removeCustomCombatant(entry.characterId)}
                      type="button"
                    >
                      <X size={16} />
                    </button>
                  ) : null}
                </li>
              ))}
            </ol>
            )}
          </div>
          <aside className="border-2 border-ink bg-success/20 p-4 shadow-[3px_3px_0_var(--color-ink)]">
            <Trophy size={24} />
            <h3 className="mt-4 text-xl">Avvia il combattimento</h3>
            <p className="mt-2 text-sm leading-5 text-muted">
              Servono le iniziative di tutti i personaggi presenti.
            </p>
            <Button
              className="mt-5 w-full"
              variant="primary"
              disabled={!canStart}
              icon={<CirclePlay size={17} />}
              onClick={startEncounter}
            >
              Avvia turni
            </Button>
          </aside>
        </div>
      ) : (
        <div className="mt-4 grid gap-4 lg:grid-cols-[minmax(0,0.8fr)_minmax(20rem,1.2fr)]">
          <div className="border-2 border-ink bg-warning/40 p-5 shadow-[3px_3px_0_var(--color-ink)]">
            <p className="text-xs font-extrabold uppercase tracking-[0.055em] text-muted">Turno attuale</p>
            <h3 className="mt-3 text-3xl">{currentEntry ? entryName(currentEntry) : "Personaggio"}</h3>
            <p className="mt-2 text-sm text-muted">
              Iniziativa {currentEntry?.initiative ?? "—"} · Round {tracker.round}
            </p>
            <Button
              className="mt-6 w-full"
              variant="primary"
              disabled={tracker.status === "PAUSED"}
              icon={<ChevronRight size={18} />}
              onClick={advanceTurn}
            >
              Termina turno
            </Button>
            <div className="mt-3 flex gap-2">
              <Button
                className="flex-1"
                size="compact"
                variant="secondary"
                icon={tracker.status === "PAUSED" ? <CirclePlay size={16} /> : <CirclePause size={16} />}
                onClick={() => setTracker((current) => ({ ...current, status: current.status === "PAUSED" ? "ACTIVE" : "PAUSED" }))}
              >
                {tracker.status === "PAUSED" ? "Riprendi" : "Pausa"}
              </Button>
              <Button
                size="compact"
                variant="secondary"
                icon={<RotateCcw size={16} />}
                onClick={resetEncounter}
              >
                Nuovo
              </Button>
            </div>
          </div>

          <ol className="divide-y-2 divide-ink border-2 border-ink bg-surface px-4 shadow-[3px_3px_0_var(--color-ink)]">
            {tracker.entries.map((entry, index) => {
              const isCurrent = index === tracker.currentIndex;
              return (
                <li
                  className={`flex items-center gap-3 py-3 ${isCurrent ? "bg-success/20 -mx-4 px-4" : ""}`}
                  key={entry.characterId}
                >
                  <span className={`grid h-8 w-8 place-items-center border-2 border-ink font-black ${isCurrent ? "bg-success/40" : "bg-surface"}`}>
                    {index + 1}
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="truncate font-extrabold">{entryName(entry)}</p>
                    <p className="mt-0.5 text-xs text-muted">Iniziativa {entry.initiative}</p>
                  </div>
                  {isCurrent ? <StatusChip tone="success">Attivo</StatusChip> : null}
                </li>
              );
            })}
          </ol>
        </div>
      )}

      <AlertBanner className="mt-4" tone="warning">
        Il tracker dei turni è locale al frontend: terreno e trappole sono già
        autorevoli sul backend, mentre la sincronizzazione dei turni con i
        dispositivi dei giocatori richiederà i relativi endpoint.
      </AlertBanner>
    </section>
  );
}
