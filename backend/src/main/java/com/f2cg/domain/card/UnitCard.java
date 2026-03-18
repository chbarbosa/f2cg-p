package com.f2cg.domain.card;

import com.f2cg.domain.deck.DeckTheme;

import java.util.List;

public record UnitCard(
        String id,
        String name,
        int manaCost,
        UnitClass unitClass,
        DeckTheme theme,
        int attack,
        int defense,
        List<Ability> abilities
) implements Card {}