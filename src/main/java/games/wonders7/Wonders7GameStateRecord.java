package games.wonders7;

import core.AbstractGameState;
import core.AbstractGameStateRecord;
import core.actions.AbstractAction;
import core.components.Deck;
import games.wonders7.cards.Wonder7Board;
import games.wonders7.cards.Wonder7Card;

import java.util.EnumMap;
import java.util.ArrayList;
import java.util.List;

public class Wonders7GameStateRecord extends AbstractGameStateRecord {
    int currentAge; // int from 1,2,3 of current age
    List<EnumMap<Wonders7Constants.Resource, Integer>> playerResources; // Each player's full resource counts
    List<Deck<Wonder7Card>> playerHands; // Player Hands
    List<Deck<Wonder7Card>> playedCards; // Player used cards
    Deck<Wonder7Card> ageDeck; // The 'draw deck' for the Age
    Deck<Wonder7Card> discardPile; // Discarded cards
    Deck<Wonder7Board> wonderBoardDeck; // The deck of wonder board that decide a players wonder
    Wonder7Board[] playerWonderBoard; // Every player's assigned Wonder Board
    AbstractAction[] turnActions; // The round's actions chosen by each player
    int direction;

    public Wonders7GameStateRecord(Wonders7GameState gs) {
        super(gs);
        currentAge = gs.currentAge;
        playerResources = gs.playerResources;
        playerHands = gs.playerHands;
        playedCards = gs.playedCards;
        ageDeck = gs.ageDeck;
        discardPile = gs.discardPile;
        wonderBoardDeck = gs.wonderBoardDeck;
        playerWonderBoard = gs.playerWonderBoard;
        turnActions = gs.turnActions;
        direction = gs.direction;
    }

    @SuppressWarnings("unused") // for JSON
    private Wonders7GameStateRecord() {
    }

    @Override
    protected void restoreGame(AbstractGameState state) {
        Wonders7GameState w7 = (Wonders7GameState) state;
        Wonders7GameParameters params = (Wonders7GameParameters) state.getGameParameters();
        w7.currentAge = currentAge;
        w7.playerResources = new ArrayList<>();
        for (EnumMap<Wonders7Constants.Resource, Integer> resources : playerResources)
            w7.playerResources.add(new EnumMap<>(resources));
        w7.playerHands = rebuilt(playerHands, params);
        w7.playedCards = rebuilt(playedCards, params);
        w7.ageDeck = rebuilt(ageDeck, params);
        w7.discardPile = rebuilt(discardPile, params);
        w7.wonderBoardDeck = wonderBoardDeck;
        w7.playerWonderBoard = playerWonderBoard.clone();
        w7.turnActions = turnActions.clone();
        w7.direction = direction;
    }

    private static List<Deck<Wonder7Card>> rebuilt(List<Deck<Wonder7Card>> decks, Wonders7GameParameters params) {
        List<Deck<Wonder7Card>> result = new ArrayList<>();
        for (Deck<Wonder7Card> deck : decks) result.add(rebuilt(deck, params));
        return result;
    }

    // Cards are rebuilt from their type, as their effects hold functions that are not written to JSON
    private static Deck<Wonder7Card> rebuilt(Deck<Wonder7Card> deck, Wonders7GameParameters params) {
        List<Wonder7Card> cards = new ArrayList<>();
        for (Wonder7Card card : deck.getComponents()) cards.add(card.rebuilt(params));
        deck.setComponents(cards);
        return deck;
    }
}
