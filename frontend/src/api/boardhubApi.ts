import { API_BASE_URL } from "../config";
import type {
  AcceptJoinRequestResult,
  CreatedSession,
  GameEvent,
  HealthStatus,
  JoinRequest,
  Participant,
  PlayerJoinStatus,
  PublicTableStatus,
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
    throw new Error(message ?? `Il server ha risposto con stato ${response.status}.`);
  }
  return body as T;
}

export async function fetchSessionEvents(sessionId: string) {
  const response = await fetch(`${API_BASE_URL}/api/v1/sessions/${encodeURIComponent(sessionId)}/events`);

  if (!response.ok) {
    throw new Error(`Backend non disponibile o risposta non valida (${response.status}).`);
  }

  return (await response.json()) as GameEvent[];
}

export async function fetchBackendHealth(): Promise<HealthStatus> {
  try {
    const response = await fetch(`${API_BASE_URL}/actuator/health`);
    if (!response.ok) {
      return "DOWN";
    }

    const body = (await response.json()) as { status?: string };
    return body.status === "UP" ? "UP" : "UNKNOWN";
  } catch {
    return "DOWN";
  }
}

export async function fetchPublicTable(tablePublicId: string): Promise<PublicTableStatus> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/public/tables/${encodeURIComponent(tablePublicId)}`,
  );
  return readJson<PublicTableStatus>(response);
}

export async function requestSessionJoin(
  sessionId: string,
  displayName: string,
  playerReference: string,
  idempotencyKey: string,
): Promise<JoinRequest> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/public/sessions/${encodeURIComponent(sessionId)}/join-requests`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Idempotency-Key": idempotencyKey,
      },
      body: JSON.stringify({ playerReference, displayName }),
    },
  );
  return readJson<JoinRequest>(response);
}

export async function fetchPlayerJoinStatus(
  sessionId: string,
  requestId: string,
  joinClaim: string,
): Promise<PlayerJoinStatus> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/public/sessions/${encodeURIComponent(sessionId)}/join-requests/${encodeURIComponent(requestId)}`,
    {
      headers: {
        "X-BoardHub-Join-Claim": joinClaim,
      },
    },
  );
  return readJson<PlayerJoinStatus>(response);
}

export async function createTableSession(
  table: PublicTableStatus,
  title: string,
  dmKey: string,
): Promise<CreatedSession> {
  const tableSuffix = String(table.tableNumber).padStart(2, "0");
  const response = await fetch(`${API_BASE_URL}/api/v1/sessions`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-BoardHub-DM-Key": dmKey.trim(),
    },
    body: JSON.stringify({
      sessionId: `session-table-${tableSuffix}-${Date.now()}`,
      venueId: "venue-01",
      tableId: `table-${tableSuffix}`,
      tablePublicId: table.tablePublicId,
      tableDisplayName: table.tableDisplayName,
      title,
      gameType: "DND",
      publicSummary: "Sessione D&D locale aperta ai giocatori del tavolo.",
      acceptingJoinRequests: true,
      grid: {
        width: 5,
        height: 5,
        difficultCells: [],
        blockedCells: [],
        obstacleCells: [],
        occupiedCells: [],
        walls: [],
        traps: [],
      },
    }),
  });
  return readJson<CreatedSession>(response);
}

function dmHeaders(dmKey: string) {
  return {
    "X-BoardHub-DM-Key": dmKey.trim(),
  };
}

export async function fetchDmJoinRequests(
  sessionId: string,
  dmKey: string,
): Promise<JoinRequest[]> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/dm/sessions/${encodeURIComponent(sessionId)}/join-requests?status=PENDING`,
    { headers: dmHeaders(dmKey) },
  );
  return readJson<JoinRequest[]>(response);
}

export async function fetchDmParticipants(
  sessionId: string,
  dmKey: string,
): Promise<Participant[]> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/dm/sessions/${encodeURIComponent(sessionId)}/participants`,
    { headers: dmHeaders(dmKey) },
  );
  return readJson<Participant[]>(response);
}

export async function acceptDmJoinRequest(
  sessionId: string,
  requestId: string,
  dmKey: string,
): Promise<AcceptJoinRequestResult> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/dm/sessions/${encodeURIComponent(sessionId)}/join-requests/${encodeURIComponent(requestId)}/accept`,
    {
      method: "POST",
      headers: dmHeaders(dmKey),
    },
  );
  return readJson<AcceptJoinRequestResult>(response);
}

export async function rejectDmJoinRequest(
  sessionId: string,
  requestId: string,
  dmKey: string,
): Promise<JoinRequest> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/dm/sessions/${encodeURIComponent(sessionId)}/join-requests/${encodeURIComponent(requestId)}/reject`,
    {
      method: "POST",
      headers: dmHeaders(dmKey),
    },
  );
  return readJson<JoinRequest>(response);
}
