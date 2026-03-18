package com.f2cg.api.dto;

import com.f2cg.domain.card.*;

public record CardResponse(
        String id,
        String name,
        int manaCost,
        String cardType,
        String theme,
        String unitClass,
        Integer attack,
        Integer defense,
        String effectType,
        Integer effectValue
) {
    public static CardResponse from(Card card) {
        return switch (card) {
            case UnitCard u -> new CardResponse(
                    u.id(), u.name(), u.manaCost(), "UNIT", u.theme().name(),
                    u.unitClass().name(), u.attack(), u.defense(), null, null);
            case BuffCard b -> new CardResponse(
                    b.id(), b.name(), b.manaCost(), "BUFF", b.theme().name(),
                    null, null, null, b.effect().type().name(), b.effect().value());
            case DebuffCard d -> new CardResponse(
                    d.id(), d.name(), d.manaCost(), "DEBUFF", d.theme().name(),
                    null, null, null, d.effect().type().name(), d.effect().value());
        };
    }
}