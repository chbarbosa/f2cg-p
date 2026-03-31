package com.f2cg.infrastructure.r2dbc;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface GameRepository extends ReactiveCrudRepository<GameEntity, Long> {
    Mono<GameEntity> findByPublicId(String publicId);
}