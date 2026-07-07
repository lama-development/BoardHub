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
