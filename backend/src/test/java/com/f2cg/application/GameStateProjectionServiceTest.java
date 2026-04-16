package com.f2cg.application;

import com.f2cg.domain.card.Card;
import com.f2cg.domain.card.UnitCard;
import com.f2cg.domain.card.UnitClass;
import com.f2cg.domain.deck.DeckTheme;
import com.f2cg.domain.game.FieldUnit;
import com.f2cg.domain.game.GamePhase;
import com.f2cg.domain.game.GameState;
import com.f2cg.domain.game.SummoningState;
import com.f2cg.domain.player.PlayerState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GameStateProjectionServiceTest {

    GameStateProjectionService projectionService;

    static final String P1_ID = "player1-id";
    static final String P2_ID = "player2-id";
    static final String GAME_ID = "game-public-id";

    @BeforeEach
    void setUp() {
        projectionService = new GameStateProjectionService();
    }

    private Card card(String id) {
        return new UnitCard(id, "Card " + id, 1, UnitClass.WARRIOR, DeckTheme.WARRIOR, 1, 1, List.of());
    }

    private List<Card> cards(String... ids) {
        return List.of(ids).stream().map(this::card).toList();
    }

    private List<Card> stack(int count) {
        Card[] cs = new Card[count];
        for (int i = 0; i < count; i++) cs[i] = card("stack-" + i);
        return List.of(cs);
    }

    private GameState buildState(List<Card> p1Hand, List<Card> p1Stack, List<Card> p2Hand, List<Card> p2Stack) {
        PlayerState p1 = new PlayerState(P1_ID, "Alice", List.of(), p1Hand, p1Stack, List.of(), false);
        PlayerState p2 = new PlayerState(P2_ID, "Bob", List.of(), p2Hand, p2Stack, List.of(), false);
        return new GameState(GAME_ID, 1, 3, GamePhase.SUMMONING, P1_ID,
                new SummoningState(false, false), p1, p2, null);
    }

    @Test
    void projectFor_player1_meContainsFullHand() {
        List<Card> p1Hand = cards("h1", "h2", "h3", "h4", "h5");
        GameState state = buildState(p1Hand, stack(35), cards("o1", "o2", "o3", "o4", "o5"), stack(35));

        PlayerGameStateView view = projectionService.projectFor(state, P1_ID);

        assertThat(view.me().hand()).containsExactlyElementsOf(p1Hand);
    }

    @Test
    void projectFor_player1_opponentHasOnlyHandSize_noHandData() {
        List<Card> p2Hand = cards("o1", "o2", "o3", "o4", "o5");
        GameState state = buildState(cards("h1", "h2", "h3", "h4", "h5"), stack(35), p2Hand, stack(35));

        PlayerGameStateView view = projectionService.projectFor(state, P1_ID);

        assertThat(view.opponent().handSize()).isEqualTo(5);
        // OpponentView has no hand() method — compile-time guarantee; verify type
        assertThat(view.opponent()).isInstanceOf(PlayerGameStateView.OpponentView.class);
    }

    @Test
    void projectFor_player2_meContainsFullHand() {
        List<Card> p2Hand = cards("o1", "o2", "o3", "o4", "o5");
        GameState state = buildState(cards("h1", "h2", "h3", "h4", "h5"), stack(35), p2Hand, stack(35));

        PlayerGameStateView view = projectionService.projectFor(state, P2_ID);

        assertThat(view.me().hand()).containsExactlyElementsOf(p2Hand);
    }

    @Test
    void projectFor_player2_opponentHasOnlyHandSize() {
        List<Card> p1Hand = cards("h1", "h2", "h3", "h4", "h5");
        GameState state = buildState(p1Hand, stack(35), cards("o1", "o2", "o3", "o4", "o5"), stack(35));

        PlayerGameStateView view = projectionService.projectFor(state, P2_ID);

        assertThat(view.opponent().handSize()).isEqualTo(5);
    }

    @Test
    void projectFor_player1_neitherViewContainsStackContents() {
        List<Card> p1Stack = stack(35);
        List<Card> p2Stack = stack(35);
        GameState state = buildState(cards("h1", "h2", "h3", "h4", "h5"), p1Stack,
                cards("o1", "o2", "o3", "o4", "o5"), p2Stack);

        PlayerGameStateView view = projectionService.projectFor(state, P1_ID);

        // Only size exposed — no card data from either stack
        assertThat(view.me().stackSize()).isEqualTo(35);
        assertThat(view.opponent().stackSize()).isEqualTo(35);
        // MyView has no stack() method — compile-time enforced
    }

    @Test
    void projectFor_player2_neitherViewContainsStackContents() {
        GameState state = buildState(cards("h1", "h2", "h3", "h4", "h5"), stack(35),
                cards("o1", "o2", "o3", "o4", "o5"), stack(35));

        PlayerGameStateView view = projectionService.projectFor(state, P2_ID);

        assertThat(view.me().stackSize()).isEqualTo(35);
        assertThat(view.opponent().stackSize()).isEqualTo(35);
    }

    @Test
    void projectFor_stackSizeAndHandSizeCountsAreCorrect() {
        GameState state = buildState(cards("h1", "h2", "h3"), stack(37),
                cards("o1", "o2", "o3", "o4"), stack(36));

        PlayerGameStateView p1View = projectionService.projectFor(state, P1_ID);
        assertThat(p1View.me().hand()).hasSize(3);
        assertThat(p1View.me().stackSize()).isEqualTo(37);
        assertThat(p1View.opponent().handSize()).isEqualTo(4);
        assertThat(p1View.opponent().stackSize()).isEqualTo(36);

        PlayerGameStateView p2View = projectionService.projectFor(state, P2_ID);
        assertThat(p2View.me().hand()).hasSize(4);
        assertThat(p2View.me().stackSize()).isEqualTo(36);
        assertThat(p2View.opponent().handSize()).isEqualTo(3);
        assertThat(p2View.opponent().stackSize()).isEqualTo(37);
    }

    @Test
    void projectFor_fieldAndGraveyardVisibleToBothPlayers() {
        Card graveyardCard = card("graveyard-card");
        FieldUnit fieldUnit = new FieldUnit(new UnitCard("fu", "FieldUnit", 1, UnitClass.WARRIOR, DeckTheme.WARRIOR, 2, 3, List.of()), 2, 3, null, null, false);

        PlayerState p1 = new PlayerState(P1_ID, "Alice", List.of(fieldUnit),
                cards("h1", "h2", "h3", "h4", "h5"), stack(35), List.of(graveyardCard), false);
        PlayerState p2 = new PlayerState(P2_ID, "Bob", List.of(),
                cards("o1", "o2", "o3", "o4", "o5"), stack(35), List.of(), false);
        GameState state = new GameState(GAME_ID, 1, 3, GamePhase.ACTION, P1_ID,
                new SummoningState(false, false), p1, p2, null);

        PlayerGameStateView p1View = projectionService.projectFor(state, P1_ID);
        assertThat(p1View.me().field()).containsExactly(fieldUnit);
        assertThat(p1View.me().graveyard()).containsExactly(graveyardCard);

        PlayerGameStateView p2View = projectionService.projectFor(state, P2_ID);
        assertThat(p2View.opponent().field()).containsExactly(fieldUnit);
        assertThat(p2View.opponent().graveyard()).containsExactly(graveyardCard);
    }

    @Test
    void projectFor_player1HandCardsNeverAppearInPlayer2View() {
        List<Card> p1Hand = cards("secret1", "secret2", "secret3", "secret4", "secret5");
        List<Card> p2Hand = cards("open1", "open2", "open3", "open4", "open5");
        GameState state = buildState(p1Hand, stack(35), p2Hand, stack(35));

        PlayerGameStateView p2View = projectionService.projectFor(state, P2_ID);

        List<String> p1CardIds = p1Hand.stream().map(Card::id).toList();
        List<String> visibleToP2 = p2View.me().hand().stream().map(Card::id).toList();
        List<String> opponentVisibleToP2 = p2View.opponent().graveyard().stream().map(Card::id).toList();

        assertThat(visibleToP2).doesNotContainAnyElementsOf(p1CardIds);
        assertThat(opponentVisibleToP2).doesNotContainAnyElementsOf(p1CardIds);
    }

    @Test
    void projectFor_player2HandCardsNeverAppearInPlayer1View() {
        List<Card> p1Hand = cards("open1", "open2", "open3", "open4", "open5");
        List<Card> p2Hand = cards("secret1", "secret2", "secret3", "secret4", "secret5");
        GameState state = buildState(p1Hand, stack(35), p2Hand, stack(35));

        PlayerGameStateView p1View = projectionService.projectFor(state, P1_ID);

        List<String> p2CardIds = p2Hand.stream().map(Card::id).toList();
        List<String> visibleToP1 = p1View.me().hand().stream().map(Card::id).toList();

        assertThat(visibleToP1).doesNotContainAnyElementsOf(p2CardIds);
        assertThat(p1View.opponent().handSize()).isEqualTo(5);
    }
}