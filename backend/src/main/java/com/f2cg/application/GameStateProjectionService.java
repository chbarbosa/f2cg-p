package com.f2cg.application;

import com.f2cg.domain.game.GameState;
import com.f2cg.domain.player.PlayerState;
import org.springframework.stereotype.Service;

@Service
public class GameStateProjectionService {

    public PlayerGameStateView projectFor(GameState state, String playerId) {
        PlayerState me;
        PlayerState opponent;
        if (playerId.equals(state.player1().playerId())) {
            me = state.player1();
            opponent = state.player2();
        } else {
            me = state.player2();
            opponent = state.player1();
        }

        return new PlayerGameStateView(
                state.gameId(),
                state.turnNumber(),
                state.currentMana(),
                state.phase(),
                state.activePlayerId(),
                new PlayerGameStateView.MyView(
                        me.playerId(),
                        me.username(),
                        me.hand(),
                        me.stack().size(),
                        me.field(),
                        me.graveyard(),
                        me.summoningConfirmed()
                ),
                new PlayerGameStateView.OpponentView(
                        opponent.playerId(),
                        opponent.username(),
                        opponent.hand().size(),
                        opponent.stack().size(),
                        opponent.field(),
                        opponent.graveyard(),
                        opponent.summoningConfirmed()
                )
        );
    }
}