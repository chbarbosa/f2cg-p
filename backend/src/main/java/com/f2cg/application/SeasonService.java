package com.f2cg.application;

import com.f2cg.domain.season.Season;
import com.f2cg.domain.season.SeasonPhase;
import com.f2cg.domain.season.SeasonStatus;
import com.f2cg.infrastructure.r2dbc.PlayerSeasonStatsRepository;
import com.f2cg.infrastructure.r2dbc.SeasonEntity;
import com.f2cg.infrastructure.r2dbc.SeasonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Service
public class SeasonService {

    private final SeasonRepository seasonRepository;
    private final PlayerSeasonStatsRepository playerSeasonStatsRepository;

    public SeasonService(SeasonRepository seasonRepository,
                         PlayerSeasonStatsRepository playerSeasonStatsRepository) {
        this.seasonRepository = seasonRepository;
        this.playerSeasonStatsRepository = playerSeasonStatsRepository;
    }

    public Mono<Season> getCurrentSeason() {
        return seasonRepository.findByStatus("ACTIVE")
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "No active season")))
                .map(this::toDomain);
    }

    public SeasonPhase getCurrentPhase(Season season, LocalDate today) {
        return today.isBefore(season.phase2StartDate()) ? SeasonPhase.FREE : SeasonPhase.RANKED;
    }

    public Flux<Season> getSeasonsByPlayer(String playerId) {
        return playerSeasonStatsRepository.findByPlayerId(playerId)
                .flatMap(stats -> seasonRepository.findById(stats.getSeasonId()))
                .map(this::toDomain);
    }

    private Season toDomain(SeasonEntity e) {
        return new Season(
                e.getId(),
                e.getYear(),
                e.getSeasonNumber(),
                e.getName(),
                e.getStartDate(),
                e.getEndDate(),
                e.getPhase2StartDate(),
                SeasonStatus.valueOf(e.getStatus()),
                e.getLastWeeklyCalculation()
        );
    }
}