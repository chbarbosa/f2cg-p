package com.f2cg.application;

import com.f2cg.domain.card.Card;
import com.f2cg.domain.game.FieldUnit;
import com.f2cg.domain.game.GamePhase;
import com.f2cg.domain.game.GameState;
import com.f2cg.domain.game.SummoningState;
import com.f2cg.domain.player.PlayerState;
import com.f2cg.infrastructure.r2dbc.CardEntityMapper;
import com.f2cg.infrastructure.r2dbc.CardRepository;
import com.f2cg.infrastructure.r2dbc.GameEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class GameStateLoader {

    private static final int INITIAL_MANA = 3;

    private final CardRepository cardRepository;
    private final CardEntityMapper cardEntityMapper;
    private final GameStateProjectionService projectionService;

    public GameStateLoader(CardRepository cardRepository,
                           CardEntityMapper cardEntityMapper,
                           GameStateProjectionService projectionService) {
        this.cardRepository = cardRepository;
        this.cardEntityMapper = cardEntityMapper;
        this.projectionService = projectionService;
    }

    public Mono<PlayerGameStateView> loadView(GameEntity game, String playerId) {
        List<String> allIds = collectAllCardIds(game);

        return cardRepository.findAllById(allIds)
                .collectMap(e -> e.getId(), cardEntityMapper::toDomain)
                .map(cardMap -> buildGameState(game, cardMap))
                .map(state -> projectionService.projectFor(state, playerId));
    }

    private List<String> collectAllCardIds(GameEntity game) {
        return Stream.of(
                game.getPlayer1Hand(),
                game.getPlayer1Stack(),
                game.getPlayer1Field(),
                game.getPlayer1Graveyard(),
                game.getPlayer2Hand(),
                game.getPlayer2Stack(),
                game.getPlayer2Field(),
                game.getPlayer2Graveyard()
        )
                .flatMap(s -> CardIdConverter.toList(s).stream())
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private GameState buildGameState(GameEntity game, Map<String, Card> cardMap) {
        PlayerState player1 = buildPlayerState(
                game.getPlayer1Id(), game.getPlayer1Username(),
                game.getPlayer1Hand(), game.getPlayer1Stack(),
                game.getPlayer1Field(), game.getPlayer1Graveyard(),
                game.isPlayer1SummoningConfirmed(),
                cardMap
        );
        PlayerState player2 = buildPlayerState(
                game.getPlayer2Id(), game.getPlayer2Username(),
                game.getPlayer2Hand(), game.getPlayer2Stack(),
                game.getPlayer2Field(), game.getPlayer2Graveyard(),
                game.isPlayer2SummoningConfirmed(),
                cardMap
        );
        return new GameState(
                game.getPublicId(),
                1,
                INITIAL_MANA,
                GamePhase.SUMMONING,
                game.getPlayer1Id(),
                new SummoningState(game.isPlayer1SummoningConfirmed(), game.isPlayer2SummoningConfirmed()),
                player1,
                player2,
                game.getWinnerId()
        );
    }

    private PlayerState buildPlayerState(
            String playerId, String username,
            String handStr, String stackStr,
            String fieldStr, String graveyardStr,
            boolean summoningConfirmed,
            Map<String, Card> cardMap) {

        List<Card> hand = resolveCards(handStr, cardMap);
        List<Card> stack = resolveCards(stackStr, cardMap);
        List<Card> graveyard = resolveCards(graveyardStr, cardMap);
        List<FieldUnit> field = List.of();

        return new PlayerState(playerId, username, field, hand, stack, graveyard, summoningConfirmed);
    }

    private List<Card> resolveCards(String idStr, Map<String, Card> cardMap) {
        return CardIdConverter.toList(idStr).stream()
                .map(cardMap::get)
                .filter(c -> c != null)
                .toList();
    }
}