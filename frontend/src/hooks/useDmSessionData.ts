import * as React from "react";
import {
  fetchDmCharacters,
  fetchDmJoinRequests,
  fetchDmParticipants,
  fetchDmPendingTrapResolutions,
  fetchDmPieces,
} from "../api/boardhubApi";
import type {
  JoinRequest,
  Participant,
  PlayerCharacter,
  SessionPiece,
  TrapResolution,
} from "../types";
import { useLiveSessionRefresh } from "./useLiveSessionRefresh";
import type { LiveConnectionState } from "./useAuthenticatedEventStream";

type DmSessionData = {
  requests: JoinRequest[];
  participants: Participant[];
  characters: PlayerCharacter[];
  pieces: SessionPiece[];
  pendingTrapResolutions: TrapResolution[];
  error: string | null;
  isLoading: boolean;
  liveState: LiveConnectionState;
  setError: React.Dispatch<React.SetStateAction<string | null>>;
  refresh: (showLoading?: boolean) => Promise<void>;
};

export function useDmSessionData(
  sessionId: string,
  dmToken: string,
): DmSessionData {
  const [requests, setRequests] = React.useState<JoinRequest[]>([]);
  const [participants, setParticipants] = React.useState<Participant[]>([]);
  const [characters, setCharacters] = React.useState<PlayerCharacter[]>([]);
  const [pieces, setPieces] = React.useState<SessionPiece[]>([]);
  const [pendingTrapResolutions, setPendingTrapResolutions] = React.useState<
    TrapResolution[]
  >([]);
  const [error, setError] = React.useState<string | null>(null);
  const [isLoading, setIsLoading] = React.useState(true);

  const refresh = React.useCallback(
    async (showLoading = true) => {
      if (showLoading) setIsLoading(true);
      try {
        const [
          pendingRequests,
          activeParticipants,
          sessionCharacters,
          sessionPieces,
          pendingTraps,
        ] = await Promise.all([
          fetchDmJoinRequests(sessionId, dmToken),
          fetchDmParticipants(sessionId, dmToken),
          fetchDmCharacters(sessionId, dmToken),
          fetchDmPieces(sessionId, dmToken),
          fetchDmPendingTrapResolutions(sessionId, dmToken),
        ]);
        setRequests(pendingRequests);
        setParticipants(activeParticipants);
        setCharacters(sessionCharacters);
        setPieces(sessionPieces);
        setPendingTrapResolutions(pendingTraps);
        setError(null);
      } catch (refreshError) {
        setError(
          refreshError instanceof Error
            ? refreshError.message
            : "Impossibile aggiornare la console DM.",
        );
      } finally {
        if (showLoading) setIsLoading(false);
      }
    },
    [dmToken, sessionId],
  );

  const refreshFromLiveEvent = React.useCallback(
    () => refresh(false),
    [refresh],
  );
  const liveState = useLiveSessionRefresh({
    url: `/api/v1/dm/sessions/${encodeURIComponent(sessionId)}/events/stream`,
    token: dmToken,
    refresh: refreshFromLiveEvent,
  });

  React.useEffect(() => {
    void refresh();
  }, [refresh]);

  return {
    requests,
    participants,
    characters,
    pieces,
    pendingTrapResolutions,
    error,
    isLoading,
    liveState,
    setError,
    refresh,
  };
}
