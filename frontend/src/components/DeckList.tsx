import React, { useEffect, useState } from 'react';
import { useDeckStore } from '../store/deckStore';

interface Props {
  onBack: () => void;
  onNew: () => void;
  onEdit: (id: string) => void;
}

export function DeckList({ onBack, onNew, onEdit }: Props) {
  const { decks, loadingDecks, decksError, fetchDecks, deletePlayerDeck, initNewDeck, loadDeckForEdit } = useDeckStore();
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null);

  useEffect(() => {
    fetchDecks();
  }, [fetchDecks]);

  const handleDelete = async (id: string) => {
    await deletePlayerDeck(id);
    setConfirmDeleteId(null);
  };

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <div style={styles.titleRow}>
          <button style={styles.backBtn} onClick={onBack}>← Back</button>
          <h2 style={styles.title}>My Decks</h2>
        </div>
        <button
          style={decks.length >= 7 ? { ...styles.newBtn, opacity: 0.4, cursor: 'not-allowed' } : styles.newBtn}
          disabled={decks.length >= 7}
          title={decks.length >= 7 ? 'Deck limit reached (7/7)' : undefined}
          onClick={() => { initNewDeck(); onNew(); }}
        >
          + New Deck
        </button>
      </div>

      {loadingDecks && <p style={styles.muted}>Loading…</p>}
      {decksError && <p style={styles.error}>{decksError}</p>}

      {!loadingDecks && decks.length === 0 && (
        <p style={styles.muted}>No decks yet. Create your first one!</p>
      )}

      <div style={styles.list}>
        {decks.map(deck => (
          <div key={deck.id} style={styles.card} onClick={async () => { await loadDeckForEdit(deck.id); onEdit(deck.id); }}>
            <div style={styles.cardMain}>
              <span style={styles.deckName}>{deck.name}</span>
              <span style={styles.theme}>{deck.theme}</span>
              <span style={deck.status === 'PLAYABLE' ? styles.playable : styles.draft}>
                {deck.status}
              </span>
              <span style={styles.count}>{deck.cardIds.length}/20 cards</span>
            </div>
            <div style={styles.actions} onClick={e => e.stopPropagation()}>
              <button style={styles.deleteBtn} onClick={() => setConfirmDeleteId(deck.id)}>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="3 6 5 6 21 6" />
                  <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6" />
                  <path d="M10 11v6" />
                  <path d="M14 11v6" />
                  <path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2" />
                </svg>
              </button>
            </div>
          </div>
        ))}
      </div>
      {confirmDeleteId && (
        <div style={styles.modalOverlay} onClick={() => setConfirmDeleteId(null)}>
          <div style={styles.modalBox} onClick={e => e.stopPropagation()}>
            <p style={styles.modalTitle}>Delete deck?</p>
            <p style={styles.modalSub}>
              "{decks.find(d => d.id === confirmDeleteId)?.name}" will be permanently deleted.
            </p>
            <div style={styles.modalActions}>
              <button style={styles.cancelBtn} onClick={() => setConfirmDeleteId(null)}>Cancel</button>
              <button style={styles.confirmBtn} onClick={() => handleDelete(confirmDeleteId)}>Delete</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    width: '100%',
    maxWidth: 700,
    margin: '0 auto',
    padding: '1.5rem',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '1.5rem',
  },
  titleRow: { display: 'flex', alignItems: 'center', gap: '0.75rem' },
  title: { margin: 0, color: '#cdd6f4', fontSize: '1.4rem' },
  backBtn: {
    padding: '0.3rem 0.8rem',
    borderRadius: 6,
    border: 'none',
    background: '#313244',
    color: '#cdd6f4',
    cursor: 'pointer',
    fontSize: '0.9rem',
  },
  newBtn: {
    padding: '0.5rem 1.2rem',
    borderRadius: 6,
    border: 'none',
    background: '#89b4fa',
    color: '#1e1e2e',
    fontWeight: 700,
    cursor: 'pointer',
  },
  list: { display: 'flex', flexDirection: 'column', gap: '0.75rem' },
  card: {
    background: '#1e1e2e',
    border: '1px solid #313244',
    borderRadius: 8,
    padding: '0.75rem 1rem',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: '1rem',
    cursor: 'pointer',
  },
  cardMain: { display: 'flex', alignItems: 'center', gap: '1rem', flex: 1 },
  deckName: { color: '#cdd6f4', fontWeight: 600, minWidth: 120 },
  theme: { color: '#89b4fa', fontSize: '0.85rem' },
  playable: {
    background: '#a6e3a1',
    color: '#1e1e2e',
    borderRadius: 4,
    padding: '2px 8px',
    fontSize: '0.75rem',
    fontWeight: 700,
  },
  draft: {
    background: '#f9e2af',
    color: '#1e1e2e',
    borderRadius: 4,
    padding: '2px 8px',
    fontSize: '0.75rem',
    fontWeight: 700,
  },
  count: { color: '#a6adc8', fontSize: '0.85rem' },
  actions: { display: 'flex', gap: '0.5rem', alignItems: 'center' },
  deleteBtn: {
    width: 32,
    height: 32,
    borderRadius: 4,
    border: 'none',
    background: '#f38ba8',
    color: '#1e1e2e',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 0,
  },
  modalOverlay: {
    position: 'fixed',
    inset: 0,
    background: 'rgba(0,0,0,0.6)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 1000,
  },
  modalBox: {
    background: '#1e1e2e',
    border: '1px solid #45475a',
    borderRadius: 10,
    padding: '1.5rem',
    minWidth: 320,
    boxShadow: '0 8px 32px rgba(0,0,0,0.5)',
  },
  modalTitle: {
    margin: '0 0 0.5rem',
    color: '#cdd6f4',
    fontWeight: 700,
    fontSize: '1.1rem',
  },
  modalSub: {
    margin: '0 0 1.25rem',
    color: '#a6adc8',
    fontSize: '0.9rem',
  },
  modalActions: {
    display: 'flex',
    justifyContent: 'flex-end',
    gap: '0.5rem',
  },
  confirmBtn: {
    padding: '0.2rem 0.6rem',
    borderRadius: 4,
    border: 'none',
    background: '#f38ba8',
    color: '#1e1e2e',
    cursor: 'pointer',
    fontSize: '0.8rem',
    fontWeight: 700,
  },
  cancelBtn: {
    padding: '0.2rem 0.6rem',
    borderRadius: 4,
    border: 'none',
    background: '#313244',
    color: '#cdd6f4',
    cursor: 'pointer',
    fontSize: '0.8rem',
  },
  muted: { color: '#a6adc8', fontSize: '0.9rem' },
  error: { color: '#f38ba8', fontSize: '0.9rem' },
};