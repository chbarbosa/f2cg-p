package com.f2cg.domain.card;

public sealed interface Card permits UnitCard, BuffCard, DebuffCard {
    String id();
    String name();
    int manaCost();
}