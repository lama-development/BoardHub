export const DEFAULT_SESSION_ID = "session-20260630-001";
export const API_BASE_URL = import.meta.env.VITE_BOARDHUB_API_URL ?? "";
// The stats service is a separate process in local development. Keep its base
// distinct from the game API so a production reverse proxy can route it too.
export const STATS_API_BASE_URL =
  import.meta.env.VITE_BOARDHUB_STATS_API_URL ?? "/stats-api";
