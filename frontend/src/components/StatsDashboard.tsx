import * as React from "react";
import {
  AlertCircle,
  BarChart3,
  Footprints,
  HeartPulse,
  History,
  RefreshCw,
  Search,
  ShieldCheck,
  Swords,
  Trophy,
  Users,
} from "lucide-react";
import {
  fetchPlayerStatistics,
  fetchSessionResults,
  fetchTournamentLeaderboard,
  fetchTournaments,
} from "../api/statsApi";
import type {
  LeaderboardEntry,
  PlayerStatistics,
  SessionResult,
  Tournament,
} from "../types";
import {
  AlertBanner,
  Button,
  PageHeaderIdentity,
  StatusChip,
  SummaryCard,
  Surface,
} from "./ui";

type StatsView = "sessions" | "player" | "leaderboard";

const dateFormatter = new Intl.DateTimeFormat("it-IT", {
  dateStyle: "medium",
  timeStyle: "short",
});

function formatDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : dateFormatter.format(date);
}

function formatDuration(minutes: number) {
  if (minutes < 60) return `${minutes} min`;
  return `${Math.floor(minutes / 60)} h ${minutes % 60} min`;
}

function EmptyState({ children }: { children: React.ReactNode }) {
  return (
    <div className="bh-empty-state p-6 text-center text-sm font-semibold leading-6 text-slate-700">
      {children}
    </div>
  );
}

export function StatsDashboard() {
  const [view, setView] = React.useState<StatsView>("sessions");
  const [sessions, setSessions] = React.useState<SessionResult[]>([]);
  const [tournaments, setTournaments] = React.useState<Tournament[]>([]);
  const [selectedSessionId, setSelectedSessionId] = React.useState<string | null>(null);
  const [selectedTournamentId, setSelectedTournamentId] = React.useState("");
  const [leaderboard, setLeaderboard] = React.useState<LeaderboardEntry[]>([]);
  const [playerReference, setPlayerReference] = React.useState("");
  const [playerStats, setPlayerStats] = React.useState<PlayerStatistics | null>(null);
  const [isLoading, setIsLoading] = React.useState(true);
  const [isLoadingLeaderboard, setIsLoadingLeaderboard] = React.useState(false);
  const [isSearchingPlayer, setIsSearchingPlayer] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);

  const selectedSession = sessions.find((session) => session.sessionId === selectedSessionId) ?? null;
  const selectedTournament = tournaments.find((tournament) => tournament.tournamentId === selectedTournamentId) ?? null;

  const loadOverview = React.useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [nextSessions, nextTournaments] = await Promise.all([
        fetchSessionResults(),
        fetchTournaments(),
      ]);
      setSessions(nextSessions);
      setSelectedSessionId((current) => current ?? nextSessions.at(0)?.sessionId ?? null);
      setTournaments(nextTournaments);
      setSelectedTournamentId((current) => current || nextTournaments.at(0)?.tournamentId || "");
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "Impossibile caricare le statistiche.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  React.useEffect(() => {
    void loadOverview();
  }, [loadOverview]);

  React.useEffect(() => {
    if (!selectedTournamentId) {
      setLeaderboard([]);
      return;
    }

    let isCurrent = true;
    setIsLoadingLeaderboard(true);
    setError(null);
    void fetchTournamentLeaderboard(selectedTournamentId)
      .then((entries) => {
        if (isCurrent) setLeaderboard(entries);
      })
      .catch((loadError) => {
        if (isCurrent) {
          setLeaderboard([]);
          setError(loadError instanceof Error ? loadError.message : "Impossibile caricare la classifica.");
        }
      })
      .finally(() => {
        if (isCurrent) setIsLoadingLeaderboard(false);
      });

    return () => {
      isCurrent = false;
    };
  }, [selectedTournamentId]);

  async function searchPlayer(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const reference = playerReference.trim();
    if (!reference) return;

    setIsSearchingPlayer(true);
    setError(null);
    try {
      setPlayerStats(await fetchPlayerStatistics(reference));
    } catch (searchError) {
      setPlayerStats(null);
      setError(searchError instanceof Error ? searchError.message : "Giocatore non trovato.");
    } finally {
      setIsSearchingPlayer(false);
    }
  }

  return (
    <main className="min-h-screen px-4 py-5 text-[#111111] sm:px-6 sm:py-7">
      <header className="bh-surface mx-auto flex w-full max-w-7xl flex-col gap-4 p-4 lg:flex-row lg:items-center lg:justify-between sm:p-5">
        <PageHeaderIdentity
          accentClassName="bg-[#c8b1ff]"
          eyebrow="BoardHub · Risultati"
          icon={<BarChart3 size={20} />}
          title="Statistiche di gioco"
        />
        <div className="flex flex-wrap gap-2">
          <a
            className="inline-flex h-10 items-center justify-center rounded-[2px] border-2 border-[#111111] bg-white px-4 text-sm font-extrabold uppercase tracking-[0.045em] shadow-[3px_3px_0_#111111] transition-all hover:translate-x-[1px] hover:translate-y-[1px] hover:bg-[#ffd400] hover:shadow-[2px_2px_0_#111111] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#9165ff] focus-visible:ring-offset-2"
            href="/"
          >
            Dashboard gioco
          </a>
          <Button variant="primary" onClick={() => void loadOverview()} disabled={isLoading} icon={<RefreshCw className={isLoading ? "animate-spin" : ""} size={17} />}>
            Aggiorna
          </Button>
        </div>
      </header>

      <section className="mx-auto mt-4 grid w-full max-w-7xl gap-3 sm:grid-cols-3">
        <SummaryCard accent="purple" icon={<History size={19} />} label="Sessioni concluse">
          <strong className="text-xl leading-none">{sessions.length}</strong>
        </SummaryCard>
        <SummaryCard accent="cyan" icon={<Users size={19} />} label="Giocatori registrati">
          <strong className="text-xl leading-none">{new Set(sessions.flatMap((session) => session.participants.map((player) => player.playerReference))).size}</strong>
        </SummaryCard>
        <SummaryCard accent="yellow" icon={<Trophy size={19} />} label="Tornei">
          <strong className="text-xl leading-none">{tournaments.length}</strong>
        </SummaryCard>
      </section>

      <nav className="mx-auto mt-4 flex w-full max-w-7xl flex-wrap gap-2" aria-label="Sezioni statistiche">
        {([
          ["sessions", "Storico sessioni", History],
          ["player", "Statistiche giocatore", Users],
          ["leaderboard", "Classifica tornei", Trophy],
        ] as const).map(([nextView, label, Icon]) => (
          <Button
            key={nextView}
            aria-pressed={view === nextView}
            variant={view === nextView ? "primary" : "secondary"}
            onClick={() => setView(nextView)}
            icon={<Icon size={17} />}
          >
            {label}
          </Button>
        ))}
      </nav>

      <section className="mx-auto mt-4 w-full max-w-7xl">
        {error ? <AlertBanner tone="danger" icon={<AlertCircle size={18} />}>{error}</AlertBanner> : null}
        {view === "sessions" ? <SessionHistory isLoading={isLoading} selectedSession={selectedSession} sessions={sessions} onSelect={setSelectedSessionId} /> : null}
        {view === "player" ? <PlayerLookup isSearching={isSearchingPlayer} playerReference={playerReference} playerStats={playerStats} onChange={setPlayerReference} onSubmit={searchPlayer} /> : null}
        {view === "leaderboard" ? <TournamentLeaderboard isLoading={isLoading || isLoadingLeaderboard} leaderboard={leaderboard} selectedTournament={selectedTournament} selectedTournamentId={selectedTournamentId} tournaments={tournaments} onSelect={setSelectedTournamentId} /> : null}
      </section>
    </main>
  );
}

function SessionHistory({ isLoading, selectedSession, sessions, onSelect }: { isLoading: boolean; selectedSession: SessionResult | null; sessions: SessionResult[]; onSelect: (sessionId: string) => void }) {
  if (isLoading) return <EmptyState>Caricamento dello storico sessioni…</EmptyState>;
  if (!sessions.length) return <EmptyState>Nessuna sessione conclusa disponibile.</EmptyState>;

  return (
    <div className="grid gap-4 xl:grid-cols-[minmax(0,0.95fr)_minmax(0,1.35fr)]">
      <Surface className="overflow-hidden">
        <div className="border-b-2 border-[#111111] bg-[#fff7d1] px-4 py-3">
          <h2 className="text-xl">Storico sessioni</h2>
        </div>
        <ul className="divide-y-2 divide-[#111111]">
          {sessions.map((session) => (
            <li key={session.sessionId}>
              <button className="w-full px-4 py-3 text-left transition-colors hover:bg-[#d8f7fb] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-[#9165ff]" onClick={() => onSelect(session.sessionId)} aria-pressed={selectedSession?.sessionId === session.sessionId}>
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0"><strong className="block truncate text-base">{session.title}</strong><span className="mt-1 block text-sm text-slate-700">{formatDate(session.endedAt)} · {session.tableId}</span></div>
                  <StatusChip tone="info">{session.participants.length} gioc.</StatusChip>
                </div>
              </button>
            </li>
          ))}
        </ul>
      </Surface>
      {selectedSession ? <SessionDetails session={selectedSession} /> : <EmptyState>Seleziona una sessione per vedere il risultato.</EmptyState>}
    </div>
  );
}

function SessionDetails({ session }: { session: SessionResult }) {
  return (
    <Surface className="overflow-hidden">
      <div className="flex flex-wrap items-start justify-between gap-3 border-b-2 border-[#111111] bg-[#d8f7fb] px-4 py-3">
        <div><p className="text-xs font-bold uppercase tracking-wide text-slate-600">{session.gameType} · {session.tableId}</p><h2 className="mt-1 text-2xl">{session.title}</h2></div>
        <StatusChip tone="success">{formatDuration(session.durationMinutes)}</StatusChip>
      </div>
      <div className="grid gap-3 p-4 sm:grid-cols-3"><div><p className="text-xs font-bold uppercase text-slate-600">Inizio</p><p className="mt-1 font-semibold">{formatDate(session.startedAt)}</p></div><div><p className="text-xs font-bold uppercase text-slate-600">Fine</p><p className="mt-1 font-semibold">{formatDate(session.endedAt)}</p></div><div><p className="text-xs font-bold uppercase text-slate-600">Partecipanti</p><p className="mt-1 text-xl font-black">{session.participants.length}</p></div></div>
      <div className="overflow-x-auto border-t-2 border-[#111111]"><table className="w-full min-w-[640px] text-left text-sm"><thead className="bg-[#f7f1e4] text-xs uppercase"><tr><th className="px-4 py-3">Giocatore</th><th className="px-3 py-3">Esito</th><th className="px-3 py-3 text-right">Punti</th><th className="px-3 py-3 text-right">Mov.</th><th className="px-3 py-3 text-right">Trappole</th></tr></thead><tbody>{session.participants.map((player) => <tr className="border-t border-[#111111]" key={player.playerReference}><td className="px-4 py-3"><strong>{player.displayName}</strong><span className="block text-xs text-slate-600">{player.characterName ?? player.playerReference}</span></td><td className="px-3 py-3"><StatusChip tone={player.survived ? "success" : "danger"}>{player.survived ? "Sopravvissuto" : "Abbattuto"}</StatusChip></td><td className="px-3 py-3 text-right font-black">{player.points}</td><td className="px-3 py-3 text-right">{player.movesConfirmed}</td><td className="px-3 py-3 text-right">{player.trapsTriggered}</td></tr>)}</tbody></table></div>
    </Surface>
  );
}

function PlayerLookup({ isSearching, onChange, onSubmit, playerReference, playerStats }: { isSearching: boolean; onChange: (value: string) => void; onSubmit: (event: React.FormEvent<HTMLFormElement>) => void; playerReference: string; playerStats: PlayerStatistics | null }) {
  return <div className="grid gap-4 xl:grid-cols-[minmax(300px,0.8fr)_minmax(0,1.2fr)]"><Surface className="h-fit p-4"><h2 className="text-xl">Cerca un giocatore</h2><p className="mt-2 text-sm leading-6 text-slate-700">Inserisci il riferimento usato nella richiesta di ingresso al tavolo.</p><form className="mt-4 flex gap-2" onSubmit={onSubmit}><label className="sr-only" htmlFor="player-reference">Riferimento giocatore</label><input className="bh-input h-10 min-w-0 flex-1 px-3" id="player-reference" value={playerReference} onChange={(event) => onChange(event.target.value)} placeholder="es. player-001" /><Button variant="primary" disabled={isSearching} type="submit" icon={<Search size={17} />}>{isSearching ? "Cerco" : "Cerca"}</Button></form></Surface>{playerStats ? <PlayerStatsCard stats={playerStats} /> : <EmptyState>Le statistiche aggregate del giocatore compariranno qui.</EmptyState>}</div>;
}

function PlayerStatsCard({ stats }: { stats: PlayerStatistics }) {
  const metrics = [["Punti totali", stats.totalPoints, Trophy, "yellow"], ["Sessioni", stats.sessionsPlayed, Swords, "purple"], ["Sopravvivenza", `${Math.round(stats.survivalRate * 100)}%`, HeartPulse, "lime"], ["Celle percorse", stats.totalCellsTravelled, Footprints, "cyan"], ["Tiri riusciti", stats.totalSavesSucceeded, ShieldCheck, "pink"], ["Danni subiti", stats.totalDamageTaken, AlertCircle, "pink"]] as const;
  return <Surface className="overflow-hidden"><div className="border-b-2 border-[#111111] bg-[#d8f7fb] px-4 py-3"><p className="text-xs font-bold uppercase tracking-wide text-slate-600">{stats.playerReference}</p><h2 className="mt-1 text-2xl">{stats.displayName}</h2></div><div className="grid gap-3 p-4 sm:grid-cols-2 lg:grid-cols-3">{metrics.map(([label, value, Icon, accent]) => <SummaryCard key={label} label={label} icon={<Icon size={18} />} accent={accent}><strong className="text-xl leading-none">{value}</strong></SummaryCard>)}</div></Surface>;
}

function TournamentLeaderboard({ isLoading, leaderboard, onSelect, selectedTournament, selectedTournamentId, tournaments }: { isLoading: boolean; leaderboard: LeaderboardEntry[]; onSelect: (id: string) => void; selectedTournament: Tournament | null; selectedTournamentId: string; tournaments: Tournament[] }) {
  if (isLoading && !selectedTournament) return <EmptyState>Caricamento dei tornei…</EmptyState>;
  if (!tournaments.length) return <EmptyState>Nessun torneo disponibile.</EmptyState>;
  return <Surface className="overflow-hidden"><div className="flex flex-col gap-3 border-b-2 border-[#111111] bg-[#fff7d1] p-4 sm:flex-row sm:items-end sm:justify-between"><div><p className="text-xs font-bold uppercase tracking-wide text-slate-600">Classifica torneo</p><h2 className="mt-1 text-2xl">{selectedTournament?.name ?? "Seleziona un torneo"}</h2></div><label className="text-sm font-bold" htmlFor="tournament-select">Torneo<select className="bh-input mt-1 block h-10 min-w-64 px-3 font-normal" id="tournament-select" value={selectedTournamentId} onChange={(event) => onSelect(event.target.value)}>{tournaments.map((tournament) => <option key={tournament.tournamentId} value={tournament.tournamentId}>{tournament.name} · {tournament.gameType}</option>)}</select></label></div>{isLoading ? <div className="p-6 text-center text-sm font-semibold">Aggiornamento classifica…</div> : !leaderboard.length ? <EmptyState>Questo torneo non ha ancora risultati in classifica.</EmptyState> : <div className="overflow-x-auto"><table className="w-full min-w-[680px] text-left text-sm"><thead className="bg-[#f7f1e4] text-xs uppercase"><tr><th className="px-4 py-3">Pos.</th><th className="px-3 py-3">Giocatore</th><th className="px-3 py-3 text-right">Punti</th><th className="px-3 py-3 text-right">Sessioni</th><th className="px-3 py-3 text-right">Tiri</th><th className="px-3 py-3 text-right">Celle</th></tr></thead><tbody>{leaderboard.map((entry) => <tr className="border-t border-[#111111]" key={entry.playerReference}><td className="px-4 py-3"><span className="inline-grid h-7 w-7 place-items-center border-2 border-[#111111] bg-[#ffd400] font-black">{entry.position}</span></td><td className="px-3 py-3"><strong>{entry.displayName}</strong><span className="block text-xs text-slate-600">{entry.playerReference}</span></td><td className="px-3 py-3 text-right text-base font-black">{entry.points}</td><td className="px-3 py-3 text-right">{entry.sessionsPlayed}</td><td className="px-3 py-3 text-right">{entry.savesSucceeded}</td><td className="px-3 py-3 text-right">{entry.cellsTravelled}</td></tr>)}</tbody></table></div>}</Surface>;
}
