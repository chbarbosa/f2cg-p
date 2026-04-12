import { useEffect, useState } from 'react';
import { useDeckStore } from '../store/deckStore';
import { PrimaryButton, SecondaryButton, DangerButton } from './ui';

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
    <div className="deck-list">
      <div className="deck-list-header">
        <div className="deck-list-title-row">
          <SecondaryButton onClick={onBack}>← Back</SecondaryButton>
          <h2 className="section-title">My Decks</h2>
        </div>
        <PrimaryButton
          disabled={decks.length >= 7}
          onClick={() => { initNewDeck(); onNew(); }}
        >
          + New Deck
        </PrimaryButton>
      </div>

      {loadingDecks && <p className="text-muted">Loading…</p>}
      {decksError && <p className="text-error">{decksError}</p>}

      {!loadingDecks && decks.length === 0 && (
        <p className="text-muted">No decks yet. Create your first one!</p>
      )}

      <div className="deck-rows">
        {decks.map(deck => (
          <div key={deck.id} className="deck-row" onClick={async () => { await loadDeckForEdit(deck.id); onEdit(deck.id); }}>
            <div className="deck-row__main">
              <span className="deck-row__name">{deck.name}</span>
              <span className="deck-row__theme">{deck.theme}</span>
              <span className={`status-badge${deck.status === 'PLAYABLE' ? ' status-badge--playable' : ' status-badge--draft'}`}>
                {deck.status}
              </span>
              <span className="deck-row__count">{deck.cardIds.length}/20 cards</span>
            </div>
            <div className="deck-row__actions" onClick={e => e.stopPropagation()}>
              <button className="btn btn--danger btn--icon" onClick={() => setConfirmDeleteId(deck.id)}>
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
        <div className="modal-overlay" onClick={() => setConfirmDeleteId(null)}>
          <div className="modal-box" onClick={e => e.stopPropagation()}>
            <p className="modal-title">Delete deck?</p>
            <p className="modal-sub">
              "{decks.find(d => d.id === confirmDeleteId)?.name}" will be permanently deleted.
            </p>
            <div className="modal-actions">
              <SecondaryButton onClick={() => setConfirmDeleteId(null)}>Cancel</SecondaryButton>
              <DangerButton onClick={() => handleDelete(confirmDeleteId)}>Delete</DangerButton>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}