package com.f2cg.domain.card;

import com.f2cg.domain.deck.DeckTheme;

public sealed interface Card permits UnitCard, BuffCard, DebuffCard {
    String id();
    String name();
    int manaCost();
    DeckTheme theme();
}