package com.f2cg.domain.game;

import com.f2cg.domain.player.PlayerState;
import org.springframework.lang.Nullable;

public record GameState(
        String gameId,
        int turnNumber,
        int currentMana,
        GamePhase phase,
        String activePlayerId,
        SummoningState summoning,
        PlayerState player1,
        PlayerState player2,
        @Nullable String winnerId
) {}