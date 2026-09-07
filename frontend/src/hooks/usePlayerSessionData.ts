import * as React from "react";
import {
  fetchPlayerCharacters,
  fetchPlayerIdentity,
  fetchPlayerPieces,
} from "../api/boardhubApi";
import type { Participant, PlayerCharacter, SessionPiece } from "../types";
import { useLiveSessionRefresh } from "./useLiveSessionRefresh";
import type { LiveConnectionState } from "./useAuthenticatedEventStream";

type PlayerSessionData = {
  identity: Participant | null;
  characters: PlayerCharacter[];
  pieces: SessionPiece[];
  error: string | null;
  isLoading: boolean;
  liveState: LiveConnectionState;
  setCharacters: React.Dispatch<React.SetStateAction<PlayerCharacter[]>>;
  setPieces: React.Dispatch<React.SetStateAction<SessionPiece[]>>;
  setError: React.Dispatch<React.SetStateAction<string | null>>;
  refresh: () => Promise<void>;
};

export function usePlayerSessionData(
  sessionId: string,
  playerToken: string,
): PlayerSessionData {
  const [identity, setIdentity] = React.useState<Participant | null>(null);
  const [characters, setCharacters] = React.useState<PlayerCharacter[]>([]);
  const [pieces, setPieces] = React.useState<SessionPiece[]>([]);
  const [error, setError] = React.useState<string | null>(null);
  const [isLoading, setIsLoading] = React.useState(true);

  const refresh = React.useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [activeIdentity, ownedCharacters, ownedPieces] = await Promise.all([
        fetchPlayerIdentity(sessionId, playerToken),
        fetchPlayerCharacters(sessionId, playerToken),
        fetchPlayerPieces(sessionId, playerToken),
      ]);
      setIdentity(activeIdentity);
      setCharacters(ownedCharacters);
      setPieces(ownedPieces);
    } catch (refreshError) {
      setError(
        refreshError instanceof Error
          ? refreshError.message
          : "Impossibile caricare il profilo del giocatore.",
      );
    } finally {
      setIsLoading(false);
    }
  }, [playerToken, sessionId]);

  const liveState = useLiveSessionRefresh({
    url: `/api/v1/player/sessions/${encodeURIComponent(sessionId)}/events/stream`,
    token: playerToken,
    refresh,
  });

  React.useEffect(() => {
    void refresh();
  }, [refresh]);

  return {
    identity,
    characters,
    pieces,
    error,
    isLoading,
    liveState,
    setCharacters,
    setPieces,
    setError,
    refresh,
  };
}
