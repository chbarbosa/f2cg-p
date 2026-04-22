import { useEffect, useRef, useState } from 'react';

const MAX_RETRIES = 3;
const RETRY_DELAY_MS = 2000;

interface SSEOptions {
  onOpen?: () => void;
  onError?: () => void;
  onClose?: () => void;
}

export function useSSE(
  url: string | null,
  eventHandlers: Record<string, (data: unknown) => void>,
  options: SSEOptions = {}
): { isReconnecting: boolean } {
  const [isReconnecting, setIsReconnecting] = useState(false);
  const retriesRef = useRef(0);
  const esRef = useRef<EventSource | null>(null);
  const optionsRef = useRef(options);
  const handlersRef = useRef(eventHandlers);

  useEffect(() => {
    optionsRef.current = options;
  });
  useEffect(() => {
    handlersRef.current = eventHandlers;
  });

  useEffect(() => {
    if (!url) return;

    let cancelled = false;

    function connect() {
      if (cancelled) return;

      const es = new EventSource(url as string);
      esRef.current = es;

      es.onopen = () => {
        if (cancelled) { es.close(); return; }
        retriesRef.current = 0;
        setIsReconnecting(false);
        optionsRef.current.onOpen?.();
      };

      for (const [eventName, handler] of Object.entries(handlersRef.current)) {
        es.addEventListener(eventName, (e: MessageEvent) => {
          if (cancelled) return;
          try {
            handler(JSON.parse(e.data));
          } catch {
            handler(e.data);
          }
        });
      }

      es.onerror = () => {
        es.close();
        esRef.current = null;
        if (cancelled) return;

        if (retriesRef.current < MAX_RETRIES) {
          retriesRef.current += 1;
          setIsReconnecting(true);
          setTimeout(connect, RETRY_DELAY_MS);
        } else {
          setIsReconnecting(false);
          optionsRef.current.onError?.();
        }
      };
    }

    connect();

    return () => {
      cancelled = true;
      esRef.current?.close();
      esRef.current = null;
      optionsRef.current.onClose?.();
    };
  }, [url]);

  return { isReconnecting };
}