package com.f2cg.application;

import com.f2cg.domain.card.Card;
import com.f2cg.domain.game.FieldUnit;
import com.f2cg.domain.game.GamePhase;

import java.util.List;

public record PlayerGameStateView(
        String gameId,
        int turnNumber,
        int currentMana,
        GamePhase phase,
        String activePlayerId,
        MyView me,
        OpponentView opponent
) {
    public record MyView(
            String playerId,
            String username,
            List<Card> hand,
            int stackSize,
            List<FieldUnit> field,
            List<Card> graveyard,
            boolean summoningConfirmed
    ) {}

    public record OpponentView(
            String playerId,
            String username,
            int handSize,
            int stackSize,
            List<FieldUnit> field,
            List<Card> graveyard,
            boolean summoningConfirmed
    ) {}
}