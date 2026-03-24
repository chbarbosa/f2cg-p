package com.f2cg.infrastructure.r2dbc;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PlayerSeasonStatsRepository extends ReactiveCrudRepository<PlayerSeasonStatsEntity, String> {
    Flux<PlayerSeasonStatsEntity> findBySeasonId(String seasonId);
    Mono<PlayerSeasonStatsEntity> findByPlayerIdAndSeasonId(String playerId, String seasonId);
    Flux<PlayerSeasonStatsEntity> findByPlayerId(String playerId);
}