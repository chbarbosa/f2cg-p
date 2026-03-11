package com.f2cg.domain.card;

import java.util.List;

public record UnitCard(
        String id,
        String name,
        int manaCost,
        UnitClass unitClass,
        String theme,
        int attack,
        int defense,
        List<Ability> abilities
) implements Card {}