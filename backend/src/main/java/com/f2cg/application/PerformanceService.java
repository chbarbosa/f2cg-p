package com.f2cg.application;

import com.f2cg.api.dto.PerformanceResponse;
import com.f2cg.api.dto.SeasonSummaryResponse;
import com.f2cg.domain.season.PlayerRank;
import com.f2cg.domain.season.PlayerSeasonStats;
import com.f2cg.domain.season.Season;
import com.f2cg.infrastructure.r2dbc.PlayerSeasonStatsEntity;
import com.f2cg.infrastructure.r2dbc.PlayerSeasonStatsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Comparator;

@Service
public class PerformanceService {

    private final SeasonService seasonService;
    private final PlayerSeasonStatsRepository playerSeasonStatsRepository;

    public PerformanceService(SeasonService seasonService,
                              PlayerSeasonStatsRepository playerSeasonStatsRepository) {
        this.seasonService = seasonService;
        this.playerSeasonStatsRepository = playerSeasonStatsRepository;
    }

    public Mono<PerformanceResponse> getCurrentPerformance(String playerId) {
        return seasonService.getCurrentSeason()
                .flatMap(season ->
                        playerSeasonStatsRepository
                                .findByPlayerIdAndSeasonId(playerId, season.id())
                                .map(this::toDomainStats)
                                .defaultIfEmpty(defaultStats(playerId, season.id()))
                                .map(stats -> {
                                    var phase = seasonService.getCurrentPhase(season, LocalDate.now());
                                    return PerformanceResponse.from(season, stats, phase);
                                })
                );
    }

    public Mono<PerformanceResponse> getSeasonPerformance(String playerId, String seasonId) {
        return seasonService.getSeasonById(seasonId)
                .flatMap(season ->
                        playerSeasonStatsRepository
                                .findByPlayerIdAndSeasonId(playerId, seasonId)
                                .switchIfEmpty(Mono.error(
                                        new ResponseStatusException(HttpStatus.NOT_FOUND,
                                                "Player did not participate in this season")))
                                .map(statsEntity -> PerformanceResponse.from(season, toDomainStats(statsEntity), null))
                );
    }

    public Flux<SeasonSummaryResponse> getParticipatedSeasons(String playerId) {
        return seasonService.getSeasonsByPlayer(playerId)
                .sort(Comparator.comparingInt(Season::year)
                        .thenComparingInt(Season::seasonNumber)
                        .reversed())
                .map(SeasonSummaryResponse::from);
    }

    private PlayerSeasonStats defaultStats(String playerId, String seasonId) {
        return new PlayerSeasonStats(
                null, playerId, seasonId,
                0, 0, 0,
                PlayerRank.PENDING, PlayerRank.PENDING,
                0, null
        );
    }

    private PlayerSeasonStats toDomainStats(PlayerSeasonStatsEntity e) {
        return new PlayerSeasonStats(
                e.getId(),
                e.getPlayerId(),
                e.getSeasonId(),
                e.getTotalMatches(),
                e.getVictories(),
                e.getDefeats(),
                PlayerRank.valueOf(e.getRank()),
                PlayerRank.valueOf(e.getHighestRank()),
                e.getMatchesThisWeek(),
                e.getLastRankUpdate()
        );
    }
}