package games.seasaltpaper;

import core.AbstractGameState;
import core.AbstractGameStateRecord;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.seasaltpaper.cards.SeaSaltPaperCard;

import java.util.ArrayList;
import java.util.List;

public class SSPGameStateRecord extends AbstractGameStateRecord {
    public final List<PartialObservableDeck<SeaSaltPaperCard>> playerHands;
    public final List<Deck<SeaSaltPaperCard>> playerDiscards;
    public final Deck<SeaSaltPaperCard> discardPile1, discardPile2;
    public final Deck<SeaSaltPaperCard> drawPile;
    public final int lastChance; // index of the player that play "lastChance", -1 for no one
    public final boolean[] protectedHands;
    public final int[] playerTotalScores; // player points of previous rounds
    public final int[] playerPlayedDuoPoints;
    public final SeaSaltPaperGameState.TurnPhase currentPhase;

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
        this.playerPlayedDuoPoints = gs.playerPlayedDuoPoints;
        this.currentPhase = gs.currentPhase;
    }

    @SuppressWarnings("unused") // for JSON
    private SSPGameStateRecord() {
        playerHands = null;
        playerDiscards = null;
        discardPile1 = discardPile2 = drawPile = null;
        lastChance = -1;
        protectedHands = null;
        playerTotalScores = playerPlayedDuoPoints = null;
        currentPhase = null;
    }

    @Override
    protected void restoreGame(AbstractGameState state) {
        SeaSaltPaperGameState ssp = (SeaSaltPaperGameState) state;
        ssp.playerHands = new ArrayList<>(playerHands);
        ssp.playerDiscards = new ArrayList<>(playerDiscards);
        ssp.discardPile1 = discardPile1;
        ssp.discardPile2 = discardPile2;
        ssp.drawPile = drawPile;
        ssp.lastChance = lastChance;
        ssp.protectedHands = protectedHands.clone();
        ssp.playerTotalScores = playerTotalScores.clone();
        ssp.playerPlayedDuoPoints = playerPlayedDuoPoints.clone();
        ssp.currentPhase = currentPhase;
    }
}
