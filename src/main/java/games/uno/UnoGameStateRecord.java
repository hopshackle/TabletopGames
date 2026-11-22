package games.uno;

import core.AbstractGameState;
import core.AbstractGameStateRecord;
import core.components.Deck;
import games.uno.cards.UnoCard;

import java.util.ArrayList;
import java.util.List;

public class UnoGameStateRecord extends AbstractGameStateRecord {
    List<Deck<UnoCard>> playerDecks;
    Deck<UnoCard> drawDeck;
    Deck<UnoCard> discardDeck;
    UnoCard currentCard;
    String currentColor;
    int[] playerScore;
    int[] expulsionRound;

    // Turn order data
    boolean skipTurn;
    int direction;
    public UnoGameStateRecord(UnoGameState gs) {
        super(gs);
        playerDecks = gs.playerDecks;
        drawDeck = gs.drawDeck;
        discardDeck = gs.discardDeck;
        currentCard = gs.currentCard;
        currentColor = gs.currentColor;
        playerScore = gs.playerScore;
        expulsionRound = gs.expulsionRound;

        // Turn order data
        skipTurn = gs.skipTurn;
        direction = gs.direction;
    }

    @SuppressWarnings("unused") // for JSON
    private UnoGameStateRecord() {
    }

    @Override
    protected void restoreGame(AbstractGameState state) {
        UnoGameState uno = (UnoGameState) state;
        uno.playerDecks = new ArrayList<>(playerDecks);
        uno.drawDeck = drawDeck;
        uno.discardDeck = discardDeck;
        uno.currentCard = currentCard;
        uno.currentColor = currentColor;
        uno.playerScore = playerScore.clone();
        uno.expulsionRound = expulsionRound.clone();
        uno.skipTurn = skipTurn;
        uno.direction = direction;
    }
}
