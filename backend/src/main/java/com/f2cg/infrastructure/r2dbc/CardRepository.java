package com.f2cg.infrastructure.r2dbc;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface CardRepository extends ReactiveCrudRepository<CardEntity, String> {
    Flux<CardEntity> findByTheme(String theme);
}