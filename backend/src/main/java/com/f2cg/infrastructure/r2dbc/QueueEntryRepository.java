package com.f2cg.infrastructure.r2dbc;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface QueueEntryRepository extends ReactiveCrudRepository<QueueEntryEntity, String> {
    Mono<QueueEntryEntity> findByPlayerIdAndStatus(String playerId, String status);
    Flux<QueueEntryEntity> findByPlayerIdAndStatusIn(String playerId, List<String> statuses);
}