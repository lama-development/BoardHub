export type GameEvent = {
  eventId: string;
  eventType: string;
  venueId: string;
  tableId: string;
  sessionId: string;
  source: string;
  occurredAt: string;
  sequenceNumber: number;
  payload: Record<string, unknown>;
};

export type HealthStatus = "UP" | "DOWN" | "UNKNOWN";

export type BoardToken = {
  id: string;
  cell: string;
  kind: "character" | "monster";
};

export type PublicActiveSession = {
  tablePublicId: string;
  tableDisplayName: string;
  sessionId: string;
  title: string;
  gameType: string;
  publicSummary: string | null;
};

export type PublicTableStatus = {
  tablePublicId: string;
  tableNumber: number;
  tableDisplayName: string;
  status: "AVAILABLE" | "IN_SESSION";
  activeSession: PublicActiveSession | null;
};

export type JoinRequest = {
  requestId: string;
  sessionId: string;
  playerReference: string;
  displayName: string;
  status: "PENDING" | "ACCEPTED" | "REJECTED" | "EXPIRED";
  requestedAt: string;
  expiresAt: string;
};

export type Participant = {
  participantId: string;
  sessionId: string;
  role: "PLAYER" | "DM";
  displayName: string;
  status: "ACTIVE" | "LEFT";
  joinedAt: string;
};

export type AcceptJoinRequestResult = {
  request: JoinRequest;
  participant: Participant;
  accessToken: string;
};

export type PlayerJoinStatus = {
  request: JoinRequest;
  participant: Participant | null;
  accessToken: string | null;
};

export type CreatedSession = {
  sessionId: string;
  tablePublicId: string;
  tableDisplayName: string;
  title: string;
  status: string;
};
