package com.f2cg.infrastructure.r2dbc;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface SeasonRepository extends ReactiveCrudRepository<SeasonEntity, String> {
    Mono<SeasonEntity> findByStatus(String status);
}