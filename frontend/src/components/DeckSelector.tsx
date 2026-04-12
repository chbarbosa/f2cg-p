import { useEffect, useState } from 'react';
import { useDeckStore } from '../store/deckStore';
import type { DeckResponse } from '../api/types';
import { PrimaryButton, SecondaryButton } from './ui';

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
    <div className="deck-selector">
      <div className="deck-selector-header">
        <SecondaryButton onClick={onBack}>← Back</SecondaryButton>
        <h2 className="section-title">Choose Your Deck</h2>
      </div>

      {loadingDecks && <p className="text-muted">Loading decks…</p>}

      {!loadingDecks && playableDecks.length === 0 && (
        <p className="text-muted">No playable decks available. Build a full deck of 20 cards first.</p>
      )}

      <div className="selector-rows">
        {playableDecks.map(deck => (
          <div
            key={deck.id}
            className={`selector-card${selectedId === deck.id ? ' selector-card--selected' : ''}`}
            onClick={() => setSelectedId(deck.id)}
            role="button"
            aria-pressed={selectedId === deck.id}
          >
            <span className="selector-card__name">{deck.name}</span>
            <span className="selector-card__theme">{deck.theme}</span>
            <span className="selector-card__badge">{deck.cardIds.length}/20</span>
          </div>
        ))}
      </div>

      <PrimaryButton
        disabled={!selectedId}
        onClick={() => selectedId && onReady(selectedId)}
      >
        Ready
      </PrimaryButton>
    </div>
  );
}