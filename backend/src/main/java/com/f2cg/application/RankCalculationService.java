package com.f2cg.application;

import com.f2cg.domain.season.PlayerRank;
import com.f2cg.domain.season.Season;
import com.f2cg.domain.season.SeasonPhase;
import com.f2cg.eventbus.AppEventType;
import com.f2cg.eventbus.EventBus;
import com.f2cg.infrastructure.r2dbc.PlayerSeasonStatsEntity;
import com.f2cg.infrastructure.r2dbc.PlayerSeasonStatsRepository;
import com.f2cg.infrastructure.r2dbc.SeasonRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class RankCalculationService {

    private final PlayerSeasonStatsRepository playerSeasonStatsRepository;
    private final SeasonRepository seasonRepository;
    private final SeasonService seasonService;
    private final EventBus eventBus;

    public RankCalculationService(PlayerSeasonStatsRepository playerSeasonStatsRepository,
                                  SeasonRepository seasonRepository,
                                  SeasonService seasonService,
                                  EventBus eventBus) {
        this.playerSeasonStatsRepository = playerSeasonStatsRepository;
        this.seasonRepository = seasonRepository;
        this.seasonService = seasonService;
        this.eventBus = eventBus;
    }

    public boolean isWeeklyCalculationDue(Season season) {
        if (season.lastWeeklyCalculation() == null) {
            return true;
        }
        LocalDate thisMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return season.lastWeeklyCalculation().isBefore(thisMonday);
    }

    public Mono<Void> calculateRanksIfDue(Season season) {
        if (!isWeeklyCalculationDue(season)) {
            return Mono.empty();
        }
        return calculateRanks(season);
    }

    public Mono<Void> calculateRanks(Season season) {
        SeasonPhase phase = seasonService.getCurrentPhase(season, LocalDate.now());

        Mono<Void> core = playerSeasonStatsRepository.findBySeasonId(season.id())
                .collectList()
                .flatMap(statsList -> {
                    List<PlayerSeasonStatsEntity> updated = phase == SeasonPhase.FREE
                            ? applyFreePhaseRanks(statsList, season)
                            : applyRankedPhaseRanks(statsList);

                    updated.forEach(s -> s.setMatchesThisWeek(0));

                    return playerSeasonStatsRepository.saveAll(updated)
                            .then(updateSeasonLastCalculation(season));
                });

        return eventBus.timed(AppEventType.RANK_CALCULATION_TIMED, null, season.id(), "RANK", core);
    }

    public PlayerRank getMatchmakingRank(PlayerRank rank, SeasonPhase phase) {
        if (phase == SeasonPhase.FREE) {
            return null;
        }
        return rank == PlayerRank.PENDING ? PlayerRank.ROOKIE : rank;
    }

    // --- private helpers ---

    private List<PlayerSeasonStatsEntity> applyFreePhaseRanks(
            List<PlayerSeasonStatsEntity> all, Season season) {

        boolean pastFirstWeek = LocalDate.now().isAfter(season.startDate().plusDays(6));

        List<PlayerSeasonStatsEntity> eligible = all.stream()
                .filter(s -> s.getTotalMatches() >= 15 && pastFirstWeek)
                .sorted(Comparator.comparingDouble(this::winRate).reversed())
                .toList();

        assignDistribution(eligible, /* rankDownAllowed */ true);

        for (PlayerSeasonStatsEntity s : all) {
            if (s.getTotalMatches() < 15 || !pastFirstWeek) {
                s.setRank(PlayerRank.PENDING.name());
            }
        }

        return all;
    }

    private List<PlayerSeasonStatsEntity> applyRankedPhaseRanks(
            List<PlayerSeasonStatsEntity> all) {

        List<PlayerSeasonStatsEntity> inactive = new ArrayList<>();
        List<PlayerSeasonStatsEntity> active = new ArrayList<>();

        for (PlayerSeasonStatsEntity s : all) {
            if (s.getMatchesThisWeek() < 15) {
                inactive.add(s);
            } else {
                active.add(s);
            }
        }

        // Inactivity demotion
        for (PlayerSeasonStatsEntity s : inactive) {
            PlayerRank current = rankOrRookie(s.getRank());
            PlayerRank demoted = demoteOne(current);
            s.setRank(demoted.name());
            s.setLastRankUpdate(LocalDateTime.now());
        }

        // Active players: distribution with rank-down blocked
        active.sort(Comparator.comparingDouble(this::winRate).reversed());
        assignDistribution(active, /* rankDownAllowed */ false);

        // Update highestRank for all
        for (PlayerSeasonStatsEntity s : all) {
            PlayerRank current = PlayerRank.valueOf(s.getRank());
            PlayerRank highest = PlayerRank.valueOf(s.getHighestRank());
            if (isHigher(current, highest)) {
                s.setHighestRank(current.name());
            }
        }

        return all;
    }

    private void assignDistribution(List<PlayerSeasonStatsEntity> sorted, boolean rankDownAllowed) {
        int n = sorted.size();
        if (n == 0) return;

        int eliteCount      = (int) Math.ceil(n * 0.10);
        int advancedCount   = (int) Math.ceil(n * 0.30);
        int intermediateCount = (int) Math.ceil(n * 0.30);

        for (int i = 0; i < n; i++) {
            PlayerSeasonStatsEntity s = sorted.get(i);
            PlayerRank newRank;
            if (i < eliteCount) {
                newRank = PlayerRank.ELITE;
            } else if (i < eliteCount + advancedCount) {
                newRank = PlayerRank.ADVANCED;
            } else if (i < eliteCount + advancedCount + intermediateCount) {
                newRank = PlayerRank.INTERMEDIATE;
            } else {
                newRank = PlayerRank.ROOKIE;
            }

            if (!rankDownAllowed) {
                PlayerRank current = rankOrRookie(s.getRank());
                if (isHigher(current, newRank)) {
                    newRank = current;
                }
            }

            s.setRank(newRank.name());
            s.setLastRankUpdate(LocalDateTime.now());
        }
    }

    private PlayerRank demoteOne(PlayerRank rank) {
        return switch (rank) {
            case ELITE        -> PlayerRank.ADVANCED;
            case ADVANCED     -> PlayerRank.INTERMEDIATE;
            case INTERMEDIATE -> PlayerRank.ROOKIE;
            case ROOKIE, PENDING -> PlayerRank.ROOKIE;
        };
    }

    private PlayerRank rankOrRookie(String rankStr) {
        try {
            PlayerRank rank = PlayerRank.valueOf(rankStr);
            return rank == PlayerRank.PENDING ? PlayerRank.ROOKIE : rank;
        } catch (IllegalArgumentException e) {
            return PlayerRank.ROOKIE;
        }
    }

    private boolean isHigher(PlayerRank a, PlayerRank b) {
        return rankOrder(a) < rankOrder(b);
    }

    private int rankOrder(PlayerRank rank) {
        return switch (rank) {
            case ELITE        -> 0;
            case ADVANCED     -> 1;
            case INTERMEDIATE -> 2;
            case ROOKIE       -> 3;
            case PENDING      -> 4;
        };
    }

    private double winRate(PlayerSeasonStatsEntity s) {
        if (s.getTotalMatches() == 0) return 0.0;
        return (double) s.getVictories() / s.getTotalMatches();
    }

    private Mono<Void> updateSeasonLastCalculation(Season season) {
        return seasonRepository.findById(season.id())
                .flatMap(entity -> {
                    entity.setLastWeeklyCalculation(LocalDate.now());
                    return seasonRepository.save(entity);
                })
                .then();
    }
}