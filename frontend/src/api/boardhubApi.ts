import { API_BASE_URL } from "../config";
import type { GameEvent, HealthStatus } from "../types";

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
