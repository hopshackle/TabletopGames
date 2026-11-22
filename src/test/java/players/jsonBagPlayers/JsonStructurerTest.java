package players.jsonBagPlayers;

import com.fasterxml.jackson.databind.JsonNode;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import evaluation.records.GameStateRecordFactory;
import games.GameType;
import org.junit.Test;
import players.simple.RandomPlayer;
import utilities.Json;

import java.util.*;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class JsonStructurerTest {

    private static final String STATE = """
            {"turn": 3,
             "hands": [
               {"class": "core.components.Deck", "componentID": 10, "componentName": "Player 0 hand", "ownerId": 0,
                "components": [
                  {"class": "x.Card", "componentID": 11, "componentName": "Copper", "ownerId": 0, "cost": 0},
                  {"class": "x.Card", "componentID": 12, "componentName": "Copper", "ownerId": 0, "cost": 0},
                  {"class": "x.Card", "componentID": 13, "componentName": "Gold", "ownerId": 0, "cost": 6}]},
               {"class": "core.components.Deck", "componentID": 20, "componentName": "Player 1 hand", "ownerId": 1,
                "components": [
                  {"class": "x.Card", "componentID": 21, "componentName": "Estate", "ownerId": 1, "cost": 2}]}],
             "current": {"class": "x.Card", "componentID": 30, "componentName": "Silver", "cost": 3},
             "lastPlayedID": 13,
             "edgeIDs": [11, 999]}
            """;

    @Test
    public void decksAreKeyedByNameAndOwnerWithCardsGroupedByName() {
        Map<String, Integer> bag = Tokenizer.tokenizeStructured(Json.parse(STATE), null, false);
        assertEquals(2, bag.get(".decks.Player hand[owner=0].Copper.cost.0").intValue());
        assertEquals(1, bag.get(".decks.Player hand[owner=0].Gold.cost.6").intValue());
        assertEquals(1, bag.get(".decks.Player hand[owner=1].Estate.cost.2").intValue());
        assertEquals(3, bag.get(".decks.Player hand[owner=0].size").intValue());
        assertTrue("decks leave where they were", bag.keySet().stream().noneMatch(t -> t.startsWith(".hands")));
    }

    @Test
    public void otherComponentsStayWhereTheyAreWithTheirKind() {
        Map<String, Integer> bag = Tokenizer.tokenizeStructured(Json.parse(STATE), null, false);
        assertTrue(bag.containsKey(".current.kind.Card"));
        assertTrue(bag.containsKey(".current.cost.3"));
        assertTrue(bag.containsKey(".turn.3"));
    }

    @Test
    public void componentIdsAreReplacedByWhatTheyReferTo() {
        Map<String, Integer> bag = Tokenizer.tokenizeStructured(Json.parse(STATE), null, false);
        assertTrue(bag.containsKey(".lastPlayed.Gold"));
        // 999 is no component in the document, so it is dropped
        assertEquals(Set.of(".edge.Copper", ".edge[0].Copper"),
                bag.keySet().stream().filter(t -> t.startsWith(".edge")).collect(java.util.stream.Collectors.toSet()));
        assertTrue(bag.keySet().stream().noneMatch(Tokenizer::isIdentityToken));
    }

    @Test
    public void jsonWithoutComponentsTokenizesAsWithoutThePass() {
        JsonNode plain = Json.parse("{\"a\":[1,2,{\"b\":\"c\"}],\"d\":{\"e\":true}}");
        assertSame(plain, JsonStructurer.structure(plain));
        assertEquals(Tokenizer.tokenize(plain, "", true, "both", true, false, false, false, null, false),
                Tokenizer.tokenizeStructured(plain, null, false));
    }

    @Test
    public void everyGameWithARecordStructuresWithoutIdentityTokens() {
        for (GameType gt : GameType.values()) {
            if (!GameStateRecordFactory.hasRecord(gt)) continue;
            int nPlayers = Math.max(2, gt.getMinPlayers());
            Game game = gt.createGameInstance(nPlayers, 5L);
            game.reset(IntStream.range(0, nPlayers).mapToObj(i -> (AbstractPlayer) new RandomPlayer(new Random(i))).toList());
            AbstractGameState state = game.getGameState();
            Random rnd = new Random(5);
            for (int i = 0; i < 30 && state.isNotTerminal(); i++) {
                List<AbstractAction> actions = game.getForwardModel().computeAvailableActions(state);
                game.getForwardModel().next(state, actions.get(rnd.nextInt(actions.size())));
            }
            Map<String, Integer> bag = Tokenizer.tokenizeStructured(state, null, false);
            assertFalse(gt + " gives tokens", bag.isEmpty());
            List<String> identity = bag.keySet().stream().filter(Tokenizer::isIdentityToken).limit(5).toList();
            assertTrue(gt + " identity tokens: " + identity, identity.isEmpty());
        }
    }
}
