package com.f2cg.domain.card;

public record DebuffCard(
        String id,
        String name,
        int manaCost,
        Effect effect
) implements Card {}