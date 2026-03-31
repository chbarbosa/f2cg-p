package com.f2cg.domain.game;

import java.time.LocalDateTime;

public record Game(
        Long id,
        String publicId,
        String player1Id,
        String player1PublicId,
        String player1Username,
        String player2Id,
        String player2PublicId,
        String player2Username,
        GameStatus status,
        LocalDateTime createdAt
) {}