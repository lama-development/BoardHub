import { API_BASE_URL } from "../config";
import type {
  AcceptJoinRequestResult,
  CreatePlayerCharacterInput,
  ClosedSession,
  CreatedSession,
  GameEvent,
  HealthStatus,
  JoinRequest,
  Participant,
  PieceMoveResult,
  PieceReachability,
  PlayerCharacter,
  PlayerJoinStatus,
  PublicTableStatus,
  SessionPiece,
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
): Promise<CreatedSession> {
  const tableSuffix = String(table.tableNumber).padStart(2, "0");
  const response = await fetch(`${API_BASE_URL}/api/v1/sessions`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
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

function dmHeaders(dmToken: string) {
  return {
    Authorization: `Bearer ${dmToken.trim()}`,
  };
}

function playerHeaders(playerToken: string) {
  return {
    Authorization: `Bearer ${playerToken.trim()}`,
  };
}

export async function fetchPlayerIdentity(
  sessionId: string,
  playerToken: string,
): Promise<Participant> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/player/sessions/${encodeURIComponent(sessionId)}/me`,
    { headers: playerHeaders(playerToken) },
  );
  return readJson<Participant>(response);
}

export async function fetchPlayerCharacters(
  sessionId: string,
  playerToken: string,
): Promise<PlayerCharacter[]> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/player/sessions/${encodeURIComponent(sessionId)}/characters`,
    { headers: playerHeaders(playerToken) },
  );
  return readJson<PlayerCharacter[]>(response);
}

export async function createPlayerCharacter(
  sessionId: string,
  playerToken: string,
  character: CreatePlayerCharacterInput,
): Promise<PlayerCharacter> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/player/sessions/${encodeURIComponent(sessionId)}/characters`,
    {
      method: "POST",
      headers: {
        ...playerHeaders(playerToken),
        "Content-Type": "application/json",
      },
      body: JSON.stringify(character),
    },
  );
  return readJson<PlayerCharacter>(response);
}

export async function fetchPlayerPieces(
  sessionId: string,
  playerToken: string,
): Promise<SessionPiece[]> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/player/sessions/${encodeURIComponent(sessionId)}/pieces`,
    { headers: playerHeaders(playerToken) },
  );
  return readJson<SessionPiece[]>(response);
}

export async function createPlayerPiece(
  sessionId: string,
  playerToken: string,
  characterId: string,
  startCell: string,
): Promise<SessionPiece> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/player/sessions/${encodeURIComponent(sessionId)}/pieces`,
    {
      method: "POST",
      headers: {
        ...playerHeaders(playerToken),
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        characterId,
        representationMode: "VIRTUAL",
        startCell: startCell.trim().toUpperCase(),
      }),
    },
  );
  return readJson<SessionPiece>(response);
}

export async function fetchPieceReachability(
  sessionId: string,
  sessionPieceId: string,
  playerToken: string,
): Promise<PieceReachability> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/player/sessions/${encodeURIComponent(sessionId)}/pieces/${encodeURIComponent(sessionPieceId)}/reachable-cells`,
    { headers: playerHeaders(playerToken) },
  );
  return readJson<PieceReachability>(response);
}

export async function movePlayerPiece(
  sessionId: string,
  sessionPieceId: string,
  playerToken: string,
  destination: string,
  expectedVersion: number,
  commandId: string,
): Promise<PieceMoveResult> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/player/sessions/${encodeURIComponent(sessionId)}/pieces/${encodeURIComponent(sessionPieceId)}/moves`,
    {
      method: "POST",
      headers: {
        ...playerHeaders(playerToken),
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ destination, expectedVersion, commandId }),
    },
  );
  return readJson<PieceMoveResult>(response);
}

export async function fetchDmCharacters(
  sessionId: string,
  dmToken: string,
): Promise<PlayerCharacter[]> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/dm/sessions/${encodeURIComponent(sessionId)}/characters`,
    { headers: dmHeaders(dmToken) },
  );
  return readJson<PlayerCharacter[]>(response);
}

export async function fetchDmPieces(
  sessionId: string,
  dmToken: string,
): Promise<SessionPiece[]> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/dm/sessions/${encodeURIComponent(sessionId)}/pieces`,
    { headers: dmHeaders(dmToken) },
  );
  return readJson<SessionPiece[]>(response);
}

export async function fetchDmJoinRequests(
  sessionId: string,
  dmToken: string,
): Promise<JoinRequest[]> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/dm/sessions/${encodeURIComponent(sessionId)}/join-requests?status=PENDING`,
    { headers: dmHeaders(dmToken) },
  );
  return readJson<JoinRequest[]>(response);
}

export async function fetchDmParticipants(
  sessionId: string,
  dmToken: string,
): Promise<Participant[]> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/dm/sessions/${encodeURIComponent(sessionId)}/participants`,
    { headers: dmHeaders(dmToken) },
  );
  return readJson<Participant[]>(response);
}

export async function acceptDmJoinRequest(
  sessionId: string,
  requestId: string,
  dmToken: string,
): Promise<AcceptJoinRequestResult> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/dm/sessions/${encodeURIComponent(sessionId)}/join-requests/${encodeURIComponent(requestId)}/accept`,
    {
      method: "POST",
      headers: dmHeaders(dmToken),
    },
  );
  return readJson<AcceptJoinRequestResult>(response);
}

export async function rejectDmJoinRequest(
  sessionId: string,
  requestId: string,
  dmToken: string,
): Promise<JoinRequest> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/dm/sessions/${encodeURIComponent(sessionId)}/join-requests/${encodeURIComponent(requestId)}/reject`,
    {
      method: "POST",
      headers: dmHeaders(dmToken),
    },
  );
  return readJson<JoinRequest>(response);
}

export async function closeDmSession(
  sessionId: string,
  dmToken: string,
): Promise<ClosedSession> {
  const response = await fetch(
    `${API_BASE_URL}/api/v1/dm/sessions/${encodeURIComponent(sessionId)}/close`,
    {
      method: "POST",
      headers: dmHeaders(dmToken),
    },
  );
  return readJson<ClosedSession>(response);
}
