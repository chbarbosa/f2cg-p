package com.f2cg.domain.player;

import com.f2cg.domain.card.Card;
import com.f2cg.domain.game.FieldUnit;

import java.util.List;

public record PlayerState(
        String playerId,
        List<FieldUnit> field,
        List<Card> hand,
        List<Card> graveyard,
        int deckSize,
        boolean summoningConfirmed
) {}