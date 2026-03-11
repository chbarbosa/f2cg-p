package com.f2cg.domain.card;

public record Ability(
        String id,
        String name,
        AbilityType type,
        Effect effect,
        String description
) {}