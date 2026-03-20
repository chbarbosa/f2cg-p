package com.f2cg.api;

import com.f2cg.api.dto.UpdateProfileRequest;
import com.f2cg.application.PlayerService;
import com.f2cg.infrastructure.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/player")
public class PlayerController {

    private final PlayerService playerService;
    private final JwtUtil jwtUtil;

    public PlayerController(PlayerService playerService, JwtUtil jwtUtil) {
        this.playerService = playerService;
        this.jwtUtil = jwtUtil;
    }

    @PutMapping("/profile")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> updateProfile(
            @RequestBody UpdateProfileRequest req,
            @RequestHeader(value = "Authorization", required = false) String auth) {
        return playerService.updateProfile(extractPlayerId(auth), req.nickname(), req.country());
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