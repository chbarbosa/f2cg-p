import React, { useEffect, useState } from 'react';
import { useDeckStore } from '../store/deckStore';
import type { DeckResponse } from '../api/types';

interface Props {
  onReady: (deckId: string) => void;
  onBack: () => void;
}

export function DeckSelector({ onReady, onBack }: Props) {
  const { decks, loadingDecks, fetchDecks } = useDeckStore();
  const [selectedId, setSelectedId] = useState<string | null>(null);

  useEffect(() => {
    fetchDecks();
  }, [fetchDecks]);

  const playableDecks = decks.filter((d: DeckResponse) => d.status === 'PLAYABLE');

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <button style={styles.backBtn} onClick={onBack}>← Back</button>
        <h2 style={styles.title}>Choose Your Deck</h2>
      </div>

      {loadingDecks && <p style={styles.muted}>Loading decks…</p>}

      {!loadingDecks && playableDecks.length === 0 && (
        <p style={styles.muted}>No playable decks available. Build a full deck of 20 cards first.</p>
      )}

      <div style={styles.list}>
        {playableDecks.map(deck => (
          <div
            key={deck.id}
            style={selectedId === deck.id ? { ...styles.card, ...styles.cardSelected } : styles.card}
            onClick={() => setSelectedId(deck.id)}
            role="button"
            aria-pressed={selectedId === deck.id}
          >
            <span style={styles.deckName}>{deck.name}</span>
            <span style={styles.theme}>{deck.theme}</span>
            <span style={styles.badge}>{deck.cardIds.length}/20</span>
          </div>
        ))}
      </div>

      <button
        style={selectedId ? styles.readyBtn : { ...styles.readyBtn, opacity: 0.4, cursor: 'not-allowed' }}
        disabled={!selectedId}
        onClick={() => selectedId && onReady(selectedId)}
      >
        Ready
      </button>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    width: '100%',
    maxWidth: 560,
    margin: '0 auto',
    padding: '1.5rem',
    display: 'flex',
    flexDirection: 'column',
    gap: '1rem',
  },
  header: {
    display: 'flex',
    alignItems: 'center',
    gap: '1rem',
    marginBottom: '0.5rem',
  },
  title: { margin: 0, color: '#cdd6f4', fontSize: '1.4rem' },
  backBtn: {
    padding: '0.4rem 0.8rem',
    borderRadius: 6,
    border: 'none',
    background: '#313244',
    color: '#cdd6f4',
    cursor: 'pointer',
    fontSize: '0.9rem',
  },
  list: { display: 'flex', flexDirection: 'column', gap: '0.6rem' },
  card: {
    background: '#1e1e2e',
    border: '2px solid #313244',
    borderRadius: 8,
    padding: '0.85rem 1rem',
    display: 'flex',
    alignItems: 'center',
    gap: '1rem',
    cursor: 'pointer',
    transition: 'border-color 0.15s',
  },
  cardSelected: {
    borderColor: '#89b4fa',
  },
  deckName: { color: '#cdd6f4', fontWeight: 600, flex: 1 },
  theme: { color: '#89b4fa', fontSize: '0.85rem' },
  badge: {
    background: '#a6e3a1',
    color: '#1e1e2e',
    borderRadius: 4,
    padding: '2px 8px',
    fontSize: '0.75rem',
    fontWeight: 700,
  },
  readyBtn: {
    marginTop: '0.5rem',
    padding: '0.65rem 2rem',
    borderRadius: 6,
    border: 'none',
    background: '#a6e3a1',
    color: '#1e1e2e',
    fontWeight: 700,
    fontSize: '1rem',
    cursor: 'pointer',
    alignSelf: 'center',
  },
  muted: { color: '#a6adc8', fontSize: '0.9rem' },
};