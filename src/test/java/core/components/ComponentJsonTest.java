package core.components;

import com.fasterxml.jackson.databind.JsonNode;
import core.CoreConstants.VisibilityMode;
import core.properties.PropertyColor;
import core.properties.PropertyInt;
import core.properties.PropertyString;
import org.junit.Test;
import utilities.Json;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Core components are written to JSON with their class and IDs, and read back equal to the original.
 */
public class ComponentJsonTest {

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T component) throws Exception {
        String json = Json.mapper().writeValueAsString(component);
        return (T) Json.stateReader(new HashMap<>()).readValue(json, Component.class);
    }

    @Test
    public void componentsCarryTheirClassAndIds() {
        Token token = new Token("gem");
        token.setOwnerId(1);
        JsonNode json = Json.toTree(token);
        assertEquals(Token.class.getName(), json.get("class").asText());
        assertEquals(token.getComponentID(), json.get("componentID").asInt());
        assertEquals(1, json.get("ownerId").asInt());
    }

    @Test
    public void cardWithPropertiesRoundTrips() throws Exception {
        Card card = new Card("Copper");
        card.setProperty(new PropertyInt("cost", 3));
        card.setProperty(new PropertyString("text", "+1 coin"));
        card.setProperty(new PropertyColor("colour", "red"));
        Card back = roundTrip(card);
        assertEquals(card.getComponentID(), back.getComponentID());
        assertEquals(card.getComponentName(), back.getComponentName());
        assertEquals(card.getType(), back.getType());
        assertEquals(card.getProperty("cost"), back.getProperty("cost"));
        assertEquals(card.getProperty("text"), back.getProperty("text"));
        assertEquals(card.getProperty("colour"), back.getProperty("colour"));
    }

    @Test
    public void deckRoundTripsWithItsCards() throws Exception {
        Deck<Card> deck = new Deck<>("hand", 1, VisibilityMode.VISIBLE_TO_OWNER);
        deck.add(new Card("Copper"));
        deck.add(new Card("Estate"));
        Deck<Card> back = roundTrip(deck);
        assertEquals(deck, back);
        assertEquals(1, back.getOwnerId());
        assertEquals("Estate", back.get(0).getComponentName());
    }

    @Test
    public void anEmptyDeckRoundTrips() throws Exception {
        Deck<Card> deck = new Deck<>("discard", VisibilityMode.VISIBLE_TO_ALL);
        Deck<Card> back = roundTrip(deck);
        assertEquals(deck, back);
        assertEquals(0, back.getSize());
    }

    @Test
    public void partialObservableDeckKeepsItsClassAndVisibility() throws Exception {
        PartialObservableDeck<Card> deck = new PartialObservableDeck<>("draw", 0, 2, VisibilityMode.HIDDEN_TO_ALL);
        deck.add(new Card("Silver"));
        deck.add(new Card("Gold"));
        deck.setVisibilityOfComponent(0, 1, true);
        Deck<Card> asDeck = deck;
        PartialObservableDeck<Card> back = roundTrip(asDeck) instanceof PartialObservableDeck<Card> p ? p : null;
        assertNotNull("read back as a plain Deck", back);
        assertEquals(deck, back);
    }

    @Test
    public void countersAndDiceRoundTrip() throws Exception {
        Counter counter = new Counter(2, 0, 10, "score");
        Counter counterBack = roundTrip(counter);
        assertEquals(counter, counterBack);
        assertEquals(2, counterBack.getValue());

        Dice dice = new Dice(new double[]{0.5, 0.25, 0.25});
        Dice diceBack = roundTrip(dice);
        assertEquals(dice.getComponentID(), diceBack.getComponentID());
        assertEquals(dice.getNumberOfSides(), diceBack.getNumberOfSides());
        assertEquals(dice.getValue(), diceBack.getValue());
        assertArrayEquals(dice.getPdf(), diceBack.getPdf(), 0.0);
    }

    @Test
    public void tokenRoundTrips() throws Exception {
        Token token = new Token("gem");
        token.setOwnerId(2);
        Token back = roundTrip(token);
        assertEquals(token.getComponentID(), back.getComponentID());
        assertEquals("gem", back.getTokenType());
        assertEquals(2, back.getOwnerId());
    }

    @Test
    public void aComponentInSeveralPlacesIsReadBackAsOneInstance() throws Exception {
        BoardNode empty = new BoardNode(".");
        GridBoard board = new GridBoard(3, 2, empty);
        BoardNode piece = new BoardNode("x");
        board.setElement(1, 1, piece);

        Map<Integer, Component> read = new HashMap<>();
        GridBoard back = (GridBoard) Json.stateReader(read).readValue(Json.mapper().writeValueAsString(board), Component.class);
        assertEquals(board, back);
        assertSame(back.getElement(0, 0), back.getElement(2, 1));
        assertEquals(piece.getComponentID(), back.getElement(1, 1).getComponentID());
        assertTrue(read.keySet().containsAll(java.util.List.of(board.getComponentID(), empty.getComponentID(), piece.getComponentID())));
    }

    @Test
    public void theIdCounterMovesPastIdsReadBack() {
        Token token = new Token("t");
        Component.reserveIdsUpTo(token.getComponentID() + 1000);
        assertTrue(new Token("u").getComponentID() > token.getComponentID() + 1000);
    }
}
