package com.f2cg.api;

import com.f2cg.api.dto.JoinQueueRequest;
import com.f2cg.api.dto.QueueEntryResponse;
import com.f2cg.application.QueueService;
import com.f2cg.infrastructure.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
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
        String playerId = extractPlayerId(auth);
        return queueService.joinQueue(playerId, req.deckId()).map(QueueEntryResponse::from);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> cancelQueue(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        String playerId = extractPlayerId(auth);
        return queueService.cancelQueue(playerId).then();
    }

    @GetMapping("/status")
    public Mono<QueueEntryResponse> getStatus(
            @RequestHeader(value = "Authorization", required = false) String auth) {
        String playerId = extractPlayerId(auth);
        return queueService.getStatus(playerId).map(QueueEntryResponse::from);
    }

    private String extractPlayerId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        try {
            return jwtUtil.extractPlayerId(authHeader.substring(7));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
    }
}