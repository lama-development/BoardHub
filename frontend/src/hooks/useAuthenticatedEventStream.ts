import * as React from "react";

export type LiveConnectionState =
  | "CONNECTING"
  | "LIVE"
  | "RECONNECTING"
  | "ERROR";

type EventStreamMessage = {
  id?: string;
  event: string;
  data: string;
};

type UseAuthenticatedEventStreamOptions = {
  url: string;
  token: string;
  onEvent: (message: EventStreamMessage) => void;
};

const RETRY_DELAYS = [1000, 2000, 5000, 10000];

function parseMessage(block: string): EventStreamMessage | null {
  const lines = block.split(/\r?\n/);
  const data: string[] = [];
  let id: string | undefined;
  let event = "message";

  for (const line of lines) {
    if (!line || line.startsWith(":")) continue;
    const separator = line.indexOf(":");
    const field = separator === -1 ? line : line.slice(0, separator);
    const value = separator === -1 ? "" : line.slice(separator + 1).replace(/^ /, "");
    if (field === "id") id = value;
    if (field === "event") event = value;
    if (field === "data") data.push(value);
  }

  return data.length ? { id, event, data: data.join("\n") } : null;
}

export function useAuthenticatedEventStream({
  url,
  token,
  onEvent,
}: UseAuthenticatedEventStreamOptions): LiveConnectionState {
  const [state, setState] = React.useState<LiveConnectionState>("CONNECTING");
  const onEventRef = React.useRef(onEvent);

  React.useEffect(() => {
    onEventRef.current = onEvent;
  }, [onEvent]);

  React.useEffect(() => {
    const controller = new AbortController();
    let retryTimer: number | undefined;
    let retryAttempt = 0;

    const connect = async () => {
      setState(retryAttempt === 0 ? "CONNECTING" : "RECONNECTING");
      try {
        const response = await fetch(url, {
          headers: {
            Accept: "text/event-stream",
            Authorization: `Bearer ${token.trim()}`,
          },
          signal: controller.signal,
          cache: "no-store",
        });
        if (!response.ok || !response.body) {
          throw new Error(`Stream non disponibile (${response.status}).`);
        }

        retryAttempt = 0;
        setState("LIVE");
        // REST remains the authoritative state read. Sync once on every new
        // connection so events missed while reconnecting are recovered.
        onEventRef.current({ event: "connected", data: "" });
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = "";

        while (!controller.signal.aborted) {
          const { done, value } = await reader.read();
          if (done) break;
          buffer += decoder.decode(value, { stream: true });

          let boundary = buffer.match(/\r?\n\r?\n/);
          while (boundary?.index !== undefined) {
            const block = buffer.slice(0, boundary.index);
            buffer = buffer.slice(boundary.index + boundary[0].length);
            const message = parseMessage(block);
            if (message && message.event !== "connected") {
              onEventRef.current(message);
            }
            boundary = buffer.match(/\r?\n\r?\n/);
          }
        }
      } catch {
        if (controller.signal.aborted) return;
        setState("ERROR");
      }

      if (!controller.signal.aborted) {
        const delay = RETRY_DELAYS[Math.min(retryAttempt, RETRY_DELAYS.length - 1)];
        retryAttempt += 1;
        retryTimer = window.setTimeout(() => void connect(), delay);
      }
    };

    void connect();
    return () => {
      controller.abort();
      if (retryTimer !== undefined) window.clearTimeout(retryTimer);
    };
  }, [token, url]);

  return state;
}
