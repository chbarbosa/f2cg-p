package com.f2cg.application;

import com.f2cg.domain.game.GameStatus;
import com.f2cg.infrastructure.r2dbc.GameEntity;
import com.f2cg.infrastructure.r2dbc.GameRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
public class GameService {

    static final long DISCONNECT_SECONDS = 90;

    private final GameRepository gameRepository;

    public GameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    public Mono<Void> heartbeat(String publicId, String playerId) {
        return gameRepository.findByPublicId(publicId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found")))
                .flatMap(game -> {
                    if (isTerminal(game.getStatus())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.GONE, "Game is over"));
                    }
                    boolean isPlayer1 = playerId.equals(game.getPlayer1Id());
                    boolean isPlayer2 = playerId.equals(game.getPlayer2Id());
                    if (!isPlayer1 && !isPlayer2) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a participant"));
                    }
                    LocalDateTime now = LocalDateTime.now();
                    Mono<Integer> updateHeartbeat = isPlayer1
                            ? gameRepository.updatePlayer1Heartbeat(publicId, playerId, now)
                            : gameRepository.updatePlayer2Heartbeat(publicId, playerId, now);

                    return updateHeartbeat.then(gameRepository.findByPublicId(publicId))
                            .flatMap(updated -> checkOpponent(updated, isPlayer1));
                });
    }

    private Mono<Void> checkOpponent(GameEntity game, boolean iAmPlayer1) {
        if (isTerminal(game.getStatus())) {
            return Mono.empty();
        }
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(DISCONNECT_SECONDS);
        LocalDateTime opponentHeartbeat = iAmPlayer1 ? game.getPlayer2Heartbeat() : game.getPlayer1Heartbeat();
        boolean opponentAlive = opponentHeartbeat != null && opponentHeartbeat.isAfter(cutoff);

        if (GameStatus.WAITING_START.name().equals(game.getStatus())) {
            if (opponentAlive) {
                return gameRepository.transitionToInProgress(game.getPublicId()).then();
            }
            if (!game.getCreatedAt().isAfter(cutoff)) {
                return gameRepository.cancelGame(game.getPublicId(), GameStatus.WAITING_START.name()).then();
            }
            return Mono.empty();
        }

        if (GameStatus.IN_PROGRESS.name().equals(game.getStatus()) && !opponentAlive) {
            String myId = iAmPlayer1 ? game.getPlayer1Id() : game.getPlayer2Id();
            return gameRepository.finishGame(game.getPublicId(), myId, GameStatus.IN_PROGRESS.name()).then();
        }

        return Mono.empty();
    }

    public Mono<Void> forfeit(String publicId, String playerId) {
        return gameRepository.findByPublicId(publicId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found")))
                .flatMap(game -> {
                    boolean isPlayer1 = playerId.equals(game.getPlayer1Id());
                    boolean isPlayer2 = playerId.equals(game.getPlayer2Id());
                    if (!isPlayer1 && !isPlayer2) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a participant"));
                    }
                    if (isTerminal(game.getStatus())) {
                        return Mono.<Void>empty();
                    }
                    if (GameStatus.WAITING_START.name().equals(game.getStatus())) {
                        return gameRepository.cancelGame(publicId, GameStatus.WAITING_START.name()).then();
                    }
                    String opponentId = isPlayer1 ? game.getPlayer2Id() : game.getPlayer1Id();
                    return gameRepository.finishGame(publicId, opponentId, GameStatus.IN_PROGRESS.name()).then();
                });
    }

    private boolean isTerminal(String status) {
        return GameStatus.FINISHED.name().equals(status) || GameStatus.CANCELLED.name().equals(status);
    }
}