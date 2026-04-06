package com.f2cg.api;

import com.f2cg.api.dto.GameResponse;
import com.f2cg.application.GameService;
import com.f2cg.infrastructure.JwtUtil;
import com.f2cg.infrastructure.r2dbc.GameRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameRepository gameRepository;
    private final GameService gameService;
    private final JwtUtil jwtUtil;

    public GameController(GameRepository gameRepository, GameService gameService, JwtUtil jwtUtil) {
        this.gameRepository = gameRepository;
        this.gameService = gameService;
        this.jwtUtil = jwtUtil;
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
}