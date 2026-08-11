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
  status: "DISABLED" | "CLAIMABLE" | "IN_SESSION";
  claimExpiresAt: string | null;
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

export type CharacterPartyVisibility = "OWNER_ONLY" | "PARTY" | "DM_ONLY";

export type PlayerCharacter = {
  characterId: string;
  sessionId: string;
  participantId: string;
  name: string;
  species: string;
  age: number | null;
  className: string;
  level: number;
  speedCells: number;
  hpCurrent: number;
  hpMax: number;
  armorClass: number;
  partyVisibility: CharacterPartyVisibility;
  version: number;
  createdAt: string;
  updatedAt: string;
};

export type CreatePlayerCharacterInput = {
  name: string;
  species: string;
  age: number | null;
  className: string;
  level: number;
  speedCells: number;
  hpCurrent?: number;
  hpMax: number;
  armorClass: number;
  partyVisibility: CharacterPartyVisibility;
};

export type SessionPiece = {
  sessionPieceId: string;
  sessionId: string;
  characterId: string;
  participantId: string;
  representationMode: "VIRTUAL" | "PHYSICAL";
  currentCell: string;
  version: number;
  createdAt: string;
  updatedAt: string;
};

export type ReachableCell = {
  cell: string;
  cost: number;
  path: string[];
  trapsOnPath: string[];
};

export type PieceReachability = {
  sessionPieceId: string;
  currentCell: string;
  movementPoints: number;
  version: number;
  reachableCells: ReachableCell[];
};

export type PieceMoveResult = {
  status: "CONFIRMED";
  commandId: string;
  eventId: string;
  sessionPieceId: string;
  characterId: string;
  from: string;
  to: string;
  path: string[];
  cost: number;
  version: number;
  visibleTrapsOnPath: string[];
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
  dmAccessToken: string;
};

export type ClosedSession = {
  sessionId: string;
  status: "ENDED";
  endedAt: string;
};
