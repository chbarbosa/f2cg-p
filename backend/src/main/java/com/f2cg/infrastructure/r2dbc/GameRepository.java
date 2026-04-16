package com.f2cg.infrastructure.r2dbc;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

public interface GameRepository extends ReactiveCrudRepository<GameEntity, Long> {
    Mono<GameEntity> findByPublicId(String publicId);

    @Query("SELECT * FROM games WHERE (player1_id = :playerId OR player2_id = :playerId) AND status = 'WAITING_START' ORDER BY created_at DESC LIMIT 1")
    Mono<GameEntity> findLatestWaitingByPlayerId(String playerId);

    @Modifying
    @Query("UPDATE games SET status = 'CANCELLED' WHERE public_id = :publicId AND status = :expectedStatus")
    Mono<Integer> cancelGame(String publicId, String expectedStatus);

    @Modifying
    @Query("UPDATE games SET status = 'FINISHED', winner_id = :winnerId WHERE public_id = :publicId AND status = :expectedStatus")
    Mono<Integer> finishGame(String publicId, String winnerId, String expectedStatus);

    @Modifying
    @Query("UPDATE games SET status = 'IN_PROGRESS' WHERE public_id = :publicId AND status = 'WAITING_START'")
    Mono<Integer> transitionToInProgress(String publicId);

    @Modifying
    @Query("UPDATE games SET player1_heartbeat = :ts WHERE public_id = :publicId AND player1_id = :playerId")
    Mono<Integer> updatePlayer1Heartbeat(String publicId, String playerId, LocalDateTime ts);

    @Modifying
    @Query("UPDATE games SET player2_heartbeat = :ts WHERE public_id = :publicId AND player2_id = :playerId")
    Mono<Integer> updatePlayer2Heartbeat(String publicId, String playerId, LocalDateTime ts);

    @Modifying
    @Query("UPDATE games SET player1_hand = :p1Hand, player1_stack = :p1Stack, player2_hand = :p2Hand, player2_stack = :p2Stack WHERE public_id = :publicId")
    Mono<Integer> updateGameState(String publicId, String p1Hand, String p1Stack, String p2Hand, String p2Stack);
}