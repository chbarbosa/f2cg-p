import { useEffect, useState } from 'react';
import { useAuthStore } from '../store/authStore';
import { MatchFoundPayload } from '../api/types';

interface QueueSSEState {
  matchFound: MatchFoundPayload | null;
  timedOut: boolean;
}

export function useQueueSSE(): QueueSSEState {
  const [state, setState] = useState<QueueSSEState>({ matchFound: null, timedOut: false });

  useEffect(() => {
    const token = useAuthStore.getState().token;
    if (!token) return;

    const url = `/api/queue/stream?token=${encodeURIComponent(token)}`;
    const es = new EventSource(url);

    es.addEventListener('MATCH_FOUND', (e: MessageEvent) => {
      try {
        const payload: MatchFoundPayload = JSON.parse(e.data);
        setState({ matchFound: payload, timedOut: false });
      } catch {
        // ignore malformed event
      }
      es.close();
    });

    es.addEventListener('QUEUE_TIMEOUT', () => {
      setState({ matchFound: null, timedOut: true });
      es.close();
    });

    return () => {
      es.close();
    };
  }, []);

  return state;
}