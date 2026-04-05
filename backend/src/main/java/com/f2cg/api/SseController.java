package com.f2cg.api;

import com.f2cg.infrastructure.JwtUtil;
import com.f2cg.infrastructure.r2dbc.GameRepository;
import com.f2cg.infrastructure.r2dbc.QueueEntryRepository;
import com.f2cg.infrastructure.sse.QueueSseBroadcaster;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/queue")
public class SseController {

    private final QueueSseBroadcaster sseBroadcaster;
    private final JwtUtil jwtUtil;
    private final QueueEntryRepository queueEntryRepository;
    private final GameRepository gameRepository;

    public SseController(QueueSseBroadcaster sseBroadcaster, JwtUtil jwtUtil,
                         QueueEntryRepository queueEntryRepository, GameRepository gameRepository) {
        this.sseBroadcaster = sseBroadcaster;
        this.jwtUtil = jwtUtil;
        this.queueEntryRepository = queueEntryRepository;
        this.gameRepository = gameRepository;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> stream(@RequestParam String token) {
        String playerId = jwtUtil.extractPlayerId(token);

        Flux<ServerSentEvent<Object>> heartbeat = Flux.interval(Duration.ofSeconds(15))
                .map(tick -> ServerSentEvent.builder(new Object())
                        .event("heartbeat")
                        .build());

        Flux<ServerSentEvent<Object>> events = Flux.create(sink -> {
            sseBroadcaster.register(playerId, sink);
            // Race condition guard: if the match was found before this SSE connection
            // opened, emit MATCH_FOUND immediately rather than waiting forever.
            queueEntryRepository.findByPlayerIdAndStatus(playerId, "MATCHED")
                    .flatMap(entry -> gameRepository.findLatestWaitingByPlayerId(playerId))
                    .subscribe(game -> {
                        String opponentUsername = game.getPlayer1Id().equals(playerId)
                                ? game.getPlayer2Username()
                                : game.getPlayer1Username();
                        sink.next(ServerSentEvent.<Object>builder(Map.of(
                                "gamePublicId", game.getPublicId(),
                                "opponentUsername", opponentUsername
                        )).event("MATCH_FOUND").build());
                        sink.complete();
                    });
        });

        return Flux.merge(heartbeat, events)
                .doOnCancel(() -> sseBroadcaster.remove(playerId))
                .doOnTerminate(() -> sseBroadcaster.remove(playerId));
    }
}