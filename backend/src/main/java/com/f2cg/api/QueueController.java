package com.f2cg.api;

import com.f2cg.api.dto.JoinQueueRequest;
import com.f2cg.api.dto.QueueEntryResponse;
import com.f2cg.application.QueueService;
import com.f2cg.infrastructure.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/queue")
public class QueueController {

    private final QueueService queueService;
    private final JwtUtil jwtUtil;

    public QueueController(QueueService queueService, JwtUtil jwtUtil) {
        this.queueService = queueService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<QueueEntryResponse> joinQueue(
            @RequestBody JoinQueueRequest req,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        String playerId = jwtUtil.extractPlayerIdFromHeader(auth);
        return queueService.joinQueue(playerId, req.deckId()).map(QueueEntryResponse::from);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> cancelQueue(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        String playerId = jwtUtil.extractPlayerIdFromHeader(auth);
        return queueService.cancelQueue(playerId).then();
    }

    @GetMapping("/status")
    public Mono<QueueEntryResponse> getStatus(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        String playerId = jwtUtil.extractPlayerIdFromHeader(auth);
        return queueService.getStatus(playerId).map(QueueEntryResponse::from);
    }

}