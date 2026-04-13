package com.f2cg.application;

import com.f2cg.domain.game.GameStatus;
import com.f2cg.eventbus.AppEvent;
import com.f2cg.eventbus.AppEventType;
import com.f2cg.eventbus.EventBus;
import com.f2cg.infrastructure.r2dbc.GameEntity;
import com.f2cg.infrastructure.r2dbc.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    GameRepository gameRepository;

    @Mock
    EventBus eventBus;

    GameService gameService;

    static final String PUBLIC_ID = "game-pub-id";
    static final String P1_ID = "player1-id";
    static final String P2_ID = "player2-id";

    @BeforeEach
    void setUp() {
        gameService = new GameService(gameRepository, eventBus);
    }

    private GameEntity buildGame(String status, LocalDateTime p1Hb, LocalDateTime p2Hb, LocalDateTime createdAt) {
        GameEntity g = new GameEntity(PUBLIC_ID, P1_ID, P1_ID, "p1", P2_ID, P2_ID, "p2", status, createdAt);
        g.setPlayer1Heartbeat(p1Hb);
        g.setPlayer2Heartbeat(p2Hb);
        return g;
    }

    // --- heartbeat: both alive in WAITING_START → transition to IN_PROGRESS ---
    @Test
    void heartbeat_bothAlive_waitingStart_transitionsToInProgress() {
        LocalDateTime recent = LocalDateTime.now().minusSeconds(10);
        GameEntity game = buildGame(GameStatus.WAITING_START.name(), recent, recent, LocalDateTime.now().minusSeconds(5));
        when(gameRepository.findByPublicId(PUBLIC_ID))
                .thenReturn(Mono.just(game))
                .thenReturn(Mono.just(game));
        when(gameRepository.updatePlayer1Heartbeat(eq(PUBLIC_ID), eq(P1_ID), any())).thenReturn(Mono.just(1));
        when(gameRepository.transitionToInProgress(PUBLIC_ID)).thenReturn(Mono.just(1));

        StepVerifier.create(gameService.heartbeat(PUBLIC_ID, P1_ID))
                .verifyComplete();

        verify(gameRepository).transitionToInProgress(PUBLIC_ID);
    }

    // --- heartbeat: opponent dead in WAITING_START and game old enough → cancel ---
    @Test
    void heartbeat_opponentNeverConnected_gameOld_cancels() {
        LocalDateTime recent = LocalDateTime.now().minusSeconds(10);
        LocalDateTime oldCreated = LocalDateTime.now().minusSeconds(GameService.DISCONNECT_SECONDS + 10);
        GameEntity game = buildGame(GameStatus.WAITING_START.name(), recent, null, oldCreated);

        when(gameRepository.findByPublicId(PUBLIC_ID))
                .thenReturn(Mono.just(game))
                .thenReturn(Mono.just(game));
        when(gameRepository.updatePlayer1Heartbeat(eq(PUBLIC_ID), eq(P1_ID), any())).thenReturn(Mono.just(1));
        when(gameRepository.cancelGame(PUBLIC_ID, GameStatus.WAITING_START.name())).thenReturn(Mono.just(1));

        StepVerifier.create(gameService.heartbeat(PUBLIC_ID, P1_ID))
                .verifyComplete();

        verify(gameRepository).cancelGame(PUBLIC_ID, GameStatus.WAITING_START.name());
    }

    // --- heartbeat: opponent dead in IN_PROGRESS → finish, I win ---
    @Test
    void heartbeat_opponentDead_inProgress_iWin() {
        LocalDateTime recent = LocalDateTime.now().minusSeconds(10);
        LocalDateTime stale = LocalDateTime.now().minusSeconds(GameService.DISCONNECT_SECONDS + 10);
        GameEntity game = buildGame(GameStatus.IN_PROGRESS.name(), recent, stale, LocalDateTime.now().minusMinutes(5));

        when(gameRepository.findByPublicId(PUBLIC_ID))
                .thenReturn(Mono.just(game))
                .thenReturn(Mono.just(game));
        when(gameRepository.updatePlayer1Heartbeat(eq(PUBLIC_ID), eq(P1_ID), any())).thenReturn(Mono.just(1));
        when(gameRepository.finishGame(PUBLIC_ID, P1_ID, GameStatus.IN_PROGRESS.name())).thenReturn(Mono.just(1));

        StepVerifier.create(gameService.heartbeat(PUBLIC_ID, P1_ID))
                .verifyComplete();

        verify(gameRepository).finishGame(PUBLIC_ID, P1_ID, GameStatus.IN_PROGRESS.name());
    }

    // --- forfeit: WAITING_START → cancel ---
    @Test
    void forfeit_waitingStart_cancels() {
        GameEntity game = buildGame(GameStatus.WAITING_START.name(), null, null, LocalDateTime.now());
        when(gameRepository.findByPublicId(PUBLIC_ID)).thenReturn(Mono.just(game));
        when(gameRepository.cancelGame(PUBLIC_ID, GameStatus.WAITING_START.name())).thenReturn(Mono.just(1));

        StepVerifier.create(gameService.forfeit(PUBLIC_ID, P1_ID))
                .verifyComplete();

        verify(gameRepository).cancelGame(PUBLIC_ID, GameStatus.WAITING_START.name());
    }

    // --- forfeit: IN_PROGRESS → opponent wins ---
    @Test
    void forfeit_inProgress_opponentWins() {
        GameEntity game = buildGame(GameStatus.IN_PROGRESS.name(), null, null, LocalDateTime.now().minusMinutes(2));
        when(gameRepository.findByPublicId(PUBLIC_ID)).thenReturn(Mono.just(game));
        when(gameRepository.finishGame(PUBLIC_ID, P2_ID, GameStatus.IN_PROGRESS.name())).thenReturn(Mono.just(1));

        StepVerifier.create(gameService.forfeit(PUBLIC_ID, P1_ID))
                .verifyComplete();

        verify(gameRepository).finishGame(PUBLIC_ID, P2_ID, GameStatus.IN_PROGRESS.name());
    }

    // --- forfeit: terminal game is idempotent ---
    @Test
    void forfeit_terminalGame_isIdempotent() {
        GameEntity game = buildGame(GameStatus.FINISHED.name(), null, null, LocalDateTime.now().minusMinutes(5));
        when(gameRepository.findByPublicId(PUBLIC_ID)).thenReturn(Mono.just(game));

        StepVerifier.create(gameService.forfeit(PUBLIC_ID, P1_ID))
                .verifyComplete();
    }

    // --- event publishing ---

    @Test
    void forfeit_inProgress_publishesPlayerForfeited() {
        GameEntity game = buildGame(GameStatus.IN_PROGRESS.name(), null, null, LocalDateTime.now().minusMinutes(2));
        when(gameRepository.findByPublicId(PUBLIC_ID)).thenReturn(Mono.just(game));
        when(gameRepository.finishGame(PUBLIC_ID, P2_ID, GameStatus.IN_PROGRESS.name())).thenReturn(Mono.just(1));

        StepVerifier.create(gameService.forfeit(PUBLIC_ID, P1_ID)).verifyComplete();

        ArgumentCaptor<AppEvent> captor = ArgumentCaptor.forClass(AppEvent.class);
        verify(eventBus, atLeastOnce()).publish(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(e -> e.eventType() == AppEventType.PLAYER_FORFEITED
                && e.actorId().equals(P1_ID));
    }

    @Test
    void forfeit_inProgress_publishesGameFinishedWithForfeitReason() {
        GameEntity game = buildGame(GameStatus.IN_PROGRESS.name(), null, null, LocalDateTime.now().minusMinutes(2));
        when(gameRepository.findByPublicId(PUBLIC_ID)).thenReturn(Mono.just(game));
        when(gameRepository.finishGame(PUBLIC_ID, P2_ID, GameStatus.IN_PROGRESS.name())).thenReturn(Mono.just(1));

        StepVerifier.create(gameService.forfeit(PUBLIC_ID, P1_ID)).verifyComplete();

        ArgumentCaptor<AppEvent> captor = ArgumentCaptor.forClass(AppEvent.class);
        verify(eventBus, atLeastOnce()).publish(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(e ->
                e.eventType() == AppEventType.GAME_FINISHED && e.payload().contains("FORFEIT"));
    }

    @Test
    void heartbeat_opponentDead_inProgress_publishesPlayerDisconnected() {
        LocalDateTime recent = LocalDateTime.now().minusSeconds(10);
        LocalDateTime stale = LocalDateTime.now().minusSeconds(GameService.DISCONNECT_SECONDS + 10);
        GameEntity game = buildGame(GameStatus.IN_PROGRESS.name(), recent, stale, LocalDateTime.now().minusMinutes(5));

        when(gameRepository.findByPublicId(PUBLIC_ID))
                .thenReturn(Mono.just(game)).thenReturn(Mono.just(game));
        when(gameRepository.updatePlayer1Heartbeat(eq(PUBLIC_ID), eq(P1_ID), any())).thenReturn(Mono.just(1));
        when(gameRepository.finishGame(PUBLIC_ID, P1_ID, GameStatus.IN_PROGRESS.name())).thenReturn(Mono.just(1));

        StepVerifier.create(gameService.heartbeat(PUBLIC_ID, P1_ID)).verifyComplete();

        ArgumentCaptor<AppEvent> captor = ArgumentCaptor.forClass(AppEvent.class);
        verify(eventBus, atLeastOnce()).publish(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(e -> e.eventType() == AppEventType.PLAYER_DISCONNECTED);
    }

    @Test
    void heartbeat_opponentDead_inProgress_publishesGameFinishedWithDisconnectReason() {
        LocalDateTime recent = LocalDateTime.now().minusSeconds(10);
        LocalDateTime stale = LocalDateTime.now().minusSeconds(GameService.DISCONNECT_SECONDS + 10);
        GameEntity game = buildGame(GameStatus.IN_PROGRESS.name(), recent, stale, LocalDateTime.now().minusMinutes(5));

        when(gameRepository.findByPublicId(PUBLIC_ID))
                .thenReturn(Mono.just(game)).thenReturn(Mono.just(game));
        when(gameRepository.updatePlayer1Heartbeat(eq(PUBLIC_ID), eq(P1_ID), any())).thenReturn(Mono.just(1));
        when(gameRepository.finishGame(PUBLIC_ID, P1_ID, GameStatus.IN_PROGRESS.name())).thenReturn(Mono.just(1));

        StepVerifier.create(gameService.heartbeat(PUBLIC_ID, P1_ID)).verifyComplete();

        ArgumentCaptor<AppEvent> captor = ArgumentCaptor.forClass(AppEvent.class);
        verify(eventBus, atLeastOnce()).publish(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(e ->
                e.eventType() == AppEventType.GAME_FINISHED && e.payload().contains("DISCONNECT"));
    }
}