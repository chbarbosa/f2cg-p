package com.f2cg.application;

import com.f2cg.domain.player.Player;
import com.f2cg.infrastructure.JwtUtil;
import com.f2cg.infrastructure.r2dbc.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private JwtUtil jwtUtil;

    private PlayerService playerService;

    @BeforeEach
    void setUp() {
        playerService = new PlayerService(playerRepository, jwtUtil);
    }

    // --- register ---

    @Test
    void register_success() {
        when(playerRepository.findByUsername("alice")).thenReturn(Mono.empty());
        when(playerRepository.save(any(Player.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(jwtUtil.generate(any())).thenReturn("token-abc");

        StepVerifier.create(playerService.register("alice", "pass123"))
                .assertNext(res -> {
                    assertThat(res.token()).isEqualTo("token-abc");
                    assertThat(res.playerId()).isNotBlank();
                })
                .verifyComplete();
    }

    @Test
    void register_duplicateUsername_returnsConflict() {
        Player existing = new Player("id-1", "alice", "hash");
        when(playerRepository.findByUsername("alice")).thenReturn(Mono.just(existing));

        StepVerifier.create(playerService.register("alice", "pass123"))
                .expectErrorMatches(ex ->
                        ex instanceof ResponseStatusException rse &&
                        rse.getStatusCode() == HttpStatus.CONFLICT)
                .verify();
    }

    // --- login ---

    @Test
    void login_success() {
        String rawPassword = "pass123";
        String hash = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(rawPassword);
        Player player = new Player("id-1", "alice", hash);

        when(playerRepository.findByUsername("alice")).thenReturn(Mono.just(player));
        when(jwtUtil.generate("id-1")).thenReturn("token-xyz");

        StepVerifier.create(playerService.login("alice", rawPassword))
                .assertNext(res -> {
                    assertThat(res.playerId()).isEqualTo("id-1");
                    assertThat(res.token()).isEqualTo("token-xyz");
                })
                .verifyComplete();
    }

    @Test
    void login_wrongPassword_returnsUnauthorized() {
        String hash = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("correct");
        Player player = new Player("id-1", "alice", hash);

        when(playerRepository.findByUsername("alice")).thenReturn(Mono.just(player));

        StepVerifier.create(playerService.login("alice", "wrong"))
                .expectErrorMatches(ex ->
                        ex instanceof ResponseStatusException rse &&
                        rse.getStatusCode() == HttpStatus.UNAUTHORIZED)
                .verify();
    }

    @Test
    void login_userNotFound_returnsUnauthorized() {
        when(playerRepository.findByUsername("ghost")).thenReturn(Mono.empty());

        StepVerifier.create(playerService.login("ghost", "pass123"))
                .expectErrorMatches(ex ->
                        ex instanceof ResponseStatusException rse &&
                        rse.getStatusCode() == HttpStatus.UNAUTHORIZED)
                .verify();
    }
}