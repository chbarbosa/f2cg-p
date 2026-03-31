package com.f2cg.api;

import com.f2cg.infrastructure.JwtUtil;
import com.f2cg.infrastructure.sse.QueueSseBroadcaster;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;

@RestController
@RequestMapping("/api/queue")
public class SseController {

    private final QueueSseBroadcaster sseBroadcaster;
    private final JwtUtil jwtUtil;

    public SseController(QueueSseBroadcaster sseBroadcaster, JwtUtil jwtUtil) {
        this.sseBroadcaster = sseBroadcaster;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> stream(@RequestParam String token) {
        String playerId = jwtUtil.extractPlayerId(token);

        Flux<ServerSentEvent<Object>> heartbeat = Flux.interval(Duration.ofSeconds(15))
                .map(tick -> ServerSentEvent.builder(new Object())
                        .event("heartbeat")
                        .build());

        Flux<ServerSentEvent<Object>> events = Flux.create(sink ->
                sseBroadcaster.register(playerId, sink));

        return Flux.merge(heartbeat, events)
                .doOnCancel(() -> sseBroadcaster.remove(playerId))
                .doOnTerminate(() -> sseBroadcaster.remove(playerId));
    }
}