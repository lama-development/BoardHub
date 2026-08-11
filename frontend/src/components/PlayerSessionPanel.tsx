import * as React from "react";
import {
  AlertCircle,
  BookOpen,
  Check,
  ChevronDown,
  Heart,
  MapPin,
  Minus,
  LoaderCircle,
  LogOut,
  Plus,
  RefreshCw,
  Shield,
  Swords,
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
import { createUuid } from "../utils/uuid";
import { AlertBanner, Button, PageHeaderIdentity, StatusChip, SummaryCard } from "./ui";

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

const SPECIES_SUGGESTIONS = [
  "Aasimar",
  "Dragonide",
  "Elfo",
  "Gnomo",
  "Goliath",
  "Halfling",
  "Nano",
  "Orco",
  "Tiefling",
  "Umano",
];

const CLASS_SUGGESTIONS = [
  "Barbaro",
  "Bardo",
  "Chierico",
  "Druido",
  "Guerriero",
  "Ladro",
  "Mago",
  "Monaco",
  "Paladino",
  "Ranger",
  "Stregone",
  "Warlock",
];

function toCharacterInput(form: CharacterFormState): CreatePlayerCharacterInput {
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
  const [reachability, setReachability] = React.useState<PieceReachability | null>(null);
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

  async function submitCharacter(event: React.FormEvent) {
    event.preventDefault();
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
        await fetchPieceReachability(sessionId, piece.sessionPieceId, playerToken),
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

  const assignedCharacterIds = new Set(pieces.map((piece) => piece.characterId));
  const availableCharacters = characters.filter(
    (character) => !assignedCharacterIds.has(character.characterId),
  );

  function characterName(characterId: string) {
    return characters.find((character) => character.characterId === characterId)?.name
      ?? "Personaggio";
  }

  const previewSpeedCells = Number(form.speedFeet || 0) / 5;

  return (
    <main className="min-h-screen bg-transparent px-4 py-5 text-[#111111] sm:px-6 sm:py-8">
      <header className="bh-surface mx-auto flex w-full max-w-320 flex-col gap-4 p-4 sm:flex-row sm:items-center sm:justify-between sm:p-5">
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
            icon={<RefreshCw className={isLoading ? "animate-spin" : ""} size={17} aria-hidden="true" />}
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

      <section className="mx-auto mt-4 grid w-full max-w-320 gap-3 sm:grid-cols-3">
        <SummaryCard accent="cyan" icon={<UserRound size={18} />} label="Giocatore">
          <strong className="truncate text-xl font-extrabold leading-tight">
            {identity?.displayName ?? "Verifica in corso"}
          </strong>
        </SummaryCard>
        <SummaryCard accent="lime" icon={<Check size={18} />} label="Stato">
          <StatusChip tone={identity?.status === "ACTIVE" ? "success" : "neutral"} dot>
            {identity?.status === "ACTIVE" ? "Accesso attivo" : "Verifica in corso"}
          </StatusChip>
        </SummaryCard>
        <SummaryCard accent="yellow" icon={<BookOpen size={18} />} label="Personaggi">
          <strong className="text-xl font-extrabold leading-tight">{characters.length}</strong>
        </SummaryCard>
      </section>

      {error ? (
        <AlertBanner className="mx-auto mt-4 w-full max-w-320" tone="danger" icon={<AlertCircle size={18} />} role="alert">
          <span>{error}</span>
        </AlertBanner>
      ) : null}

      <section className="bh-surface mx-auto mt-4 w-full max-w-320 p-4 sm:p-5">
        <div className="-mx-4 -mt-4 flex flex-col gap-3 border-b-2 border-[#111111] bg-[#ff8bc7] px-4 py-3 sm:-mx-5 sm:-mt-5 sm:flex-row sm:items-center sm:justify-between sm:px-5">
          <div className="flex items-center gap-3">
            <span className="bh-card-icon"><BookOpen size={18} /></span>
            <div>
              <h2 className="text-xl">I miei personaggi</h2>
              <p className="mt-1 text-sm font-medium text-slate-800">
                Crea la scheda tattica che userai per la tua pedina.
              </p>
            </div>
          </div>
          <Button
            variant={showForm ? "secondary" : "primary"}
            icon={showForm ? <X size={18} aria-hidden="true" /> : <Plus size={18} aria-hidden="true" />}
            onClick={() => setShowForm((current) => !current)}
          >
            {showForm ? "Chiudi modulo" : "Nuovo personaggio"}
          </Button>
        </div>

        {showForm ? (
          <form className="mt-5 grid gap-4" onSubmit={submitCharacter}>
            <fieldset className="bh-form-group">
              <legend className="bh-form-legend bg-[#ff8bc7]">01 · Identità del personaggio</legend>
              <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                <CharacterTextField
                  label="Nome"
                  value={form.name}
                  placeholder="Es. Elaria"
                  description="Il nome con cui il personaggio sarà mostrato al tavolo."
                  onChange={(value) => updateField("name", value)}
                />
                <CharacterCombobox
                  label="Specie"
                  value={form.species}
                  placeholder="Scegli o scrivi una specie"
                  description="Puoi usare una specie suggerita o una consentita dalla campagna."
                  options={SPECIES_SUGGESTIONS}
                  onChange={(value) => updateField("species", value)}
                />
                <CharacterCombobox
                  label="Classe"
                  value={form.className}
                  placeholder="Scegli o scrivi una classe"
                  description="La classe principale indicata sulla scheda del personaggio."
                  options={CLASS_SUGGESTIONS}
                  onChange={(value) => updateField("className", value)}
                />
              </div>
            </fieldset>

            <fieldset className="bh-form-group bg-[#fffdf2]">
              <legend className="bh-form-legend bg-[#ffd400]">02 · Valori tattici</legend>
              <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                <NumberStepper
                  label="Livello"
                  value={form.level}
                  min={1}
                  max={20}
                  description="Livello attuale del personaggio, da 1 a 20."
                  onChange={(value) => updateField("level", value)}
                />
                <NumberStepper
                  label="Punti ferita massimi"
                  value={form.hpMax}
                  min={1}
                  max={1000000}
                  description="Il totale massimo riportato sulla scheda."
                  onChange={(value) => updateField("hpMax", value)}
                />
                <NumberStepper
                  label="Classe Armatura (CA)"
                  value={form.armorClass}
                  min={0}
                  max={100}
                  description="Valore totale con armatura, Destrezza e bonus."
                  onChange={(value) => updateField("armorClass", value)}
                />
                <NumberStepper
                  label="Velocità (piedi)"
                  value={form.speedFeet}
                  min={0}
                  max={500}
                  step={5}
                  description={`${previewSpeedCells} celle sulla plancia.`}
                  onChange={(value) => updateField("speedFeet", value)}
                />
              </div>
            </fieldset>

            <details className="bh-form-group group">
              <summary className="flex cursor-pointer list-none items-center justify-between gap-3 text-sm font-extrabold uppercase tracking-[0.05em] text-[#111111] marker:content-none">
                <span>03 · Dettagli facoltativi e privacy</span>
                <span className="grid h-7 w-7 place-items-center border-2 border-[#111111] bg-[#c8b1ff] leading-none group-open:bg-[#ff8bc7]">
                  <Plus className="transition-transform group-open:rotate-45" size={16} aria-hidden="true" />
                </span>
              </summary>
              <div className="mt-3 grid gap-3 sm:grid-cols-2">
                <CharacterNumberField
                  label="Età"
                  value={form.age}
                  min={1}
                  description="Dato narrativo facoltativo; non modifica le regole di movimento."
                  onChange={(value) => updateField("age", value)}
                  required={false}
                />
                <CustomSelect
                  label="Visibilità nel gruppo"
                  value={form.partyVisibility}
                  options={Object.entries(VISIBILITY_LABELS).map(([value, label]) => ({
                    value: value as CharacterPartyVisibility,
                    label,
                  }))}
                  description="Il DM può sempre vedere la scheda completa."
                  onChange={(value) => updateField("partyVisibility", value)}
                />
              </div>
            </details>

            <div className="bh-form-preview">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div>
                  <span className="bh-kicker">Anteprima scheda</span>
                  <h3 className="mt-3 text-2xl">{form.name || "Nome del personaggio"}</h3>
                  <p className="mt-1 text-sm font-bold text-slate-700">
                    {form.species || "Specie"} · {form.className || "Classe"} · Livello {form.level}
                  </p>
                </div>
                <StatusChip tone="info">{VISIBILITY_LABELS[form.partyVisibility]}</StatusChip>
              </div>
              <div className="mt-4 grid grid-cols-1 gap-2 sm:grid-cols-3">
                <PreviewStat label="Punti ferita" value={`${form.hpMax || "-"}/${form.hpMax || "-"}`} />
                <PreviewStat label="Classe armatura" value={form.armorClass || "-"} />
                <PreviewStat label="Movimento" value={`${previewSpeedCells} celle`} />
              </div>
            </div>

            <div className="flex flex-col gap-2 border-t-2 border-[#111111] pt-4 sm:flex-row">
              <Button
                variant="primary"
                type="submit"
                disabled={isCreating}
                icon={isCreating ? <LoaderCircle className="animate-spin" size={18} /> : <Plus size={18} />}
              >
                Crea personaggio
              </Button>
              <Button
                variant="secondary"
                onClick={() => setShowForm(false)}
              >
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
            <span className="bh-empty-icon bg-[#ff8bc7]"><UserRound size={22} /></span>
            <p className="mt-3 font-medium text-slate-700">Nessun personaggio creato</p>
            <p className="mt-1 text-sm text-slate-500">Crea il primo personaggio per poter generare una pedina.</p>
          </div>
        ) : (
          <ul className="mt-6 grid gap-3 md:grid-cols-2 xl:grid-cols-3">
            {characters.map((character) => (
              <li className="bh-surface bh-surface--cream p-4" key={character.characterId}>
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="font-semibold text-slate-950">{character.name}</p>
                    <p className="mt-1 text-sm text-slate-500">
                      {character.species} · {character.className} · Livello {character.level}
                    </p>
                  </div>
                  <StatusChip tone="neutral">{VISIBILITY_LABELS[character.partyVisibility]}</StatusChip>
                </div>
                <div className="mt-4 grid grid-cols-3 gap-2 text-sm">
                  <CharacterStat icon={<Heart size={16} />} label="PF" value={`${character.hpCurrent}/${character.hpMax}`} />
                  <CharacterStat icon={<Shield size={16} />} label="CA" value={String(character.armorClass)} />
                  <CharacterStat icon={<Swords size={16} />} label="Velocità" value={`${character.speedCells}`} />
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="bh-surface mx-auto mb-8 mt-4 w-full max-w-320 p-4 sm:p-5">
        <div className="-mx-4 -mt-4 flex items-center gap-3 border-b-2 border-[#111111] bg-[#ffd400] px-4 py-3 sm:-mx-5 sm:-mt-5 sm:px-5">
          <span className="bh-card-icon"><MapPin size={18} /></span>
          <div>
            <h2 className="text-xl">Le mie pedine</h2>
            <p className="mt-1 text-sm font-medium text-slate-800">
              Associa un personaggio alla griglia, calcola le destinazioni e conferma il movimento.
            </p>
          </div>
        </div>

        {availableCharacters.length > 0 ? (
          <form className="bh-surface-flat mt-5 grid gap-3 bg-[#fbfaf6] p-4 sm:grid-cols-[minmax(220px,1fr)_150px_auto] sm:items-end" onSubmit={submitPiece}>
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
            <label className="flex flex-col gap-1.5 text-sm font-medium text-slate-700">
              Cella iniziale
              <input
                className="bh-input h-10 px-3 uppercase"
                value={startCell}
                onChange={(event) => setStartCell(event.target.value.toUpperCase())}
                pattern="[A-Za-z]+[1-9][0-9]*"
                placeholder="B2"
                required
              />
            </label>
            <Button
              variant="primary"
              type="submit"
              disabled={isCreatingPiece || !pieceCharacterId}
              icon={isCreatingPiece ? <LoaderCircle className="animate-spin" size={18} /> : <Plus size={18} />}
            >
              Genera pedina
            </Button>
          </form>
        ) : characters.length > 0 ? (
          <p className="bh-empty-state mt-5 px-4 py-4 text-sm text-slate-700">
            Ogni personaggio disponibile ha già una pedina.
          </p>
        ) : null}

        {lastMove ? (
          <AlertBanner className="mt-4" tone="success" icon={<Check size={18} />}>
            Movimento confermato: {lastMove}
          </AlertBanner>
        ) : null}

        {pieces.length === 0 ? (
          <div className="bh-empty-state mt-6 px-4 py-8 text-center">
            <span className="bh-empty-icon bg-[#ffd400]"><MapPin size={22} /></span>
            <p className="mt-3 font-medium text-slate-700">Nessuna pedina sulla griglia</p>
            <p className="mt-1 text-sm text-slate-500">Prima crea un personaggio, poi scegli una cella libera.</p>
          </div>
        ) : (
          <ul className="mt-6 grid gap-3 md:grid-cols-2">
            {pieces.map((piece) => (
              <li className="bh-surface bh-surface--cyan p-4" key={piece.sessionPieceId}>
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="font-semibold text-slate-950">{characterName(piece.characterId)}</p>
                    <p className="mt-1 text-sm text-slate-500">Pedina {piece.representationMode.toLowerCase()}</p>
                  </div>
                  <StatusChip tone="info">Cella {piece.currentCell}</StatusChip>
                </div>
                <div className="mt-4 flex items-center justify-between border-t-2 border-[#111111] pt-4">
                  <span className="text-xs text-slate-500">Versione {piece.version}</span>
                  <Button
                    size="compact"
                    variant="secondary"
                    icon={busyPieceId === piece.sessionPieceId ? <LoaderCircle className="animate-spin" size={17} /> : <MapPin size={17} />}
                    disabled={busyPieceId === piece.sessionPieceId}
                    onClick={() => void showReachableCells(piece)}
                  >
                    Muovi
                  </Button>
                </div>

                {reachability?.sessionPieceId === piece.sessionPieceId ? (
                  <div className="mt-4 border-t border-slate-100 pt-4">
                    <div className="flex items-center justify-between gap-2 text-sm">
                      <span className="font-medium text-slate-700">Celle raggiungibili</span>
                      <button className="cursor-pointer rounded-[1px] border-2 border-transparent px-2 py-1 text-xs font-bold uppercase tracking-wide text-slate-600 hover:border-[#111111] hover:bg-[#fff2a7] hover:text-[#111111]" type="button" onClick={() => setReachability(null)}>
                        Chiudi
                      </button>
                    </div>
                    <p className="mt-1 text-xs text-slate-500">
                      {reachability.movementPoints} punti movimento disponibili
                    </p>
                    <div className="mt-3 flex flex-wrap gap-2">
                      {reachability.reachableCells.map((cell) => (
                        <button
                          className="cursor-pointer rounded-[2px] border-2 border-[#111111] bg-[#d8f7fb] px-3 py-2 text-left text-sm font-bold text-[#111111] shadow-[2px_2px_0_#111111] transition hover:bg-[#8fe8f4] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#9165ff] disabled:cursor-not-allowed disabled:opacity-50"
                          key={cell.cell}
                          type="button"
                          title={`Percorso: ${cell.path.join(" → ")}`}
                          disabled={busyPieceId === piece.sessionPieceId}
                          onClick={() => void moveTo(cell.cell)}
                        >
                          <span className="font-semibold">{cell.cell}</span>
                          <span className="ml-2 text-xs">costo {cell.cost}</span>
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
      {description ? <span className="text-xs font-normal leading-4 text-slate-500">{description}</span> : null}
    </label>
  );
}

type CharacterComboboxProps = CharacterTextFieldProps & {
  options: string[];
};

function CharacterCombobox({
  label,
  value,
  description,
  placeholder,
  options,
  onChange,
}: CharacterComboboxProps) {
  const [isOpen, setIsOpen] = React.useState(false);
  const containerRef = React.useRef<HTMLDivElement>(null);
  const filteredOptions = options.filter((option) =>
    option.toLocaleLowerCase("it-IT").includes(value.trim().toLocaleLowerCase("it-IT")),
  );

  React.useEffect(() => {
    function closeOnOutsideClick(event: PointerEvent) {
      if (!containerRef.current?.contains(event.target as Node)) setIsOpen(false);
    }
    document.addEventListener("pointerdown", closeOnOutsideClick);
    return () => document.removeEventListener("pointerdown", closeOnOutsideClick);
  }, []);

  return (
    <div className="relative flex flex-col gap-1.5 text-sm font-bold text-[#111111]" ref={containerRef}>
      <span>{label}</span>
      <div className={`bh-input flex h-10 items-center ${isOpen ? "bg-[#fff8cc]" : ""}`}>
        <input
          className="h-full min-w-0 flex-1 bg-transparent px-3 font-normal text-slate-950 outline-none"
          role="combobox"
          aria-expanded={isOpen}
          autoComplete="off"
          value={value}
          placeholder={placeholder}
          maxLength={80}
          required
          onFocus={() => setIsOpen(true)}
          onChange={(event) => {
            onChange(event.target.value);
            setIsOpen(true);
          }}
          onKeyDown={(event) => {
            if (event.key === "Escape") setIsOpen(false);
          }}
        />
        <button
          className="grid h-9 w-9 shrink-0 cursor-pointer place-items-center text-slate-500 hover:text-slate-950"
          type="button"
          aria-label={`Apri opzioni ${label.toLocaleLowerCase("it-IT")}`}
          onClick={() => setIsOpen((current) => !current)}
        >
          <ChevronDown className={isOpen ? "rotate-180" : ""} size={17} aria-hidden="true" />
        </button>
      </div>
      {description ? <span className="text-xs font-normal leading-4 text-slate-500">{description}</span> : null}
      {isOpen && filteredOptions.length > 0 ? (
        <ul className="absolute inset-x-0 top-[66px] z-20 max-h-52 overflow-auto rounded-[2px] border-2 border-[#111111] bg-white p-1.5 shadow-[4px_4px_0_#111111]" role="listbox">
          {filteredOptions.map((option) => (
            <li key={option}>
              <button
                className="flex w-full cursor-pointer items-center justify-between rounded-[1px] border-2 border-transparent px-3 py-2 text-left text-sm font-bold text-slate-700 hover:border-[#111111] hover:bg-[#fff2a7]"
                type="button"
                role="option"
                aria-selected={option === value}
                onClick={() => {
                  onChange(option);
                  setIsOpen(false);
                }}
              >
                {option}
                {option === value ? <Check size={16} aria-hidden="true" /> : null}
              </button>
            </li>
          ))}
        </ul>
      ) : null}
    </div>
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
      {description ? <span className="text-xs font-normal leading-4 text-slate-500">{description}</span> : null}
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
  const selectedLabel = options.find((option) => option.value === value)?.label ?? value;

  React.useEffect(() => {
    function closeOnOutsideClick(event: PointerEvent) {
      if (!containerRef.current?.contains(event.target as Node)) setIsOpen(false);
    }
    document.addEventListener("pointerdown", closeOnOutsideClick);
    return () => document.removeEventListener("pointerdown", closeOnOutsideClick);
  }, []);

  return (
    <div className="relative flex flex-col gap-1.5 text-sm font-bold text-[#111111]" ref={containerRef}>
      <span>{label}</span>
      <button
        className={`bh-input flex h-10 w-full cursor-pointer items-center justify-between px-3 text-left font-normal ${isOpen ? "bg-[#fff8cc]" : ""}`}
        type="button"
        aria-haspopup="listbox"
        aria-expanded={isOpen}
        onClick={() => setIsOpen((current) => !current)}
      >
        {selectedLabel}
        <ChevronDown className={isOpen ? "rotate-180" : ""} size={17} aria-hidden="true" />
      </button>
      {description ? <span className="text-xs font-normal leading-4 text-slate-500">{description}</span> : null}
      {isOpen ? (
        <ul className="absolute inset-x-0 top-[66px] z-20 rounded-[2px] border-2 border-[#111111] bg-white p-1.5 shadow-[4px_4px_0_#111111]" role="listbox">
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
                {option.value === value ? <Check size={16} aria-hidden="true" /> : null}
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
      {description ? <span className="text-xs font-normal leading-4 text-slate-500">{description}</span> : null}
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
      <span className="flex items-center gap-1 text-slate-500">{icon}{label}</span>
      <p className="mt-1 font-semibold text-slate-950">{value}</p>
    </div>
  );
}

function PreviewStat({ label, value }: { label: string; value: string }) {
  return (
    <div className="border-2 border-[#111111] bg-white p-2.5 shadow-[2px_2px_0_#111111]">
      <span className="block text-[12px] font-bold uppercase leading-[1.25] tracking-[0.03em] text-slate-700">{label}</span>
      <strong className="mt-1 block text-base font-black text-[#111111]">{value}</strong>
    </div>
  );
}
