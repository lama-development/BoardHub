import * as React from "react";
import {
  AlertCircle,
  BookOpen,
  Check,
  ChevronDown,
  Footprints,
  HeartPulse,
  MapPin,
  Minus,
  LoaderCircle,
  LogOut,
  Plus,
  RefreshCw,
  Shield,
  UserRound,
  X,
} from "lucide-react";
import {
  createPlayerCharacter,
  createPlayerPiece,
  fetchPieceReachability,
  fetchPlayerCharacters,
  fetchPlayerIdentity,
  fetchPlayerPieces,
  movePlayerPiece,
} from "../api/boardhubApi";
import type {
  CharacterPartyVisibility,
  CreatePlayerCharacterInput,
  Participant,
  PieceReachability,
  PlayerCharacter,
  SessionPiece,
} from "../types";
import {
  calculateArmorClass,
  calculateHitPointMaximum,
  DEFENSE_PROFILES,
  DND_CLASSES,
  DND_SPECIES,
  findClassRule,
  findSpeciesRule,
} from "../domain/dndCharacterRules";
import type { DefenseProfile } from "../domain/dndCharacterRules";
import { createUuid } from "../utils/uuid";
import { BoardGrid } from "./BoardGrid";
import {
  AlertBanner,
  Button,
  PageHeaderIdentity,
  StatusChip,
  SummaryCard,
} from "./ui";

type PlayerSessionPanelProps = {
  sessionId: string;
  sessionTitle: string;
  tableDisplayName: string;
  playerToken: string;
  onLeave: () => void;
};

type CharacterFormState = {
  name: string;
  species: string;
  age: string;
  className: string;
  level: string;
  constitution: string;
  dexterity: string;
  wisdom: string;
  defenseProfile: DefenseProfile;
  hasShield: boolean;
  speedFeet: string;
  hpMax: string;
  armorClass: string;
  partyVisibility: CharacterPartyVisibility;
};

const INITIAL_FORM: CharacterFormState = {
  name: "",
  species: "",
  age: "",
  className: "",
  level: "1",
  constitution: "10",
  dexterity: "10",
  wisdom: "10",
  defenseProfile: "UNARMORED",
  hasShield: false,
  speedFeet: "30",
  hpMax: "10",
  armorClass: "10",
  partyVisibility: "OWNER_ONLY",
};

const VISIBILITY_LABELS: Record<CharacterPartyVisibility, string> = {
  OWNER_ONLY: "Solo io",
  PARTY: "Tutto il gruppo",
  DM_ONLY: "Io e il DM",
};

function SpeciesAvatar({
  species,
  size = "medium",
}: {
  species: string;
  size?: "medium" | "large";
}) {
  const rule = findSpeciesRule(species);
  const dimensions = size === "large" ? "h-14 w-14" : "h-10 w-10";

  return (
    <span
      className={`grid ${dimensions} shrink-0 place-items-center overflow-hidden border-2 border-[#111111] bg-[#c8b1ff] shadow-[2px_2px_0_#111111]`}
    >
      {rule ? (
        <img
          alt={`Specie ${rule.label}`}
          className="h-full w-full object-cover"
          src={rule.iconPath}
        />
      ) : (
        <UserRound size={size === "large" ? 24 : 18} aria-hidden="true" />
      )}
    </span>
  );
}

function toCharacterInput(
  form: CharacterFormState,
): CreatePlayerCharacterInput {
  return {
    name: form.name.trim(),
    species: form.species.trim(),
    age: form.age ? Number(form.age) : null,
    className: form.className.trim(),
    level: Number(form.level),
    speedCells: Number(form.speedFeet) / 5,
    hpMax: Number(form.hpMax),
    armorClass: Number(form.armorClass),
    partyVisibility: form.partyVisibility,
  };
}

export function PlayerSessionPanel({
  sessionId,
  sessionTitle,
  playerToken,
  onLeave,
}: PlayerSessionPanelProps) {
  const [identity, setIdentity] = React.useState<Participant | null>(null);
  const [characters, setCharacters] = React.useState<PlayerCharacter[]>([]);
  const [pieces, setPieces] = React.useState<SessionPiece[]>([]);
  const [form, setForm] = React.useState<CharacterFormState>(INITIAL_FORM);
  const [showForm, setShowForm] = React.useState(false);
  const [isLoading, setIsLoading] = React.useState(true);
  const [isCreating, setIsCreating] = React.useState(false);
  const [pieceCharacterId, setPieceCharacterId] = React.useState("");
  const [startCell, setStartCell] = React.useState("B2");
  const [isCreatingPiece, setIsCreatingPiece] = React.useState(false);
  const [busyPieceId, setBusyPieceId] = React.useState<string | null>(null);
  const [reachability, setReachability] =
    React.useState<PieceReachability | null>(null);
  const [lastMove, setLastMove] = React.useState<string | null>(null);
  const [error, setError] = React.useState<string | null>(null);

  const refresh = React.useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [activeIdentity, ownedCharacters, ownedPieces] = await Promise.all([
        fetchPlayerIdentity(sessionId, playerToken),
        fetchPlayerCharacters(sessionId, playerToken),
        fetchPlayerPieces(sessionId, playerToken),
      ]);
      setIdentity(activeIdentity);
      setCharacters(ownedCharacters);
      setPieces(ownedPieces);
    } catch (refreshError) {
      setError(
        refreshError instanceof Error
          ? refreshError.message
          : "Impossibile caricare il profilo del giocatore.",
      );
    } finally {
      setIsLoading(false);
    }
  }, [playerToken, sessionId]);

  React.useEffect(() => {
    void refresh();
  }, [refresh]);

  function updateField<Key extends keyof CharacterFormState>(
    field: Key,
    value: CharacterFormState[Key],
  ) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  function updateRulesField<Key extends keyof CharacterFormState>(
    field: Key,
    value: CharacterFormState[Key],
  ) {
    setForm((current) => {
      const next = { ...current, [field]: value };
      const selectedClass =
        field === "className"
          ? findClassRule(String(value))
          : findClassRule(next.className);
      const selectedSpecies =
        field === "species"
          ? findSpeciesRule(String(value))
          : findSpeciesRule(next.species);

      if (field === "className" && selectedClass) {
        next.defenseProfile = selectedClass.defaultDefense;
        next.hasShield = selectedClass.defaultShield;
      }
      if (field === "species" && selectedSpecies) {
        next.speedFeet = String(selectedSpecies.speedFeet);
      }

      next.hpMax = String(
        calculateHitPointMaximum(
          next.className,
          Number(next.level),
          Number(next.constitution),
        ),
      );
      next.armorClass = String(
        calculateArmorClass(
          next.defenseProfile,
          Number(next.dexterity),
          Number(next.constitution),
          Number(next.wisdom),
          next.hasShield,
        ),
      );
      return next;
    });
  }

  async function submitCharacter(event: React.FormEvent) {
    event.preventDefault();
    if (!findClassRule(form.className)) {
      setError("Seleziona una delle classi D&D disponibili.");
      return;
    }
    setIsCreating(true);
    setError(null);
    try {
      const created = await createPlayerCharacter(
        sessionId,
        playerToken,
        toCharacterInput(form),
      );
      setCharacters((current) => [...current, created]);
      setPieceCharacterId((current) => current || created.characterId);
      setForm(INITIAL_FORM);
      setShowForm(false);
    } catch (createError) {
      setError(
        createError instanceof Error
          ? createError.message
          : "Creazione del personaggio non riuscita.",
      );
    } finally {
      setIsCreating(false);
    }
  }

  async function submitPiece(event: React.FormEvent) {
    event.preventDefault();
    if (!pieceCharacterId) return;
    setIsCreatingPiece(true);
    setError(null);
    setLastMove(null);
    try {
      const created = await createPlayerPiece(
        sessionId,
        playerToken,
        pieceCharacterId,
        startCell,
      );
      setPieces((current) => [...current, created]);
      setPieceCharacterId("");
      setStartCell("B2");
    } catch (createError) {
      setError(
        createError instanceof Error
          ? createError.message
          : "Creazione della pedina non riuscita.",
      );
    } finally {
      setIsCreatingPiece(false);
    }
  }

  async function showReachableCells(piece: SessionPiece) {
    setBusyPieceId(piece.sessionPieceId);
    setError(null);
    setLastMove(null);
    try {
      setReachability(
        await fetchPieceReachability(
          sessionId,
          piece.sessionPieceId,
          playerToken,
        ),
      );
    } catch (reachabilityError) {
      setError(
        reachabilityError instanceof Error
          ? reachabilityError.message
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
    setLastMove(null);
    try {
      const result = await movePlayerPiece(
        sessionId,
        reachability.sessionPieceId,
        playerToken,
        destination,
        reachability.version,
        createUuid(),
      );
      setPieces((current) =>
        current.map((piece) =>
          piece.sessionPieceId === result.sessionPieceId
            ? { ...piece, currentCell: result.to, version: result.version }
            : piece,
        ),
      );
      setLastMove(`${result.from} → ${result.to} · costo ${result.cost}`);
      setReachability(null);
    } catch (moveError) {
      setError(
        moveError instanceof Error
          ? moveError.message
          : "Movimento della pedina non riuscito.",
      );
      await refresh();
      setReachability(null);
    } finally {
      setBusyPieceId(null);
    }
  }

  const assignedCharacterIds = new Set(
    pieces.map((piece) => piece.characterId),
  );
  const availableCharacters = characters.filter(
    (character) => !assignedCharacterIds.has(character.characterId),
  );

  function characterName(characterId: string) {
    return (
      characters.find((character) => character.characterId === characterId)
        ?.name ?? "Personaggio"
    );
  }

  const boardTokens = pieces.map((piece) => ({
    id: characterName(piece.characterId),
    cell: piece.currentCell,
    kind: "character" as const,
  }));
  const reachableCells = reachability?.reachableCells.map((cell) => cell.cell);

  const previewSpeedCells = Number(form.speedFeet || 0) / 5;

  return (
    <main className="min-h-screen bg-transparent px-4 py-5 text-[#111111] sm:px-6 sm:py-8">
      <header className="bh-surface mx-auto flex w-full max-w-7xl flex-col gap-4 p-4 sm:flex-row sm:items-center sm:justify-between sm:p-5">
        <PageHeaderIdentity
          accentClassName="bg-[#04c8e8]"
          eyebrow="BoardHub · Area giocatore"
          icon={<UserRound size={20} />}
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
            Esci
          </Button>
        </div>
      </header>

      <section className="mx-auto mt-4 grid w-full max-w-7xl gap-3 sm:grid-cols-3">
        <SummaryCard
          accent="cyan"
          icon={<UserRound size={18} />}
          label="Giocatore"
        >
          <strong className="truncate text-xl font-extrabold leading-tight">
            {identity?.displayName ?? "Verifica in corso"}
          </strong>
        </SummaryCard>
        <SummaryCard accent="lime" icon={<Check size={18} />} label="Stato">
          <StatusChip
            tone={identity?.status === "ACTIVE" ? "success" : "neutral"}
            dot
          >
            {identity?.status === "ACTIVE"
              ? "Accesso attivo"
              : "Verifica in corso"}
          </StatusChip>
        </SummaryCard>
        <SummaryCard
          accent="yellow"
          icon={<BookOpen size={18} />}
          label="Personaggi"
        >
          <strong className="text-xl font-extrabold leading-tight">
            {characters.length}
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

      <section className="bh-surface mx-auto mt-4 w-full max-w-7xl p-4 sm:p-5">
        <div className="-mx-4 -mt-4 flex flex-col gap-3 border-b-2 border-[#111111] bg-[#ff8bc7] px-4 py-3 sm:-mx-5 sm:-mt-5 sm:flex-row sm:items-center sm:justify-between sm:px-5">
          <div className="flex items-center gap-3">
            <span className="bh-card-icon">
              <BookOpen size={18} />
            </span>
            <div>
              <h2 className="text-xl">I miei personaggi</h2>
              <p className="mt-1 text-sm font-medium text-slate-800">
                Crea la scheda tattica che userai per la tua pedina.
              </p>
            </div>
          </div>
          <Button
            variant={showForm ? "secondary" : "primary"}
            icon={
              showForm ? (
                <X size={18} aria-hidden="true" />
              ) : (
                <Plus size={18} aria-hidden="true" />
              )
            }
            onClick={() => setShowForm((current) => !current)}
          >
            {showForm ? "Chiudi modulo" : "Nuovo personaggio"}
          </Button>
        </div>

        {showForm ? (
          <form className="mt-5 grid gap-4" onSubmit={submitCharacter}>
            <fieldset className="bh-form-group">
              <legend className="bh-form-legend bg-[#ff8bc7]">
                01 · Identità del personaggio
              </legend>
              <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                <CharacterTextField
                  label="Nome"
                  value={form.name}
                  placeholder="Es. Elaria"
                  description="Il nome con cui il personaggio sarà mostrato al tavolo."
                  onChange={(value) => updateField("name", value)}
                />
                <CharacterNumberField
                  label="Età"
                  value={form.age}
                  min={1}
                  description="Dato narrativo facoltativo; non modifica le regole di movimento."
                  onChange={(value) => updateField("age", value)}
                  required={false}
                />
                <div className="sm:col-span-2 lg:col-span-3">
                  <span className="text-sm font-bold text-[#111111]">
                    Specie
                  </span>
                  <div
                    className="mt-1.5 grid grid-cols-2 gap-2 sm:grid-cols-3 lg:grid-cols-5"
                    role="group"
                    aria-label="Specie del personaggio"
                  >
                    {DND_SPECIES.map((entry) => {
                      const selected = form.species === entry.label;
                      return (
                        <button
                          className={`flex min-h-20 cursor-pointer items-center gap-2.5 border-2 border-[#111111] p-2 text-left shadow-[2px_2px_0_#111111] transition-transform active:translate-x-0.5 active:translate-y-0.5 active:shadow-none ${selected ? "bg-[#04c8e8]" : "bg-white hover:bg-[#d8f7fb]"}`}
                          key={entry.label}
                          type="button"
                          aria-pressed={selected}
                          onClick={() =>
                            updateRulesField("species", entry.label)
                          }
                        >
                          <span className="grid h-13 w-13 shrink-0 place-items-center overflow-hidden border-2 border-[#111111] bg-[#c8b1ff]">
                            <img
                              alt=""
                              className="h-full w-full object-cover"
                              src={entry.iconPath}
                            />
                          </span>
                          <span className="min-w-0">
                            <strong className="block truncate text-sm leading-tight">
                              {entry.label}
                            </strong>
                            <span className="mt-1 block text-[11px] font-bold text-slate-600">
                              {entry.speedFeet} piedi
                            </span>
                          </span>
                        </button>
                      );
                    })}
                  </div>
                  <span className="mt-1.5 block text-xs font-normal leading-4 text-slate-500">
                    La specie imposta la velocità iniziale e identifica
                    visivamente il personaggio.
                  </span>
                </div>
                <div className="sm:col-span-2 lg:col-span-3">
                  <span className="text-sm font-bold text-[#111111]">
                    Classe
                  </span>
                  <div
                    className="mt-1.5 grid grid-cols-2 gap-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-6"
                    role="group"
                    aria-label="Classe del personaggio"
                  >
                    {DND_CLASSES.map((entry) => {
                      const selected = form.className === entry.label;
                      return (
                        <button
                          className={`min-h-14 cursor-pointer border-2 border-[#111111] px-3 py-2 text-left shadow-[2px_2px_0_#111111] transition-transform active:translate-x-0.5 active:translate-y-0.5 active:shadow-none ${selected ? "bg-[#ffd400]" : "bg-white hover:bg-[#fff2a7]"}`}
                          key={entry.label}
                          type="button"
                          aria-pressed={selected}
                          onClick={() =>
                            updateRulesField("className", entry.label)
                          }
                        >
                          <strong className="block truncate text-sm leading-tight">
                            {entry.label}
                          </strong>
                          <span className="mt-1 block text-[11px] font-bold text-slate-600">
                            D{entry.hitDie} PF
                          </span>
                        </button>
                      );
                    })}
                  </div>
                  <span className="mt-1.5 block text-xs font-normal leading-4 text-slate-500">
                    La classe imposta dado vita e profilo difensivo iniziale;
                    puoi correggere l'equipaggiamento sotto.
                  </span>
                </div>
              </div>
            </fieldset>

            <fieldset className="bh-form-group bg-[#fffdf2]">
              <legend className="bh-form-legend bg-[#ffd400]">
                02 · Valori tattici
              </legend>
              <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                <NumberStepper
                  label="Livello"
                  value={form.level}
                  min={1}
                  max={20}
                  description="Livello attuale del personaggio, da 1 a 20."
                  onChange={(value) => updateRulesField("level", value)}
                />
                <NumberStepper
                  label="Costituzione"
                  value={form.constitution}
                  min={1}
                  max={30}
                  description="Determina i PF insieme a classe e livello."
                  onChange={(value) => updateRulesField("constitution", value)}
                />
                <NumberStepper
                  label="Destrezza"
                  value={form.dexterity}
                  min={1}
                  max={30}
                  description="Contribuisce alla CA quando il profilo lo consente."
                  onChange={(value) => updateRulesField("dexterity", value)}
                />
                <NumberStepper
                  label="Saggezza"
                  value={form.wisdom}
                  min={1}
                  max={30}
                  description="Usata dalla Difesa senza armatura del Monaco."
                  onChange={(value) => updateRulesField("wisdom", value)}
                />
              </div>
              <div className="mt-4 grid gap-3 border-t-2 border-[#111111] pt-4 sm:grid-cols-2 lg:grid-cols-4">
                <CustomSelect
                  label="Profilo difensivo"
                  value={form.defenseProfile}
                  options={DEFENSE_PROFILES}
                  description="Formula proposta dalla classe; cambiala se usi un'altra armatura."
                  onChange={(value) =>
                    updateRulesField("defenseProfile", value)
                  }
                />
                <label className="flex flex-col gap-1.5 text-sm font-bold text-[#111111]">
                  Scudo
                  <button
                    className={`bh-input flex h-10 cursor-pointer items-center justify-between px-3 text-left ${form.hasShield ? "bg-[#b8ee72]" : "bg-white"}`}
                    type="button"
                    role="switch"
                    aria-checked={form.hasShield}
                    onClick={() =>
                      updateRulesField("hasShield", !form.hasShield)
                    }
                  >
                    <span>
                      {form.hasShield
                        ? "Equipaggiato (+2 CA)"
                        : "Non equipaggiato"}
                    </span>
                    <Shield size={17} aria-hidden="true" />
                  </button>
                  <span className="text-xs font-normal leading-4 text-slate-500">
                    Disponibile solo se la scheda prevede competenza.
                  </span>
                </label>
                <NumberStepper
                  label="Punti ferita massimi"
                  value={form.hpMax}
                  min={1}
                  max={1000000}
                  description="Calcolati dalla classe e dalla Costituzione; modificabili."
                  onChange={(value) => updateField("hpMax", value)}
                />
                <NumberStepper
                  label="Classe Armatura (CA)"
                  value={form.armorClass}
                  min={0}
                  max={100}
                  description="Calcolata dal profilo difensivo; modificabile."
                  onChange={(value) => updateField("armorClass", value)}
                />
                <NumberStepper
                  label="Velocità (piedi)"
                  value={form.speedFeet}
                  min={0}
                  max={500}
                  step={5}
                  description={`Proposta dalla specie: ${previewSpeedCells} celle sulla plancia.`}
                  onChange={(value) => updateField("speedFeet", value)}
                />
              </div>
            </fieldset>

            <details className="bh-form-group group">
              <summary className="flex cursor-pointer list-none items-center justify-between gap-3 text-sm font-extrabold uppercase tracking-wider text-[#111111] marker:content-none">
                <span>03 · Dettagli facoltativi e privacy</span>
                <span className="grid h-7 w-7 place-items-center border-2 border-[#111111] bg-[#c8b1ff] leading-none group-open:bg-[#ff8bc7]">
                  <Plus
                    className="transition-transform group-open:rotate-45"
                    size={16}
                    aria-hidden="true"
                  />
                </span>
              </summary>
              <div className="mt-3 grid gap-3 sm:grid-cols-2">
                <CustomSelect
                  label="Visibilità nel gruppo"
                  value={form.partyVisibility}
                  options={Object.entries(VISIBILITY_LABELS).map(
                    ([value, label]) => ({
                      value: value as CharacterPartyVisibility,
                      label,
                    }),
                  )}
                  description="Il DM può sempre vedere la scheda completa."
                  onChange={(value) => updateField("partyVisibility", value)}
                />
              </div>
            </details>

            <div className="bh-form-preview">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div>
                  <span className="bh-kicker">Anteprima scheda</span>
                  <div className="mt-3 flex items-center gap-3">
                    <SpeciesAvatar species={form.species} size="large" />
                    <h3 className="text-2xl">
                      {form.name || "Nome del personaggio"}
                    </h3>
                  </div>
                  <p className="mt-1 text-sm font-bold text-slate-700">
                    {form.species || "Specie"} · {form.className || "Classe"} ·
                    Livello {form.level}
                  </p>
                </div>
                <StatusChip tone="info">
                  {VISIBILITY_LABELS[form.partyVisibility]}
                </StatusChip>
              </div>
              <div className="mt-4 grid grid-cols-1 gap-2 sm:grid-cols-3">
                <PreviewStat
                  icon={<HeartPulse size={16} />}
                  label="Punti ferita"
                  value={`${form.hpMax || "-"}/${form.hpMax || "-"}`}
                />
                <PreviewStat
                  icon={<Shield size={16} />}
                  label="Classe armatura"
                  value={form.armorClass || "-"}
                />
                <PreviewStat
                  icon={<Footprints size={16} />}
                  label="Movimento"
                  value={`${previewSpeedCells} celle`}
                />
              </div>
            </div>

            <div className="flex flex-col gap-2 border-t-2 border-[#111111] pt-4 sm:flex-row">
              <Button
                variant="primary"
                type="submit"
                disabled={isCreating}
                icon={
                  isCreating ? (
                    <LoaderCircle className="animate-spin" size={18} />
                  ) : (
                    <Plus size={18} />
                  )
                }
              >
                Crea personaggio
              </Button>
              <Button variant="secondary" onClick={() => setShowForm(false)}>
                Annulla
              </Button>
            </div>
          </form>
        ) : null}

        {isLoading && characters.length === 0 ? (
          <div className="mt-6 flex min-h-36 items-center justify-center gap-2 text-sm text-slate-500">
            <LoaderCircle className="animate-spin" size={19} />
            Caricamento personaggi
          </div>
        ) : characters.length === 0 ? (
          <div className="bh-empty-state mt-6 px-4 py-8 text-center">
            <span className="bh-empty-icon bg-[#ff8bc7]">
              <UserRound size={22} />
            </span>
            <p className="mt-3 font-medium text-slate-700">
              Nessun personaggio creato
            </p>
            <p className="mt-1 text-sm text-slate-500">
              Crea il primo personaggio per poter generare una pedina.
            </p>
          </div>
        ) : (
          <ul className="mt-6 grid gap-3 md:grid-cols-2 xl:grid-cols-3">
            {characters.map((character) => (
              <li
                className="bh-surface bh-surface--cream p-4"
                key={character.characterId}
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="flex min-w-0 items-start gap-3">
                    <SpeciesAvatar species={character.species} />
                    <div className="min-w-0">
                      <p className="font-semibold text-slate-950">
                        {character.name}
                      </p>
                      <p className="mt-1 text-sm text-slate-500">
                        {character.species} · {character.className} · Livello{" "}
                        {character.level}
                      </p>
                    </div>
                  </div>
                  <StatusChip tone="neutral">
                    {VISIBILITY_LABELS[character.partyVisibility]}
                  </StatusChip>
                </div>
                <div className="mt-4 grid grid-cols-3 gap-2 text-sm">
                  <CharacterStat
                    icon={<HeartPulse size={16} />}
                    label="PF"
                    value={`${character.hpCurrent}/${character.hpMax}`}
                  />
                  <CharacterStat
                    icon={<Shield size={16} />}
                    label="CA"
                    value={String(character.armorClass)}
                  />
                  <CharacterStat
                    icon={<Footprints size={16} />}
                    label="Movimento"
                    value={`${character.speedCells} celle`}
                  />
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="bh-surface mx-auto mb-8 mt-4 w-full max-w-7xl p-4 sm:p-5">
        <div className="-mx-4 -mt-4 flex items-center gap-3 border-b-2 border-[#111111] bg-[#ffd400] px-4 py-3 sm:-mx-5 sm:-mt-5 sm:px-5">
          <span className="bh-card-icon">
            <MapPin size={18} />
          </span>
          <div>
            <h2 className="text-xl">Le mie pedine</h2>
            <p className="mt-1 text-sm font-medium text-slate-800">
              Associa un personaggio alla griglia, calcola le destinazioni e
              conferma il movimento.
            </p>
          </div>
        </div>

        {availableCharacters.length > 0 ? (
          <form
            className="bh-surface-flat mt-5 grid gap-3 bg-[#fbfaf6] p-4 sm:grid-cols-[minmax(220px,1fr)_auto] sm:items-end"
            onSubmit={submitPiece}
          >
            <CustomSelect
              label="Personaggio senza pedina"
              value={pieceCharacterId}
              options={[
                { value: "", label: "Seleziona un personaggio" },
                ...availableCharacters.map((character) => ({
                  value: character.characterId,
                  label: `${character.name} · ${character.className}`,
                })),
              ]}
              onChange={setPieceCharacterId}
            />
            <Button
              variant="primary"
              type="submit"
              disabled={isCreatingPiece || !pieceCharacterId}
              icon={
                isCreatingPiece ? (
                  <LoaderCircle className="animate-spin" size={18} />
                ) : (
                  <Plus size={18} />
                )
              }
            >
              Genera pedina
            </Button>
            <div className="sm:col-span-2">
              <p className="mb-2 text-sm font-medium text-slate-700">
                Cella iniziale:{" "}
                <span className="text-slate-950">{startCell}</span>
              </p>
              <BoardGrid
                tokens={boardTokens}
                selectedCell={startCell}
                onSelectCell={setStartCell}
                disabledCells={pieces.map((piece) => piece.currentCell)}
                selectionHint="Clicca una cella libera sulla plancia per scegliere la posizione iniziale."
              />
            </div>
          </form>
        ) : characters.length > 0 ? (
          <p className="bh-empty-state mt-5 px-4 py-4 text-sm text-slate-700">
            Ogni personaggio disponibile ha già una pedina.
          </p>
        ) : null}

        {lastMove ? (
          <AlertBanner
            className="mt-4"
            tone="success"
            icon={<Check size={18} />}
          >
            Movimento confermato: {lastMove}
          </AlertBanner>
        ) : null}

        {reachability ? (
          <div className="bh-surface-flat mt-5 bg-[#fbfaf6] p-4">
            <div className="flex items-center justify-between gap-3">
              <div>
                <p className="font-semibold text-slate-950">
                  Scegli la destinazione sulla plancia
                </p>
                <p className="mt-1 text-sm text-slate-500">
                  {reachability.movementPoints} punti movimento disponibili. Le
                  celle evidenziate sono raggiungibili.
                </p>
              </div>
              <button
                className="cursor-pointer rounded-[1px] border-2 border-transparent px-2 py-1 text-xs font-bold uppercase tracking-wide text-slate-600 hover:border-[#111111] hover:bg-[#fff2a7] hover:text-[#111111]"
                type="button"
                onClick={() => setReachability(null)}
              >
                Chiudi
              </button>
            </div>
            <BoardGrid
              tokens={boardTokens}
              selectableCells={reachableCells}
              selectionHint="Clicca una cella evidenziata per confermare il movimento."
              onSelectCell={(cell) => void moveTo(cell)}
            />
          </div>
        ) : null}

        {pieces.length === 0 ? (
          <div className="bh-empty-state mt-6 px-4 py-8 text-center">
            <span className="bh-empty-icon bg-[#ffd400]">
              <MapPin size={22} />
            </span>
            <p className="mt-3 font-medium text-slate-700">
              Nessuna pedina sulla griglia
            </p>
            <p className="mt-1 text-sm text-slate-500">
              Prima crea un personaggio, poi scegli una cella libera.
            </p>
          </div>
        ) : (
          <ul className="mt-6 grid gap-3 md:grid-cols-2">
            {pieces.map((piece) => (
              <li
                className="bh-surface bh-surface--cyan p-4"
                key={piece.sessionPieceId}
              >
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="font-semibold text-slate-950">
                      {characterName(piece.characterId)}
                    </p>
                    <p className="mt-1 text-sm text-slate-500">
                      Pedina {piece.representationMode.toLowerCase()}
                    </p>
                  </div>
                  <StatusChip tone="info">Cella {piece.currentCell}</StatusChip>
                </div>
                <div className="mt-4 flex items-center justify-between border-t-2 border-[#111111] pt-4">
                  <span className="text-xs text-slate-500">
                    Versione {piece.version}
                  </span>
                  <Button
                    size="compact"
                    variant="secondary"
                    icon={
                      busyPieceId === piece.sessionPieceId ? (
                        <LoaderCircle className="animate-spin" size={17} />
                      ) : (
                        <MapPin size={17} />
                      )
                    }
                    disabled={busyPieceId === piece.sessionPieceId}
                    onClick={() => void showReachableCells(piece)}
                  >
                    Muovi
                  </Button>
                </div>

                {reachability?.sessionPieceId === piece.sessionPieceId ? (
                  <div className="hidden mt-4 border-t border-slate-100 pt-4">
                    <div className="flex items-center justify-between gap-2 text-sm">
                      <span className="font-medium text-slate-700">
                        Celle raggiungibili
                      </span>
                      <button
                        className="cursor-pointer rounded-[1px] border-2 border-transparent px-2 py-1 text-xs font-bold uppercase tracking-wide text-slate-600 hover:border-[#111111] hover:bg-[#fff2a7] hover:text-[#111111]"
                        type="button"
                        onClick={() => setReachability(null)}
                      >
                        Chiudi
                      </button>
                    </div>
                    <p className="mt-1 text-xs text-slate-500">
                      {reachability.movementPoints} punti movimento disponibili
                    </p>
                    <div className="mt-3 flex flex-wrap gap-2">
                      {reachability.reachableCells.map((cell) => (
                        <button
                          className="cursor-pointer rounded-xs border-2 border-[#111111] bg-[#d8f7fb] px-3 py-2 text-left text-sm font-bold text-[#111111] shadow-[2px_2px_0_#111111] transition hover:bg-[#8fe8f4] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#9165ff] disabled:cursor-not-allowed disabled:opacity-50"
                          key={cell.cell}
                          type="button"
                          title={`Percorso: ${cell.path.join(" → ")}`}
                          disabled={busyPieceId === piece.sessionPieceId}
                          onClick={() => void moveTo(cell.cell)}
                        >
                          <span className="font-semibold">{cell.cell}</span>
                          <span className="ml-2 text-xs">
                            costo {cell.cost}
                          </span>
                        </button>
                      ))}
                    </div>
                  </div>
                ) : null}
              </li>
            ))}
          </ul>
        )}
      </section>
    </main>
  );
}

type CharacterTextFieldProps = {
  label: string;
  value: string;
  description?: string;
  placeholder?: string;
  onChange: (value: string) => void;
};

function CharacterTextField({
  label,
  value,
  description,
  placeholder,
  onChange,
}: CharacterTextFieldProps) {
  return (
    <label className="flex flex-col gap-1.5 text-sm font-bold text-[#111111]">
      {label}
      <input
        className="bh-input h-10 px-3 font-normal"
        value={value}
        placeholder={placeholder}
        onChange={(event) => onChange(event.target.value)}
        required
        maxLength={80}
      />
      {description ? (
        <span className="text-xs font-normal leading-4 text-slate-500">
          {description}
        </span>
      ) : null}
    </label>
  );
}

type NumberStepperProps = {
  label: string;
  value: string;
  min: number;
  max: number;
  step?: number;
  description?: string;
  onChange: (value: string) => void;
};

function NumberStepper({
  label,
  value,
  min,
  max,
  step = 1,
  description,
  onChange,
}: NumberStepperProps) {
  const numericValue = Number(value || min);

  function changeBy(delta: number) {
    onChange(String(Math.min(max, Math.max(min, numericValue + delta))));
  }

  return (
    <label className="flex flex-col gap-1.5 text-sm font-bold text-[#111111]">
      {label}
      <span className="bh-input flex h-10 items-center justify-between px-1.5">
        <button
          className="grid h-7 w-7 shrink-0 cursor-pointer place-items-center rounded-[1px] border-2 border-[#111111] bg-white text-[#111111] hover:bg-[#ff8bc7] disabled:cursor-not-allowed disabled:opacity-40"
          type="button"
          aria-label={`Diminuisci ${label.toLocaleLowerCase("it-IT")}`}
          disabled={numericValue <= min}
          onClick={() => changeBy(-step)}
        >
          <Minus size={15} aria-hidden="true" />
        </button>
        <input
          className="h-full w-full min-w-0 bg-transparent px-1 text-center font-semibold text-slate-950 outline-none [appearance:textfield] [&::-webkit-inner-spin-button]:appearance-none [&::-webkit-outer-spin-button]:appearance-none"
          type="number"
          value={value}
          min={min}
          max={max}
          step={step}
          required
          onChange={(event) => onChange(event.target.value)}
        />
        <button
          className="grid h-7 w-7 shrink-0 cursor-pointer place-items-center rounded-[1px] border-2 border-[#111111] bg-[#ffd400] text-[#111111] hover:bg-[#b8ee72] disabled:cursor-not-allowed disabled:opacity-40"
          type="button"
          aria-label={`Aumenta ${label.toLocaleLowerCase("it-IT")}`}
          disabled={numericValue >= max}
          onClick={() => changeBy(step)}
        >
          <Plus size={15} aria-hidden="true" />
        </button>
      </span>
      {description ? (
        <span className="text-xs font-normal leading-4 text-slate-500">
          {description}
        </span>
      ) : null}
    </label>
  );
}

type CustomSelectProps<Value extends string> = {
  label: string;
  value: Value;
  options: Array<{ value: Value; label: string }>;
  description?: string;
  onChange: (value: Value) => void;
};

function CustomSelect<Value extends string>({
  label,
  value,
  options,
  description,
  onChange,
}: CustomSelectProps<Value>) {
  const [isOpen, setIsOpen] = React.useState(false);
  const containerRef = React.useRef<HTMLDivElement>(null);
  const selectedLabel =
    options.find((option) => option.value === value)?.label ?? value;

  React.useEffect(() => {
    function closeOnOutsideClick(event: PointerEvent) {
      if (!containerRef.current?.contains(event.target as Node))
        setIsOpen(false);
    }
    document.addEventListener("pointerdown", closeOnOutsideClick);
    return () =>
      document.removeEventListener("pointerdown", closeOnOutsideClick);
  }, []);

  return (
    <div
      className="relative flex flex-col gap-1.5 text-sm font-bold text-[#111111]"
      ref={containerRef}
    >
      <span>{label}</span>
      <button
        className={`bh-input flex h-10 w-full cursor-pointer items-center justify-between px-3 text-left font-normal ${isOpen ? "bg-[#fff8cc]" : ""}`}
        type="button"
        aria-haspopup="listbox"
        aria-expanded={isOpen}
        onClick={() => setIsOpen((current) => !current)}
      >
        {selectedLabel}
        <ChevronDown
          className={isOpen ? "rotate-180" : ""}
          size={17}
          aria-hidden="true"
        />
      </button>
      {description ? (
        <span className="text-xs font-normal leading-4 text-slate-500">
          {description}
        </span>
      ) : null}
      {isOpen ? (
        <ul
          className="absolute inset-x-0 top-16.5 z-20 rounded-xs border-2 border-[#111111] bg-white p-1.5 shadow-[4px_4px_0_#111111]"
          role="listbox"
        >
          {options.map((option) => (
            <li key={option.value}>
              <button
                className="flex w-full cursor-pointer items-center justify-between rounded-[1px] border-2 border-transparent px-3 py-2 text-left text-sm font-bold text-slate-700 hover:border-[#111111] hover:bg-[#fff2a7]"
                type="button"
                role="option"
                aria-selected={option.value === value}
                onClick={() => {
                  onChange(option.value);
                  setIsOpen(false);
                }}
              >
                {option.label}
                {option.value === value ? (
                  <Check size={16} aria-hidden="true" />
                ) : null}
              </button>
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  );
}

type CharacterNumberFieldProps = {
  label: string;
  value: string;
  min: number;
  max?: number;
  step?: number;
  description?: string;
  required?: boolean;
  onChange: (value: string) => void;
};

function CharacterNumberField({
  label,
  value,
  min,
  max,
  step,
  description,
  required = true,
  onChange,
}: CharacterNumberFieldProps) {
  return (
    <label className="flex flex-col gap-1.5 text-sm font-bold text-[#111111]">
      {label}
      <input
        className="bh-input h-10 px-3 font-normal"
        type="number"
        value={value}
        min={min}
        max={max}
        step={step}
        required={required}
        onChange={(event) => onChange(event.target.value)}
      />
      {description ? (
        <span className="text-xs font-normal leading-4 text-slate-500">
          {description}
        </span>
      ) : null}
    </label>
  );
}

type CharacterStatProps = {
  icon: React.ReactNode;
  label: string;
  value: string;
};

function CharacterStat({ icon, label, value }: CharacterStatProps) {
  return (
    <div className="border-2 border-[#111111] bg-white p-2.5">
      <span className="flex items-center gap-1 text-slate-500">
        {icon}
        {label}
      </span>
      <p className="mt-1 font-semibold text-slate-950">{value}</p>
    </div>
  );
}

function PreviewStat({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
}) {
  return (
    <div className="border-2 border-[#111111] bg-white p-2.5 shadow-[2px_2px_0_#111111]">
      <span className="flex items-center gap-1.5 text-[12px] font-bold uppercase leading-tight tracking-[0.03em] text-slate-700">
        {icon}
        {label}
      </span>
      <strong className="mt-1 block text-base font-black text-[#111111]">
        {value}
      </strong>
    </div>
  );
}
