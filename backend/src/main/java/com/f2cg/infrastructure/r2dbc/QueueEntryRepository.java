package com.f2cg.infrastructure.r2dbc;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

public interface QueueEntryRepository extends ReactiveCrudRepository<QueueEntryEntity, String> {

    Mono<QueueEntryEntity> findByPlayerIdAndStatus(String playerId, String status);

    Flux<QueueEntryEntity> findByPlayerIdAndStatusIn(String playerId, List<String> statuses);

    @Query("SELECT * FROM queue_entries " +
           "WHERE status = 'WAITING' " +
           "AND player_id != :excludePlayerId " +
           "AND matchmaking_rank = :rank " +
           "ORDER BY joined_at ASC " +
           "LIMIT 1")
    Mono<QueueEntryEntity> findFirstEligibleOpponentWithRank(String excludePlayerId, String rank);

    @Query("SELECT * FROM queue_entries " +
           "WHERE status = 'WAITING' " +
           "AND player_id != :excludePlayerId " +
           "AND matchmaking_rank IS NULL " +
           "ORDER BY joined_at ASC " +
           "LIMIT 1")
    Mono<QueueEntryEntity> findFirstEligibleOpponentNullRank(String excludePlayerId);

    @Query("SELECT * FROM queue_entries " +
           "WHERE status = 'WAITING' " +
           "AND joined_at < :cutoff")
    Flux<QueueEntryEntity> findTimedOutEntries(LocalDateTime cutoff);
}