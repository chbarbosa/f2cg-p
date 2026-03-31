package com.f2cg.api.dto;

import com.f2cg.domain.game.GameStatus;
import com.f2cg.infrastructure.r2dbc.GameEntity;

public record GameResponse(
        String publicId,
        String player1Username,
        String player2Username,
        GameStatus status
) {
    public static GameResponse from(GameEntity entity) {
        return new GameResponse(
                entity.getPublicId(),
                entity.getPlayer1Username(),
                entity.getPlayer2Username(),
                GameStatus.valueOf(entity.getStatus())
        );
    }
}