package com.f2cg.application;

import com.f2cg.api.dto.AuthResponse;
import com.f2cg.domain.player.Player;
import com.f2cg.infrastructure.JwtUtil;
import com.f2cg.infrastructure.r2dbc.PlayerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public PlayerService(PlayerRepository playerRepository, JwtUtil jwtUtil) {
        this.playerRepository = playerRepository;
        this.jwtUtil = jwtUtil;
    }

    public Mono<AuthResponse> register(String username, String password) {
        return playerRepository.findByUsername(username)
                .flatMap(existing -> Mono.<AuthResponse>error(
                        new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken")))
                .switchIfEmpty(Mono.defer(() -> {
                    String id = UUID.randomUUID().toString();
                    String hash = passwordEncoder.encode(password);
                    return playerRepository.save(new Player(id, username, hash))
                            .map(player -> new AuthResponse(player.getId(), jwtUtil.generate(player.getId())));
                }));
    }

    public Mono<AuthResponse> login(String username, String password) {
        return playerRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")))
                .flatMap(player -> {
                    if (!passwordEncoder.matches(password, player.getPasswordHash())) {
                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
                    }
                    return Mono.just(new AuthResponse(player.getId(), jwtUtil.generate(player.getId())));
                });
    }
}