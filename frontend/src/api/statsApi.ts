import { STATS_API_BASE_URL } from "../config";
import type {
  LeaderboardEntry,
  PlayerStatistics,
  SessionResult,
  Tournament,
} from "../types";

async function readJson<T>(response: Response): Promise<T> {
  const body = (await response.json().catch(() => null)) as
    | T
    | { message?: string }
    | null;

  if (!response.ok) {
    const message =
      body && typeof body === "object" && "message" in body
        ? body.message
        : null;
    throw new Error(message ?? `Il servizio statistiche ha risposto con stato ${response.status}.`);
  }

  return body as T;
}

export async function fetchSessionResults(): Promise<SessionResult[]> {
  const response = await fetch(`${STATS_API_BASE_URL}/api/v1/stats/sessions`);
  return readJson<SessionResult[]>(response);
}

export async function fetchPlayerStatistics(
  playerReference: string,
): Promise<PlayerStatistics> {
  const response = await fetch(
    `${STATS_API_BASE_URL}/api/v1/stats/players/${encodeURIComponent(playerReference.trim())}`,
  );
  return readJson<PlayerStatistics>(response);
}

export async function fetchTournaments(): Promise<Tournament[]> {
  const response = await fetch(`${STATS_API_BASE_URL}/api/v1/tournaments`);
  return readJson<Tournament[]>(response);
}

export async function fetchTournamentLeaderboard(
  tournamentId: string,
): Promise<LeaderboardEntry[]> {
  const response = await fetch(
    `${STATS_API_BASE_URL}/api/v1/tournaments/${encodeURIComponent(tournamentId)}/leaderboard`,
  );
  return readJson<LeaderboardEntry[]>(response);
}
