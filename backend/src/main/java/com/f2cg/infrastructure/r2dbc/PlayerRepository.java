package com.f2cg.infrastructure.r2dbc;

import com.f2cg.domain.player.Player;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface PlayerRepository extends ReactiveCrudRepository<Player, String> {
    Mono<Player> findByUsername(String username);
}