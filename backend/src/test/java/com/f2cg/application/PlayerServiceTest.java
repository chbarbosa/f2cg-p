package com.f2cg.application;

import com.f2cg.domain.player.Player;
import com.f2cg.eventbus.AppEventType;
import com.f2cg.eventbus.AppEvent;
import com.f2cg.eventbus.EventBus;
import com.f2cg.infrastructure.JwtUtil;
import com.f2cg.infrastructure.r2dbc.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private EmailService emailService;

    @Mock
    private SeasonService seasonService;

    @Mock
    private RankCalculationService rankCalculationService;

    @Mock
    private EventBus eventBus;

    private PlayerService playerService;

    @BeforeEach
    void setUp() {
        playerService = new PlayerService(playerRepository, jwtUtil, emailService,
                seasonService, rankCalculationService, eventBus);
        lenient().when(eventBus.timed(any(), any(), any(), any(), any()))
                .thenAnswer(inv -> inv.getArgument(4));
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
        when(seasonService.getCurrentSeason()).thenReturn(Mono.error(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No active season")));

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

    // --- event publishing ---

    @Test
    void login_validCredentials_publishesLoginSuccess() {
        String rawPassword = "pass123";
        String hash = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(rawPassword);
        Player player = new Player("id-1", "alice@example.com", hash, true, null, null, null, null);

        when(playerRepository.findByUsername("alice@example.com")).thenReturn(Mono.just(player));
        when(jwtUtil.generate("id-1")).thenReturn("token-xyz");
        when(seasonService.getCurrentSeason()).thenReturn(Mono.error(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No active season")));

        StepVerifier.create(playerService.login("alice@example.com", rawPassword))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<AppEvent> captor = ArgumentCaptor.forClass(AppEvent.class);
        verify(eventBus, atLeastOnce()).publish(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(e -> e.eventType() == AppEventType.LOGIN_SUCCESS);
    }

    @Test
    void login_wrongPassword_publishesLoginFailure() {
        String hash = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("correct");
        Player player = new Player("id-1", "alice@example.com", hash, true, null, null, null, null);

        when(playerRepository.findByUsername("alice@example.com")).thenReturn(Mono.just(player));

        StepVerifier.create(playerService.login("alice@example.com", "wrong"))
                .expectError()
                .verify();

        ArgumentCaptor<AppEvent> captor = ArgumentCaptor.forClass(AppEvent.class);
        verify(eventBus, atLeastOnce()).publish(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(e -> e.eventType() == AppEventType.LOGIN_FAILURE);
    }

    @Test
    void login_accountNotFound_publishesLoginFailure() {
        when(playerRepository.findByUsername("ghost@example.com")).thenReturn(Mono.empty());

        StepVerifier.create(playerService.login("ghost@example.com", "pass"))
                .expectError()
                .verify();

        ArgumentCaptor<AppEvent> captor = ArgumentCaptor.forClass(AppEvent.class);
        verify(eventBus, atLeastOnce()).publish(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(e -> e.eventType() == AppEventType.LOGIN_FAILURE);
    }

    @Test
    void login_publishesLoginTimedEvent() {
        String rawPassword = "pass123";
        String hash = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(rawPassword);
        Player player = new Player("id-1", "alice@example.com", hash, true, null, null, null, null);

        when(playerRepository.findByUsername("alice@example.com")).thenReturn(Mono.just(player));
        when(jwtUtil.generate("id-1")).thenReturn("token-xyz");
        when(seasonService.getCurrentSeason()).thenReturn(Mono.error(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No active season")));

        StepVerifier.create(playerService.login("alice@example.com", rawPassword))
                .expectNextCount(1)
                .verifyComplete();

        verify(eventBus).timed(eq(AppEventType.LOGIN_TIMED), any(), any(), any(), any());
    }

    @Test
    void updateProfile_publishesUserUpdated() {
        Player player = new Player("id-1", "alice@example.com", "hash", true, null, null, null, null);
        when(playerRepository.findById("id-1")).thenReturn(Mono.just(player));
        when(playerRepository.save(any(Player.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(playerService.updateProfile("id-1", "Alice", "Brazil"))
                .verifyComplete();

        ArgumentCaptor<AppEvent> captor = ArgumentCaptor.forClass(AppEvent.class);
        verify(eventBus, atLeastOnce()).publish(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(e -> e.eventType() == AppEventType.USER_UPDATED);
    }
}