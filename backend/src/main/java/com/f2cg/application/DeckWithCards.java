package com.f2cg.application;

import com.f2cg.domain.card.Card;
import com.f2cg.domain.deck.Deck;

import java.util.List;

public record DeckWithCards(Deck deck, List<Card> cards) {}