package com.f2cg.infrastructure.r2dbc;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface GameRepository extends ReactiveCrudRepository<GameEntity, Long> {
    Mono<GameEntity> findByPublicId(String publicId);

    @Query("SELECT * FROM games WHERE (player1_id = :playerId OR player2_id = :playerId) AND status = 'WAITING_START' ORDER BY created_at DESC LIMIT 1")
    Mono<GameEntity> findLatestWaitingByPlayerId(String playerId);
}