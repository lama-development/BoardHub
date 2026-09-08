import * as React from "react";
import {
  AlertCircle,
  ArrowLeft,
  Check,
  ChevronLeft,
  ChevronRight,
  CircleOff,
  Dices,
  Eye,
  EyeOff,
  Footprints,
  LoaderCircle,
  Shield,
  Sparkles,
  TriangleAlert,
} from "lucide-react";
import type {
  SessionGridConfiguration,
  SessionTrapConfiguration,
} from "../types";
import { AlertBanner, Button, PageHeaderIdentity, StatusChip } from "./ui";

type Terrain = "NORMAL" | "DIFFICULT" | "BLOCKED" | "OBSTACLE";
type BoardTool = Terrain | "TRAP";

type DmSessionSetupPageProps = {
  tableDisplayName: string;
  sessionTitle: string;
  error: string | null;
  isSubmitting: boolean;
  onSessionTitleChange: (value: string) => void;
  onCancel: () => void;
  onSubmit: (grid: SessionGridConfiguration) => Promise<void>;
};

const COLUMNS = ["A", "B", "C", "D", "E"];
const ROWS = [5, 4, 3, 2, 1];

const TOOL_LABELS: Record<BoardTool, string> = {
  NORMAL: "Libera",
  DIFFICULT: "Difficile",
  BLOCKED: "Bloccata",
  OBSTACLE: "Ostacolo",
  TRAP: "Trappola",
};

function defaultTrap(cell: string): SessionTrapConfiguration {
  return {
    trapId: `trap-${cell.toLowerCase()}`,
    cell,
    visibility: "HIDDEN",
    armed: true,
    lifecyclePolicy: "ONE_SHOT",
    saveAbility: "DEXTERITY",
    saveDc: 12,
    rollMode: "NORMAL",
    damageExpression: "1d6",
    successDamage: "NONE",
    successMovement: "CONTINUE",
    failureDamage: "FULL",
    failureMovement: "STOP",
  };
}

export function DmSessionSetupPage({
  tableDisplayName,
  sessionTitle,
  error,
  isSubmitting,
  onSessionTitleChange,
  onCancel,
  onSubmit,
}: DmSessionSetupPageProps) {
  const [step, setStep] = React.useState(0);
  const [activeTool, setActiveTool] = React.useState<BoardTool>("TRAP");
  const [terrainByCell, setTerrainByCell] = React.useState<
    Record<string, Terrain>
  >({});
  const [trapsByCell, setTrapsByCell] = React.useState<
    Record<string, SessionTrapConfiguration>
  >({});
  const [selectedTrapCell, setSelectedTrapCell] = React.useState<string | null>(
    null,
  );

  const selectedTrap = selectedTrapCell
    ? (trapsByCell[selectedTrapCell] ?? null)
    : null;
  const specialTerrainCount = Object.values(terrainByCell).filter(
    (terrain) => terrain !== "NORMAL",
  ).length;
  const trapCount = Object.keys(trapsByCell).length;
  const invalidTrapCount = Object.values(trapsByCell).filter(
    (trap) =>
      !Number.isInteger(trap.saveDc) ||
      trap.saveDc < 1 ||
      trap.saveDc > 30 ||
      !/^\d+d\d+(?:[+-]\d+)?$/i.test(trap.damageExpression.trim()),
  ).length;

  function paintCell(cell: string) {
    if (activeTool === "TRAP") {
      if (
        terrainByCell[cell] === "BLOCKED" ||
        terrainByCell[cell] === "OBSTACLE"
      ) {
        setTerrainByCell((current) => ({ ...current, [cell]: "NORMAL" }));
      }
      setTrapsByCell((current) =>
        current[cell] ? current : { ...current, [cell]: defaultTrap(cell) },
      );
      setSelectedTrapCell(cell);
      return;
    }

    setTerrainByCell((current) => ({ ...current, [cell]: activeTool }));
    if (activeTool === "BLOCKED" || activeTool === "OBSTACLE") {
      setTrapsByCell((current) => {
        if (!current[cell]) return current;
        const next = { ...current };
        delete next[cell];
        return next;
      });
      if (selectedTrapCell === cell) setSelectedTrapCell(null);
    }
  }

  function updateSelectedTrap(patch: Partial<SessionTrapConfiguration>) {
    if (!selectedTrapCell) return;
    setTrapsByCell((current) => ({
      ...current,
      [selectedTrapCell]: { ...current[selectedTrapCell], ...patch },
    }));
  }

  function removeSelectedTrap() {
    if (!selectedTrapCell) return;
    setTrapsByCell((current) => {
      const next = { ...current };
      delete next[selectedTrapCell];
      return next;
    });
    setSelectedTrapCell(null);
  }

  function buildGrid(): SessionGridConfiguration {
    const cells = Object.entries(terrainByCell);
    return {
      width: 5,
      height: 5,
      difficultCells: cells
        .filter(([, terrain]) => terrain === "DIFFICULT")
        .map(([cell]) => cell),
      blockedCells: cells
        .filter(([, terrain]) => terrain === "BLOCKED")
        .map(([cell]) => cell),
      obstacleCells: cells
        .filter(([, terrain]) => terrain === "OBSTACLE")
        .map(([cell]) => cell),
      occupiedCells: [],
      walls: [],
      traps: Object.values(trapsByCell),
    };
  }

  const canContinue = step !== 0 || sessionTitle.trim().length > 0;

  return (
    <main className="min-h-screen bg-transparent px-3 py-4 text-ink sm:px-6 sm:py-7">
      <header className="bh-surface mx-auto flex w-full max-w-7xl flex-col gap-4 p-4 sm:flex-row sm:items-center sm:justify-between sm:p-5">
        <PageHeaderIdentity
          accentClassName="bg-warning"
          eyebrow={`BoardHub · ${tableDisplayName}`}
          icon={<Shield size={20} />}
          title="Preparazione DM"
        />
        <Button
          size="compact"
          variant="secondary"
          icon={<ArrowLeft size={17} aria-hidden="true" />}
          onClick={onCancel}
        >
          Annulla
        </Button>
      </header>

      <nav
        className="mx-auto mt-4 grid w-full max-w-7xl grid-cols-3 gap-2"
        aria-label="Fasi di preparazione"
      >
        {["Avventura", "Plancia", "Riepilogo"].map((label, index) => (
          <button
            className={`min-h-14 border-2 border-ink px-2 text-xs font-extrabold uppercase tracking-[0.04em] shadow-[2px_2px_0_var(--color-ink)] sm:text-sm ${
              step === index
                ? "bg-warning"
                : index < step
                  ? "bg-success/40"
                  : "bg-surface"
            }`}
            key={label}
            onClick={() => {
              if (index === 0 || sessionTitle.trim()) setStep(index);
            }}
            type="button"
          >
            <span className="mr-2 hidden sm:inline">0{index + 1}</span>
            {label}
          </button>
        ))}
      </nav>

      {error ? (
        <AlertBanner
          className="mx-auto mt-4 w-full max-w-7xl"
          tone="danger"
          icon={<AlertCircle size={18} />}
        >
          {error}
        </AlertBanner>
      ) : null}

      <section className="bh-surface mx-auto mt-4 w-full max-w-7xl overflow-hidden">
        {step === 0 ? (
          <div className="grid lg:grid-cols-[minmax(0,1.3fr)_minmax(18rem,0.7fr)]">
            <div className="p-5 sm:p-8 lg:p-10">
              <p className="bh-kicker bg-info/40">01 · Avventura</p>
              <h1 className="mt-5 text-4xl sm:text-5xl">
                Dai un nome alla sessione
              </h1>
              <p className="mt-4 max-w-2xl leading-7 text-muted">
                La sessione verrà creata soltanto alla fine della preparazione.
                Fino ad allora il tavolo resta disponibile.
              </p>
              <label
                className="mt-8 block text-sm font-bold text-muted"
                htmlFor="setupSessionTitle"
              >
                Titolo della sessione
              </label>
              <input
                autoFocus
                className="bh-input mt-2 h-12 w-full max-w-2xl px-3 text-lg"
                id="setupSessionTitle"
                maxLength={120}
                required
                value={sessionTitle}
                onChange={(event) => onSessionTitleChange(event.target.value)}
              />
            </div>
            <aside className="border-t-2 border-ink bg-info/20 p-5 sm:p-7 lg:border-l-2 lg:border-t-0 lg:p-8">
              <span className="bh-card-icon bg-surface h-12 w-12">
                <Sparkles size={23} />
              </span>
              <h2 className="mt-6 text-2xl">Prima di giocare</h2>
              <p className="mt-3 text-sm leading-6 text-muted">
                Disegnerai il terreno, posizionerai le trappole e controllerai
                il riepilogo prima di rendere la sessione visibile ai giocatori.
              </p>
            </aside>
          </div>
        ) : null}

        {step === 1 ? (
          <div className="grid xl:grid-cols-[minmax(0,1fr)_23rem]">
            <div className="p-4 sm:p-6 lg:p-8">
              <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
                <div>
                  <p className="bh-kicker bg-primary/40">02 · Plancia</p>
                  <h1 className="mt-4 text-3xl sm:text-4xl">
                    Prepara la mappa 5 × 5
                  </h1>
                </div>
                <div className="flex gap-2">
                  <StatusChip tone="warning">{trapCount} trappole</StatusChip>
                  <StatusChip tone="info">
                    {specialTerrainCount} terreni
                  </StatusChip>
                </div>
              </div>

              <div
                className="mt-6 flex flex-wrap gap-2"
                role="toolbar"
                aria-label="Strumenti plancia"
              >
                {(Object.keys(TOOL_LABELS) as BoardTool[]).map((tool) => (
                  <button
                    aria-pressed={activeTool === tool}
                    className={`min-h-10 border-2 border-ink px-3 text-xs font-extrabold uppercase tracking-[0.04em] shadow-[2px_2px_0_var(--color-ink)] transition-transform hover:-translate-y-0.5 ${
                      activeTool === tool ? "bg-warning" : "bg-surface"
                    }`}
                    key={tool}
                    onClick={() => setActiveTool(tool)}
                    type="button"
                  >
                    {TOOL_LABELS[tool]}
                  </button>
                ))}
              </div>

              <p className="mt-3 text-sm text-muted">
                Strumento attivo: <strong>{TOOL_LABELS[activeTool]}</strong>.
                Clicca sulle celle per applicarlo.
              </p>

              <div className="mt-5 overflow-x-auto pb-1">
                <div className="mx-auto grid min-w-82.5 max-w-155 grid-cols-[24px_repeat(5,minmax(52px,1fr))] gap-1.5">
                  <div />
                  {COLUMNS.map((column) => (
                    <span
                      className="text-center text-xs font-bold text-muted"
                      key={column}
                    >
                      {column}
                    </span>
                  ))}
                  {ROWS.map((row) => (
                    <React.Fragment key={row}>
                      <span className="grid place-items-center text-xs font-bold text-muted">
                        {row}
                      </span>
                      {COLUMNS.map((column) => {
                        const cell = `${column}${row}`;
                        const terrain = terrainByCell[cell] ?? "NORMAL";
                        const trap = trapsByCell[cell];
                        const terrainClass =
                          terrain === "DIFFICULT"
                            ? "bg-warning/40"
                            : terrain === "BLOCKED"
                              ? "bg-ink text-surface"
                              : terrain === "OBSTACLE"
                                ? "bg-accent/40"
                                : "bg-canvas";
                        return (
                          <button
                            aria-label={`Cella ${cell}, ${TOOL_LABELS[terrain]}${trap ? ", con trappola" : ""}`}
                            className={`relative aspect-square min-h-13 border-2 border-ink p-1 font-mono text-xs font-bold transition-all hover:-translate-y-0.5 hover:shadow-[3px_3px_0_var(--color-ink)] ${terrainClass} ${
                              selectedTrapCell === cell
                                ? "ring-4 ring-primary ring-offset-1"
                                : ""
                            }`}
                            key={cell}
                            onClick={() => paintCell(cell)}
                            type="button"
                          >
                            <span className="absolute left-1 top-1 opacity-60">
                              {cell}
                            </span>
                            {terrain === "DIFFICULT" ? (
                              <Footprints size={22} className="mx-auto" />
                            ) : null}
                            {terrain === "BLOCKED" ? (
                              <CircleOff size={22} className="mx-auto" />
                            ) : null}
                            {terrain === "OBSTACLE" ? (
                              <TriangleAlert size={22} className="mx-auto" />
                            ) : null}
                            {trap ? (
                              <span className="absolute bottom-1 right-1 grid h-6 w-6 place-items-center rounded-full border-2 border-ink bg-primary text-ink">
                                {trap.visibility === "REVEALED" ? (
                                  <Eye size={13} />
                                ) : (
                                  <EyeOff size={13} />
                                )}
                              </span>
                            ) : null}
                          </button>
                        );
                      })}
                    </React.Fragment>
                  ))}
                </div>
              </div>
            </div>

            <aside className="border-t-2 border-ink bg-warning/20 p-4 sm:p-6 xl:border-l-2 xl:border-t-0">
              {selectedTrap ? (
                <TrapEditor
                  trap={selectedTrap}
                  onChange={updateSelectedTrap}
                  onRemove={removeSelectedTrap}
                />
              ) : (
                <div className="flex min-h-80 flex-col items-center justify-center text-center">
                  <span className="bh-card-icon bg-primary/40 h-12 w-12">
                    <TriangleAlert size={23} />
                  </span>
                  <h2 className="mt-5 text-2xl">Configura una trappola</h2>
                  <p className="mt-3 max-w-xs text-sm leading-6 text-muted">
                    Seleziona lo strumento Trappola e poi una cella. I dettagli
                    restano visibili soltanto al DM.
                  </p>
                </div>
              )}
            </aside>
          </div>
        ) : null}

        {step === 2 ? (
          <div className="grid lg:grid-cols-[minmax(0,1.25fr)_minmax(19rem,0.75fr)]">
            <div className="p-5 sm:p-8 lg:p-10">
              <p className="bh-kicker bg-success/40">03 · Riepilogo</p>
              <h1 className="mt-5 text-4xl sm:text-5xl">Pronto a giocare</h1>
              <div className="mt-7 grid gap-3 sm:grid-cols-3">
                <SetupSummary label="Sessione" value={sessionTitle.trim()} />
                <SetupSummary
                  label="Terreni speciali"
                  value={String(specialTerrainCount)}
                />
                <SetupSummary
                  label="Trappole armate"
                  value={String(
                    Object.values(trapsByCell).filter((trap) => trap.armed)
                      .length,
                  )}
                />
              </div>
              <AlertBanner
                className="mt-6"
                tone="info"
                icon={<Dices size={18} />}
              >
                Dopo l&apos;ingresso dei giocatori potrai inserire le
                iniziative, avviare il combattimento e avanzare turni e round
                dalla console DM.
              </AlertBanner>
              {invalidTrapCount > 0 ? (
                <AlertBanner
                  className="mt-4"
                  tone="danger"
                  icon={<AlertCircle size={18} />}
                >
                  {invalidTrapCount === 1
                    ? "Una trappola ha CD o formula di danno non validi."
                    : `${invalidTrapCount} trappole hanno CD o formula di danno non validi.`}
                </AlertBanner>
              ) : null}
              <p className="mt-5 text-sm leading-6 text-muted">
                Creando la sessione, terreno e trappole vengono salvati nel
                backend. Le trappole nascoste non saranno esposte ai giocatori.
              </p>
            </div>
            <aside className="border-t-2 border-ink bg-success/20 p-5 sm:p-7 lg:border-l-2 lg:border-t-0 lg:p-8">
              <span className="bh-card-icon bg-surface h-12 w-12">
                <Check size={23} />
              </span>
              <h2 className="mt-6 text-2xl">Conferma preparazione</h2>
              <p className="mt-3 text-sm leading-6 text-muted">
                Il tavolo passerà allo stato attivo e i giocatori potranno
                chiedere di entrare.
              </p>
              <Button
                className="mt-7 w-full"
                variant="primary"
                disabled={isSubmitting || invalidTrapCount > 0}
                icon={
                  isSubmitting ? (
                    <LoaderCircle className="animate-spin" size={18} />
                  ) : (
                    <Dices size={18} />
                  )
                }
                onClick={() => void onSubmit(buildGrid())}
              >
                {isSubmitting ? "Creazione..." : "Crea sessione"}
              </Button>
            </aside>
          </div>
        ) : null}

        <footer className="flex items-center justify-between gap-3 border-t-2 border-ink bg-muted/10 p-4 sm:px-6">
          <Button
            variant="secondary"
            disabled={step === 0 || isSubmitting}
            icon={<ChevronLeft size={17} />}
            onClick={() => setStep((current) => Math.max(0, current - 1))}
          >
            Indietro
          </Button>
          {step < 2 ? (
            <Button
              variant="primary"
              disabled={!canContinue}
              icon={<ChevronRight size={17} />}
              onClick={() => setStep((current) => Math.min(2, current + 1))}
            >
              Continua
            </Button>
          ) : (
            <StatusChip tone="success" dot>
              Configurazione completa
            </StatusChip>
          )}
        </footer>
      </section>
    </main>
  );
}

function TrapEditor({
  trap,
  onChange,
  onRemove,
}: {
  trap: SessionTrapConfiguration;
  onChange: (patch: Partial<SessionTrapConfiguration>) => void;
  onRemove: () => void;
}) {
  return (
    <div>
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="bh-kicker bg-primary/40">Trappola · {trap.cell}</p>
          <h2 className="mt-4 text-2xl">Dettagli riservati</h2>
        </div>
        <button
          aria-label={`Rimuovi trappola dalla cella ${trap.cell}`}
          className="grid h-9 w-9 place-items-center border-2 border-ink bg-surface shadow-[2px_2px_0_var(--color-ink)] hover:bg-primary/40"
          onClick={onRemove}
          type="button"
        >
          <CircleOff size={17} />
        </button>
      </div>

      <div className="mt-6 grid gap-4">
        <SelectField
          label="Visibilità"
          value={trap.visibility}
          onChange={(value) =>
            onChange({
              visibility: value as SessionTrapConfiguration["visibility"],
            })
          }
          options={[
            ["HIDDEN", "Nascosta"],
            ["REVEALED", "Rivelata"],
            ["KEEP_DETAILS_HIDDEN", "Sempre segreta"],
          ]}
        />
        <div className="grid grid-cols-2 gap-3">
          <SelectField
            label="Salvezza"
            value={trap.saveAbility}
            onChange={(value) =>
              onChange({
                saveAbility: value as SessionTrapConfiguration["saveAbility"],
              })
            }
            options={[
              ["DEXTERITY", "Destrezza"],
              ["CONSTITUTION", "Costituzione"],
              ["STRENGTH", "Forza"],
              ["WISDOM", "Saggezza"],
              ["INTELLIGENCE", "Intelligenza"],
              ["CHARISMA", "Carisma"],
            ]}
          />
          <label className="text-sm font-bold text-muted">
            CD
            <input
              className="bh-input mt-2 h-10 w-full px-3"
              max={30}
              min={1}
              type="number"
              value={trap.saveDc}
              onChange={(event) =>
                onChange({ saveDc: Number(event.target.value) })
              }
            />
          </label>
        </div>
        <label className="text-sm font-bold text-muted">
          Danno
          <input
            className="bh-input mt-2 h-10 w-full px-3 font-mono"
            maxLength={20}
            value={trap.damageExpression}
            onChange={(event) =>
              onChange({ damageExpression: event.target.value })
            }
          />
          <span className="mt-1 block text-xs font-normal text-muted">
            Formato supportato: NdS+K, ad esempio 2d6+1.
          </span>
        </label>
        <SelectField
          label="Durata"
          value={trap.lifecyclePolicy}
          onChange={(value) =>
            onChange({
              lifecyclePolicy:
                value as SessionTrapConfiguration["lifecyclePolicy"],
            })
          }
          options={[
            ["ONE_SHOT", "Un solo utilizzo"],
            ["PERSISTENT", "Persistente"],
          ]}
        />
        <label className="flex items-center gap-3 border-2 border-ink bg-surface p-3 text-sm font-bold shadow-[2px_2px_0_var(--color-ink)]">
          <input
            checked={trap.armed}
            className="h-5 w-5 accent-primary"
            type="checkbox"
            onChange={(event) => onChange({ armed: event.target.checked })}
          />
          Trappola armata all&apos;avvio
        </label>
      </div>
    </div>
  );
}

function SelectField({
  label,
  value,
  options,
  onChange,
}: {
  label: string;
  value: string;
  options: Array<[string, string]>;
  onChange: (value: string) => void;
}) {
  return (
    <label className="text-sm font-bold text-muted">
      {label}
      <select
        className="bh-input mt-2 h-10 w-full px-2"
        value={value}
        onChange={(event) => onChange(event.target.value)}
      >
        {options.map(([optionValue, optionLabel]) => (
          <option key={optionValue} value={optionValue}>
            {optionLabel}
          </option>
        ))}
      </select>
    </label>
  );
}

function SetupSummary({ label, value }: { label: string; value: string }) {
  return (
    <div className="border-2 border-ink bg-surface p-4 shadow-[3px_3px_0_var(--color-ink)]">
      <p className="text-xs font-extrabold uppercase tracking-wider text-muted">
        {label}
      </p>
      <p className="mt-2 wrap-break-words text-xl font-extrabold">{value}</p>
    </div>
  );
}
