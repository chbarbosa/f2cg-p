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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private EmailService emailService;

    private PlayerService playerService;

    @BeforeEach
    void setUp() {
        playerService = new PlayerService(playerRepository, jwtUtil, emailService);
    }

    // --- register ---

    @Test
    void register_success() {
        when(playerRepository.findByUsername("alice@example.com")).thenReturn(Mono.empty());
        when(playerRepository.save(any(Player.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(playerService.register("alice@example.com", "pass123"))
                .assertNext(res -> assertThat(res.message()).isEqualTo("VERIFICATION_SENT"))
                .verifyComplete();

        verify(emailService).sendVerificationCode(eq("alice@example.com"), any());
    }

    @Test
    void register_invalidEmail_returnsBadRequest() {
        StepVerifier.create(playerService.register("not-an-email", "pass123"))
                .expectErrorMatches(ex ->
                        ex instanceof ResponseStatusException rse &&
                        rse.getStatusCode() == HttpStatus.BAD_REQUEST)
                .verify();
    }

    @Test
    void register_duplicateEmail_returnsConflict() {
        Player existing = new Player("id-1", "alice@example.com", "hash", true, null, null, null, null);
        when(playerRepository.findByUsername("alice@example.com")).thenReturn(Mono.just(existing));

        StepVerifier.create(playerService.register("alice@example.com", "pass123"))
                .expectErrorMatches(ex ->
                        ex instanceof ResponseStatusException rse &&
                        rse.getStatusCode() == HttpStatus.CONFLICT)
                .verify();
    }

    // --- verify ---

    @Test
    void verify_success() {
        Player player = new Player("id-1", "alice@example.com", "hash",
                false, "12345", LocalDateTime.now().plusMinutes(10), null, null);
        when(playerRepository.findByUsername("alice@example.com")).thenReturn(Mono.just(player));
        when(playerRepository.save(any(Player.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(jwtUtil.generate("id-1")).thenReturn("token-abc");

        StepVerifier.create(playerService.verify("alice@example.com", "12345"))
                .assertNext(res -> {
                    assertThat(res.playerId()).isEqualTo("id-1");
                    assertThat(res.token()).isEqualTo("token-abc");
                })
                .verifyComplete();
    }

    @Test
    void verify_wrongCode_returnsBadRequest() {
        Player player = new Player("id-1", "alice@example.com", "hash",
                false, "12345", LocalDateTime.now().plusMinutes(10), null, null);
        when(playerRepository.findByUsername("alice@example.com")).thenReturn(Mono.just(player));

        StepVerifier.create(playerService.verify("alice@example.com", "99999"))
                .expectErrorMatches(ex ->
                        ex instanceof ResponseStatusException rse &&
                        rse.getStatusCode() == HttpStatus.BAD_REQUEST)
                .verify();
    }

    @Test
    void verify_expiredCode_returnsBadRequest() {
        Player player = new Player("id-1", "alice@example.com", "hash",
                false, "12345", LocalDateTime.now().minusMinutes(1), null, null);
        when(playerRepository.findByUsername("alice@example.com")).thenReturn(Mono.just(player));

        StepVerifier.create(playerService.verify("alice@example.com", "12345"))
                .expectErrorMatches(ex ->
                        ex instanceof ResponseStatusException rse &&
                        rse.getStatusCode() == HttpStatus.BAD_REQUEST)
                .verify();
    }

    // --- login ---

    @Test
    void login_success() {
        String rawPassword = "pass123";
        String hash = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(rawPassword);
        Player player = new Player("id-1", "alice@example.com", hash, true, null, null, null, null);

        when(playerRepository.findByUsername("alice@example.com")).thenReturn(Mono.just(player));
        when(jwtUtil.generate("id-1")).thenReturn("token-xyz");

        StepVerifier.create(playerService.login("alice@example.com", rawPassword))
                .assertNext(res -> {
                    assertThat(res.playerId()).isEqualTo("id-1");
                    assertThat(res.token()).isEqualTo("token-xyz");
                })
                .verifyComplete();
    }

    @Test
    void login_inactiveAccount_returnsForbidden() {
        String rawPassword = "pass123";
        String hash = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(rawPassword);
        Player player = new Player("id-1", "alice@example.com", hash, false, "12345",
                LocalDateTime.now().plusMinutes(10), null, null);

        when(playerRepository.findByUsername("alice@example.com")).thenReturn(Mono.just(player));

        StepVerifier.create(playerService.login("alice@example.com", rawPassword))
                .expectErrorMatches(ex ->
                        ex instanceof ResponseStatusException rse &&
                        rse.getStatusCode() == HttpStatus.FORBIDDEN)
                .verify();
    }

    @Test
    void login_wrongPassword_returnsUnauthorized() {
        String hash = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("correct");
        Player player = new Player("id-1", "alice@example.com", hash, true, null, null, null, null);

        when(playerRepository.findByUsername("alice@example.com")).thenReturn(Mono.just(player));

        StepVerifier.create(playerService.login("alice@example.com", "wrong"))
                .expectErrorMatches(ex ->
                        ex instanceof ResponseStatusException rse &&
                        rse.getStatusCode() == HttpStatus.UNAUTHORIZED)
                .verify();
    }

    @Test
    void login_userNotFound_returnsUnauthorized() {
        when(playerRepository.findByUsername("ghost@example.com")).thenReturn(Mono.empty());

        StepVerifier.create(playerService.login("ghost@example.com", "pass123"))
                .expectErrorMatches(ex ->
                        ex instanceof ResponseStatusException rse &&
                        rse.getStatusCode() == HttpStatus.UNAUTHORIZED)
                .verify();
    }
}