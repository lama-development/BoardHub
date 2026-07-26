import type { JoinRequest, Participant } from "../types";

const STORAGE_PREFIX = "boardhub-player-join";

export type PlayerJoinAccess = {
  version: 1;
  tablePublicId: string;
  sessionId: string;
  joinClaim: string;
  request: JoinRequest;
  participant: Participant | null;
  accessToken: string | null;
};

function storageKey(tablePublicId: string) {
  return `${STORAGE_PREFIX}:${tablePublicId}`;
}

export function readPlayerJoinAccess(tablePublicId: string): PlayerJoinAccess | null {
  try {
    const raw = window.localStorage.getItem(storageKey(tablePublicId));
    if (!raw) return null;
    const parsed = JSON.parse(raw) as Partial<PlayerJoinAccess>;
    if (
      parsed.version !== 1 ||
      parsed.tablePublicId !== tablePublicId ||
      typeof parsed.sessionId !== "string" ||
      typeof parsed.joinClaim !== "string" ||
      !parsed.request ||
      typeof parsed.request.requestId !== "string"
    ) {
      clearPlayerJoinAccess(tablePublicId);
      return null;
    }
    return parsed as PlayerJoinAccess;
  } catch {
    clearPlayerJoinAccess(tablePublicId);
    return null;
  }
}

export function savePlayerJoinAccess(access: PlayerJoinAccess) {
  window.localStorage.setItem(storageKey(access.tablePublicId), JSON.stringify(access));
}

export function clearPlayerJoinAccess(tablePublicId: string) {
  window.localStorage.removeItem(storageKey(tablePublicId));
}
