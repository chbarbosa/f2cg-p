package com.f2cg.infrastructure.r2dbc;

import com.f2cg.domain.card.*;
import com.f2cg.domain.deck.DeckTheme;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CardEntityMapper {

    public Card toDomain(CardEntity e) {
        DeckTheme theme = DeckTheme.valueOf(e.getTheme());
        return switch (e.getCardType()) {
            case "UNIT" -> new UnitCard(
                    e.getId(),
                    e.getName(),
                    e.getManaCost(),
                    UnitClass.valueOf(e.getUnitClass()),
                    theme,
                    e.getAttack(),
                    e.getDefense(),
                    List.of()
            );
            case "BUFF" -> new BuffCard(
                    e.getId(),
                    e.getName(),
                    e.getManaCost(),
                    theme,
                    new Effect(EffectType.valueOf(e.getEffectType()), e.getEffectValue())
            );
            case "DEBUFF" -> new DebuffCard(
                    e.getId(),
                    e.getName(),
                    e.getManaCost(),
                    theme,
                    new Effect(EffectType.valueOf(e.getEffectType()), e.getEffectValue())
            );
            default -> throw new IllegalStateException("Unknown card type: " + e.getCardType());
        };
    }
}