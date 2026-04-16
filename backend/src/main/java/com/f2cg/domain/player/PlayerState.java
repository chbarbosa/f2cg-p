package com.f2cg.domain.player;

import com.f2cg.domain.card.Card;
import com.f2cg.domain.game.FieldUnit;

import java.util.List;

public record PlayerState(
        String playerId,
        String username,
        List<FieldUnit> field,
        List<Card> hand,
        List<Card> stack,
        List<Card> graveyard,
        boolean summoningConfirmed
) {}