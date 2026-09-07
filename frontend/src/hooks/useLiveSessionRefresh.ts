import * as React from "react";
import { useAuthenticatedEventStream } from "./useAuthenticatedEventStream";

type UseLiveSessionRefreshOptions = {
  url: string;
  token: string;
  refresh: () => Promise<void>;
};

/**
 * Batches bursts of SSE events into one REST refresh and cleans up its timer.
 * The API remains the source of truth after a live notification.
 */
export function useLiveSessionRefresh({
  url,
  token,
  refresh,
}: UseLiveSessionRefreshOptions) {
  const refreshTimer = React.useRef<number | null>(null);

  const scheduleRefresh = React.useCallback(() => {
    if (refreshTimer.current !== null) return;

    refreshTimer.current = window.setTimeout(() => {
      refreshTimer.current = null;
      void refresh();
    }, 150);
  }, [refresh]);

  const state = useAuthenticatedEventStream({
    url,
    token,
    onEvent: scheduleRefresh,
  });

  React.useEffect(
    () => () => {
      if (refreshTimer.current !== null) {
        window.clearTimeout(refreshTimer.current);
        refreshTimer.current = null;
      }
    },
    [refresh],
  );

  return state;
}
