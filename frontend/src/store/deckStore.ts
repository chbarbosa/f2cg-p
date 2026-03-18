import { create } from 'zustand';
import { getCardsByTheme } from '../api/cards';
import { listDecks, getDeck, createDeck, updateDeck, deleteDeck } from '../api/decks';
import type { CardResponse, DeckResponse, DeckTheme } from '../api/types';

interface DeckBuilderState {
  // deck list
  decks: DeckResponse[];
  loadingDecks: boolean;
  decksError: string | null;

  // card pool
  availableCards: CardResponse[];
  loadingCards: boolean;

  // builder
  editingDeckId: string | null;
  deckName: string;
  selectedTheme: DeckTheme | null;
  selectedCardIds: string[];
  themeIsLocked: boolean;
  isDirty: boolean;
  saving: boolean;
  saveError: string | null;

  // actions
  fetchDecks: () => Promise<void>;
  fetchCards: (theme: DeckTheme) => Promise<void>;
  selectTheme: (theme: DeckTheme) => void;
  toggleCard: (cardId: string) => void;
  resetBuilder: () => void;
  setDeckName: (name: string) => void;
  saveDeck: () => Promise<void>;
  deletePlayerDeck: (id: string) => Promise<void>;
  loadDeckForEdit: (id: string) => Promise<void>;
  initNewDeck: () => void;
}

export const useDeckStore = create<DeckBuilderState>((set, get) => ({
  decks: [],
  loadingDecks: false,
  decksError: null,

  availableCards: [],
  loadingCards: false,

  editingDeckId: null,
  deckName: '',
  selectedTheme: null,
  selectedCardIds: [],
  themeIsLocked: false,
  isDirty: false,
  saving: false,
  saveError: null,

  fetchDecks: async () => {
    set({ loadingDecks: true, decksError: null });
    try {
      const decks = await listDecks();
      set({ decks, loadingDecks: false });
    } catch (e) {
      set({ decksError: (e as Error).message, loadingDecks: false });
    }
  },

  fetchCards: async (theme: DeckTheme) => {
    set({ loadingCards: true, availableCards: [] });
    try {
      const cards = await getCardsByTheme(theme);
      set({ availableCards: cards, loadingCards: false });
    } catch {
      set({ loadingCards: false });
    }
  },

  selectTheme: (theme: DeckTheme) => {
    const { themeIsLocked, selectedTheme, fetchCards } = get();
    if (themeIsLocked) return;
    if (selectedTheme !== theme) {
      set({ selectedTheme: theme, isDirty: true });
      fetchCards(theme);
    }
  },

  toggleCard: (cardId: string) => {
    const { selectedCardIds } = get();
    if (selectedCardIds.includes(cardId)) {
      const updated = selectedCardIds.filter(id => id !== cardId);
      set({
        selectedCardIds: updated,
        themeIsLocked: updated.length > 0,
        isDirty: true,
      });
    } else {
      if (selectedCardIds.length >= 20) return;
      const updated = [...selectedCardIds, cardId];
      set({ selectedCardIds: updated, themeIsLocked: true, isDirty: true });
    }
  },

  resetBuilder: () => {
    set({ selectedCardIds: [], themeIsLocked: false, isDirty: true });
  },

  setDeckName: (name: string) => {
    set({ deckName: name, isDirty: true });
  },

  saveDeck: async () => {
    const { editingDeckId, deckName, selectedTheme, selectedCardIds } = get();
    if (!selectedTheme) return;

    set({ saving: true, saveError: null });
    try {
      const req = { name: deckName, theme: selectedTheme, cardIds: selectedCardIds };
      let saved: DeckResponse;
      if (editingDeckId) {
        saved = await updateDeck(editingDeckId, req);
        set(state => ({
          decks: state.decks.map(d => (d.id === saved.id ? saved : d)),
        }));
      } else {
        saved = await createDeck(req);
        set(state => ({ decks: [...state.decks, saved] }));
      }
      set({ saving: false, isDirty: false });
    } catch (e) {
      set({ saving: false, saveError: (e as Error).message });
    }
  },

  deletePlayerDeck: async (id: string) => {
    await deleteDeck(id);
    set(state => ({ decks: state.decks.filter(d => d.id !== id) }));
  },

  loadDeckForEdit: async (id: string) => {
    const { data } = await getDeck(id).then(d => ({ data: d }));
    set({
      editingDeckId: data.deck.id,
      deckName: data.deck.name,
      selectedTheme: data.deck.theme,
      selectedCardIds: data.deck.cardIds,
      themeIsLocked: data.deck.cardIds.length > 0,
      availableCards: data.cards,
      isDirty: false,
    });
  },

  initNewDeck: () => {
    set({
      editingDeckId: null,
      deckName: '',
      selectedTheme: null,
      selectedCardIds: [],
      themeIsLocked: false,
      availableCards: [],
      isDirty: false,
      saveError: null,
    });
  },
}));