package com.f2cg.application;

import com.f2cg.domain.deck.DeckStatus;
import com.f2cg.domain.game.GameStatus;
import com.f2cg.domain.queue.QueueEntry;
import com.f2cg.domain.queue.QueueStatus;
import com.f2cg.domain.season.PlayerRank;
import com.f2cg.infrastructure.r2dbc.DeckRepository;
import com.f2cg.infrastructure.r2dbc.GameEntity;
import com.f2cg.infrastructure.r2dbc.GameRepository;
import com.f2cg.infrastructure.r2dbc.PlayerRepository;
import com.f2cg.infrastructure.r2dbc.PlayerSeasonStatsRepository;
import com.f2cg.infrastructure.r2dbc.QueueEntryEntity;
import com.f2cg.infrastructure.r2dbc.QueueEntryRepository;
import com.f2cg.infrastructure.sse.QueueSseBroadcaster;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class QueueService {

    private final QueueEntryRepository queueEntryRepository;
    private final DeckRepository deckRepository;
    private final SeasonService seasonService;
    private final RankCalculationService rankCalculationService;
    private final PlayerSeasonStatsRepository playerSeasonStatsRepository;
    private final GameRepository gameRepository;
    private final PlayerRepository playerRepository;
    private final QueueSseBroadcaster sseBroadcaster;

    @Value("${game.queue.timeout-seconds}")
    private int timeoutSeconds;

    public QueueService(QueueEntryRepository queueEntryRepository,
                        DeckRepository deckRepository,
                        SeasonService seasonService,
                        RankCalculationService rankCalculationService,
                        PlayerSeasonStatsRepository playerSeasonStatsRepository,
                        GameRepository gameRepository,
                        PlayerRepository playerRepository,
                        QueueSseBroadcaster sseBroadcaster) {
        this.queueEntryRepository = queueEntryRepository;
        this.deckRepository = deckRepository;
        this.seasonService = seasonService;
        this.rankCalculationService = rankCalculationService;
        this.playerSeasonStatsRepository = playerSeasonStatsRepository;
        this.gameRepository = gameRepository;
        this.playerRepository = playerRepository;
        this.sseBroadcaster = sseBroadcaster;
    }

    public Mono<QueueEntry> joinQueue(String playerId, String deckId) {
        return playerRepository.findById(playerId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found")))
                .flatMap(player -> {
                    if (player.getNickname() == null || player.getNickname().isBlank()) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "A nickname is required to play"));
                    }
                    return joinQueueInternal(playerId, deckId);
                });
    }

    private Mono<QueueEntry> joinQueueInternal(String playerId, String deckId) {
        return deckRepository.findById(deckId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Deck not found")))
                .flatMap(deck -> {
                    if (!deck.getPlayerId().equals(playerId)) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));
                    }
                    if (DeckStatus.DRAFT.name().equals(deck.getStatus())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Deck must be PLAYABLE to join queue"));
                    }
                    return seasonService.getCurrentSeason()
                            .flatMap(season -> {
                                var phase = seasonService.getCurrentPhase(season, LocalDate.now());
                                return playerSeasonStatsRepository
                                        .findByPlayerIdAndSeasonId(playerId, season.id())
                                        .map(stats -> PlayerRank.valueOf(stats.getRank()))
                                        .defaultIfEmpty(PlayerRank.PENDING)
                                        .map(rank -> Optional.ofNullable(
                                                rankCalculationService.getMatchmakingRank(rank, phase)));
                            })
                            .flatMap(matchmakingRankOpt -> {
                                PlayerRank matchmakingRank = matchmakingRankOpt.orElse(null);
                                return queueEntryRepository
                                        .findByPlayerIdAndStatus(playerId, QueueStatus.WAITING.name())
                                        .flatMap(existing -> Mono.<QueueEntryEntity>error(
                                                new ResponseStatusException(HttpStatus.CONFLICT, "Already in queue")))
                                        .switchIfEmpty(Mono.defer(() -> {
                                            QueueEntryEntity entity = new QueueEntryEntity(
                                                    UUID.randomUUID().toString(),
                                                    playerId,
                                                    deckId,
                                                    QueueStatus.WAITING.name(),
                                                    matchmakingRank != null ? matchmakingRank.name() : null,
                                                    LocalDateTime.now()
                                            );
                                            return queueEntryRepository.save(entity);
                                        }))
                                        .flatMap(saved -> {
                                            saved.markPersisted();
                                            return tryMatchmaking(saved, matchmakingRank).thenReturn(saved);
                                        });
                            });
                })
                .map(this::toDomain);
    }

    private Mono<Void> tryMatchmaking(QueueEntryEntity joiner, PlayerRank matchmakingRank) {
        Mono<QueueEntryEntity> opponentSearch = matchmakingRank != null
                ? queueEntryRepository.findFirstEligibleOpponentWithRank(joiner.getPlayerId(), matchmakingRank.name())
                : queueEntryRepository.findFirstEligibleOpponentNullRank(joiner.getPlayerId());

        return opponentSearch
                .flatMap(opponent -> queueEntryRepository.claimForMatch(opponent.getId())
                        .filter(n -> n > 0)
                        .flatMap(__ -> playerRepository.findById(joiner.getPlayerId())
                                .zipWith(playerRepository.findById(opponent.getPlayerId()))
                                .flatMap(players -> {
                                    var p1 = players.getT1();
                                    var p2 = players.getT2();
                                    String p1Username = p1.getNickname() != null ? p1.getNickname() : p1.getUsername();
                                    String p2Username = p2.getNickname() != null ? p2.getNickname() : p2.getUsername();

                                    String gamePublicId = UUID.randomUUID().toString();
                                    GameEntity game = new GameEntity(
                                            gamePublicId,
                                            p1.getId(), p1.getId(), p1Username,
                                            p2.getId(), p2.getId(), p2Username,
                                            GameStatus.WAITING_START.name(),
                                            LocalDateTime.now()
                                    );
                                    return gameRepository.save(game)
                                            .flatMap(savedGame -> {
                                                joiner.setStatus(QueueStatus.MATCHED.name());
                                                return queueEntryRepository.save(joiner)
                                                        .doOnSuccess(ignored -> {
                                                            Map<String, String> matchFoundPayload1 = Map.of(
                                                                    "gamePublicId", gamePublicId,
                                                                    "opponentUsername", p2Username
                                                            );
                                                            Map<String, String> matchFoundPayload2 = Map.of(
                                                                    "gamePublicId", gamePublicId,
                                                                    "opponentUsername", p1Username
                                                            );
                                                            sseBroadcaster.emit(p1.getId(), "MATCH_FOUND", matchFoundPayload1);
                                                            sseBroadcaster.emit(p2.getId(), "MATCH_FOUND", matchFoundPayload2);
                                                            sseBroadcaster.complete(p1.getId());
                                                            sseBroadcaster.complete(p2.getId());
                                                        });
                                            });
                                })))
                .then();
    }

    @Scheduled(fixedDelay = 10_000)
    public void checkTimeouts() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(timeoutSeconds);
        queueEntryRepository.findTimedOutEntries(cutoff)
                .flatMap(entry -> {
                    entry.setStatus(QueueStatus.TIMED_OUT.name());
                    return queueEntryRepository.save(entry)
                            .doOnSuccess(saved -> sseBroadcaster.emit(
                                    saved.getPlayerId(),
                                    "QUEUE_TIMEOUT",
                                    Map.of("message", "No opponent found. Please try again.")
                            ))
                            .doOnSuccess(saved -> sseBroadcaster.complete(saved.getPlayerId()));
                })
                .subscribe();
    }

    public Mono<Void> cancelQueue(String playerId) {
        return queueEntryRepository.cancelIfWaiting(playerId)
                .flatMap(updated -> updated == 0
                        ? Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "No active queue entry"))
                        : Mono.empty());
    }

    public Mono<QueueEntry> getStatus(String playerId) {
        return queueEntryRepository
                .findByPlayerIdAndStatusIn(playerId,
                        List.of(QueueStatus.WAITING.name(), QueueStatus.MATCHED.name()))
                .next()
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No active queue entry")))
                .map(this::toDomain);
    }

    private QueueEntry toDomain(QueueEntryEntity entity) {
        PlayerRank matchmakingRank = entity.getMatchmakingRank() != null
                ? PlayerRank.valueOf(entity.getMatchmakingRank())
                : null;
        return new QueueEntry(
                entity.getId(),
                entity.getPlayerId(),
                entity.getDeckId(),
                matchmakingRank,
                QueueStatus.valueOf(entity.getStatus()),
                entity.getJoinedAt()
        );
    }
}