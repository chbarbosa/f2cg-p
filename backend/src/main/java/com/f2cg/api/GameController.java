package com.f2cg.api;

import com.f2cg.api.dto.GameResponse;
import com.f2cg.application.GameService;
import com.f2cg.application.GameStateLoader;
import com.f2cg.application.PlayerGameStateView;
import com.f2cg.infrastructure.JwtUtil;
import com.f2cg.infrastructure.r2dbc.GameRepository;
import com.f2cg.infrastructure.sse.GameSseBroadcaster;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameRepository gameRepository;
    private final GameService gameService;
    private final GameStateLoader gameStateLoader;
    private final JwtUtil jwtUtil;
    private final GameSseBroadcaster gameSseBroadcaster;

    public GameController(GameRepository gameRepository, GameService gameService,
                          GameStateLoader gameStateLoader, JwtUtil jwtUtil,
                          GameSseBroadcaster gameSseBroadcaster) {
        this.gameRepository = gameRepository;
        this.gameService = gameService;
        this.gameStateLoader = gameStateLoader;
        this.jwtUtil = jwtUtil;
        this.gameSseBroadcaster = gameSseBroadcaster;
    }

    @GetMapping("/{publicId}")
    public Mono<GameResponse> getGame(@PathVariable String publicId,
                                      @RequestHeader("Authorization") String authHeader) {
        String playerId = jwtUtil.extractPlayerIdFromHeader(authHeader);
        return gameRepository.findByPublicId(publicId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found")))
                .flatMap(game -> {
                    boolean isParticipant = playerId.equals(game.getPlayer1Id())
                            || playerId.equals(game.getPlayer2Id());
                    if (!isParticipant) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));
                    }
                    return Mono.just(GameResponse.from(game));
                });
    }

    @GetMapping("/{publicId}/state")
    public Mono<PlayerGameStateView> getGameState(@PathVariable String publicId,
                                                  @RequestHeader("Authorization") String authHeader) {
        String playerId = jwtUtil.extractPlayerIdFromHeader(authHeader);
        return gameRepository.findByPublicId(publicId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found")))
                .flatMap(game -> {
                    boolean isParticipant = playerId.equals(game.getPlayer1Id())
                            || playerId.equals(game.getPlayer2Id());
                    if (!isParticipant) {
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));
                    }
                    return gameStateLoader.loadView(game, playerId);
                });
    }

    @PostMapping("/{publicId}/heartbeat")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> heartbeat(@PathVariable String publicId,
                                @RequestHeader("Authorization") String authHeader) {
        String playerId = jwtUtil.extractPlayerIdFromHeader(authHeader);
        return gameService.heartbeat(publicId, playerId);
    }

    @PostMapping("/{publicId}/forfeit")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> forfeit(@PathVariable String publicId,
                              @RequestHeader("Authorization") String authHeader) {
        String playerId = jwtUtil.extractPlayerIdFromHeader(authHeader);
        return gameService.forfeit(publicId, playerId);
    }

    @GetMapping(value = "/{publicId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> stream(@PathVariable String publicId,
                                                @RequestParam String token) {
        String playerId = jwtUtil.extractPlayerId(token);

        return gameRepository.findByPublicId(publicId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found")))
                .flatMapMany(game -> {
                    boolean isParticipant = playerId.equals(game.getPlayer1Id())
                            || playerId.equals(game.getPlayer2Id());
                    if (!isParticipant) {
                        return Flux.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));
                    }

                    Flux<ServerSentEvent<Object>> heartbeat = Flux.interval(Duration.ofSeconds(15))
                            .map(tick -> ServerSentEvent.builder(new Object())
                                    .event("heartbeat")
                                    .build());

                    Flux<ServerSentEvent<Object>> events = Flux.create(sink -> {
                        gameSseBroadcaster.register(playerId, sink);
                        gameStateLoader.loadView(game, playerId)
                                .subscribe(view -> sink.next(ServerSentEvent.<Object>builder(view)
                                        .event("GAME_STATE_UPDATE")
                                        .build()));
                    });

                    return Flux.merge(heartbeat, events)
                            .doOnCancel(() -> gameSseBroadcaster.remove(playerId))
                            .doOnTerminate(() -> gameSseBroadcaster.remove(playerId));
                });
    }
}