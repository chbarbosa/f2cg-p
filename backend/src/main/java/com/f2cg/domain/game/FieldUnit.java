package com.f2cg.domain.game;

import com.f2cg.domain.card.BuffCard;
import com.f2cg.domain.card.DebuffCard;
import com.f2cg.domain.card.UnitCard;
import org.springframework.lang.Nullable;

public record FieldUnit(
        UnitCard card,
        int currentAttack,
        int currentDefense,
        @Nullable BuffCard activeBuff,
        @Nullable DebuffCard activeDebuff,
        boolean hasActed
) {}