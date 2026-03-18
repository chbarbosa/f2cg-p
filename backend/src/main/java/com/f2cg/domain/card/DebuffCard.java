package com.f2cg.domain.card;

import com.f2cg.domain.deck.DeckTheme;

public record DebuffCard(
        String id,
        String name,
        int manaCost,
        DeckTheme theme,
        Effect effect
) implements Card {}