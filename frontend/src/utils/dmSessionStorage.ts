import type { CreatedSession } from "../types";

type StoredDmAccess = {
  version: 1;
  dmKey: string;
  session: CreatedSession;
};

function storageKey(tablePublicId: string) {
  return `boardhub-dm-access-${tablePublicId}`;
}

function isCreatedSession(value: unknown): value is CreatedSession {
  if (!value || typeof value !== "object") return false;
  const session = value as Partial<CreatedSession>;
  return (
    typeof session.sessionId === "string" &&
    typeof session.tablePublicId === "string" &&
    typeof session.tableDisplayName === "string" &&
    typeof session.title === "string" &&
    typeof session.status === "string"
  );
}

export function readDmAccess(tablePublicId: string): StoredDmAccess | null {
  try {
    const rawValue = window.localStorage.getItem(storageKey(tablePublicId));
    if (!rawValue) return null;

    const value = JSON.parse(rawValue) as Partial<StoredDmAccess>;
    if (
      value.version !== 1 ||
      typeof value.dmKey !== "string" ||
      value.dmKey.length === 0 ||
      !isCreatedSession(value.session) ||
      value.session.tablePublicId !== tablePublicId
    ) {
      clearDmAccess(tablePublicId);
      return null;
    }

    return value as StoredDmAccess;
  } catch {
    clearDmAccess(tablePublicId);
    return null;
  }
}

export function saveDmAccess(
  tablePublicId: string,
  session: CreatedSession,
  dmKey: string,
) {
  const value: StoredDmAccess = {
    version: 1,
    dmKey: dmKey.trim(),
    session,
  };
  window.localStorage.setItem(storageKey(tablePublicId), JSON.stringify(value));
}

export function clearDmAccess(tablePublicId: string) {
  window.localStorage.removeItem(storageKey(tablePublicId));
}
