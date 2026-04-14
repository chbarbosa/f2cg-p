type EventMap = {
  USER_LOGOUT: void;
};

type Listener<T> = (payload: T) => void;

const listeners = new Map<keyof EventMap, Set<Listener<unknown>>>();

export const eventBus = {
  publish<K extends keyof EventMap>(event: K, payload: EventMap[K]): void {
    listeners.get(event)?.forEach(fn => fn(payload));
  },

  subscribe<K extends keyof EventMap>(event: K, listener: Listener<EventMap[K]>): () => void {
    if (!listeners.has(event)) {
      listeners.set(event, new Set());
    }
    listeners.get(event)!.add(listener as Listener<unknown>);
    return () => {
      listeners.get(event)!.delete(listener as Listener<unknown>);
    };
  },
};