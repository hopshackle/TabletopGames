package games.seasaltpaper;

import core.AbstractGameStateRecord;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.seasaltpaper.cards.SeaSaltPaperCard;

import java.util.List;

public class SSPGameStateRecord extends AbstractGameStateRecord {
    public final List<PartialObservableDeck<SeaSaltPaperCard>> playerHands;
    public final List<Deck<SeaSaltPaperCard>> playerDiscards;
    public final Deck<SeaSaltPaperCard> discardPile1, discardPile2;
    public final Deck<SeaSaltPaperCard> drawPile;
    public final int lastChance; // index of the player that play "lastChance", -1 for no one
    public final boolean[] protectedHands;
    public final int[] playerTotalScores; // player points of previous rounds


    public SSPGameStateRecord(SeaSaltPaperGameState gs) {
        super(gs);
        this.playerHands = gs.getPlayerHands();
        this.playerDiscards = gs.getPlayerDiscards();
        this.discardPile1 = gs.getDiscardPile1();
        this.discardPile2 = gs.getDiscardPile2();
        this.drawPile = gs.getDrawPile();
        this.lastChance = gs.lastChance;
        this.protectedHands = gs.getProtectedHands();
        this.playerTotalScores = gs.playerTotalScores;
    }
}
