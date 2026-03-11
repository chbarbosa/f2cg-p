package com.f2cg.domain.card;

public record BuffCard(
        String id,
        String name,
        int manaCost,
        Effect effect
) implements Card {}