package com.f2cg.api;

import com.f2cg.api.dto.AuthResponse;
import com.f2cg.api.dto.LoginRequest;
import com.f2cg.api.dto.RegisterRequest;
import com.f2cg.application.PlayerService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final PlayerService playerService;

    public AuthController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<AuthResponse> register(@RequestBody RegisterRequest request) {
        return playerService.register(request.username(), request.password());
    }

    @PostMapping("/login")
    public Mono<AuthResponse> login(@RequestBody LoginRequest request) {
        return playerService.login(request.username(), request.password());
    }
}