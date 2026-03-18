package com.f2cg.infrastructure.r2dbc;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DeckRepository extends ReactiveCrudRepository<DeckEntity, String> {
    Flux<DeckEntity> findByPlayerId(String playerId);
    Mono<Long> countByPlayerId(String playerId);
}