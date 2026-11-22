package games.explodingkittens;

import core.AbstractGameState;
import core.AbstractGameStateRecord;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.explodingkittens.cards.ExplodingKittensCard;

import java.util.ArrayList;
import java.util.List;

public class ExplodingKittensGameStateRecord extends AbstractGameStateRecord {
    final List<PartialObservableDeck<ExplodingKittensCard>> playerHandCards;
    final PartialObservableDeck<ExplodingKittensCard> drawPile;
    final Deck<ExplodingKittensCard> discardPile;
    final Deck<ExplodingKittensCard> inPlay;
    final int currentPlayerTurnsLeft;
    final int nextAttackLevel;
    final boolean skip;
    final int[] orderOfPlayerDeath;

    public ExplodingKittensGameStateRecord(ExplodingKittensGameState gs) {
        super(gs);
        playerHandCards = gs.playerHandCards;
        drawPile = gs.drawPile;
        discardPile = gs.discardPile;
        inPlay = gs.inPlay;
        currentPlayerTurnsLeft = gs.currentPlayerTurnsLeft;
        nextAttackLevel = gs.nextAttackLevel;
        skip = gs.skip;
        orderOfPlayerDeath = gs.orderOfPlayerDeath;
    }

    @SuppressWarnings("unused") // for JSON
    private ExplodingKittensGameStateRecord() {
        playerHandCards = null;
        drawPile = null;
        discardPile = inPlay = null;
        currentPlayerTurnsLeft = nextAttackLevel = 0;
        skip = false;
        orderOfPlayerDeath = null;
    }

    @Override
    protected void restoreGame(AbstractGameState state) {
        ExplodingKittensGameState ek = (ExplodingKittensGameState) state;
        ek.playerHandCards = new ArrayList<>(playerHandCards);
        ek.drawPile = drawPile;
        ek.discardPile = discardPile;
        ek.inPlay = inPlay;
        ek.currentPlayerTurnsLeft = currentPlayerTurnsLeft;
        ek.nextAttackLevel = nextAttackLevel;
        ek.skip = skip;
        ek.orderOfPlayerDeath = orderOfPlayerDeath.clone();
    }
}
