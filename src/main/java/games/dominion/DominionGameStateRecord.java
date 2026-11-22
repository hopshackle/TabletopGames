package games.dominion;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import core.AbstractGameState;
import core.AbstractGameStateRecord;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.dominion.cards.CardType;
import games.dominion.actions.IDelayedAction;
import games.dominion.cards.DominionCard;

import java.util.*;

public class DominionGameStateRecord extends AbstractGameStateRecord {
    Map<CardType, Integer> cardsIncludedInGame;
    // Then Decks for each player - Hand, Discard and Draw
    PartialObservableDeck<DominionCard>[] playerHands;
    PartialObservableDeck<DominionCard>[] playerDrawPiles;
    Deck<DominionCard>[] playerDiscards;
    Deck<DominionCard>[] playerTableaux;
    // Trash pile and other global decks
    Deck<DominionCard> trashPile;
    boolean[] defenceStatus;
    // The current player's turn
    int buysLeftForCurrentPlayer, actionsLeftForCurrentPlayer, spentSoFar, additionalSpendAvailable;
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "class")
    List<IDelayedAction> delayedActions;

    public DominionGameStateRecord(DominionGameState gs) {
        super(gs);
        cardsIncludedInGame = gs.cardsIncludedInGame;
        playerHands = gs.playerHands;
        playerDrawPiles = gs.playerDrawPiles;
        playerDiscards = gs.playerDiscards;
        playerTableaux = gs.playerTableaux;
        trashPile = gs.trashPile;
        defenceStatus = gs.defenceStatus;
        buysLeftForCurrentPlayer = gs.buysLeftForCurrentPlayer;
        actionsLeftForCurrentPlayer = gs.actionsLeftForCurrentPlayer;
        spentSoFar = gs.spentSoFar;
        additionalSpendAvailable = gs.additionalSpendAvailable;
        delayedActions = gs.delayedActions;
    }

    @SuppressWarnings("unused") // for JSON
    private DominionGameStateRecord() {
    }

    @Override
    protected void restoreGame(AbstractGameState state) {
        DominionGameState dom = (DominionGameState) state;
        // JSON writes map keys sorted; the supply's order (which orders the buy actions) is setup's
        Map<CardType, Integer> supply = new LinkedHashMap<>();
        dom.cardsIncludedInGame.keySet().forEach(card -> {
            if (cardsIncludedInGame.containsKey(card)) supply.put(card, cardsIncludedInGame.get(card));
        });
        supply.putAll(cardsIncludedInGame);
        dom.cardsIncludedInGame = supply;
        dom.playerHands = playerHands;
        dom.playerDrawPiles = playerDrawPiles;
        dom.playerDiscards = playerDiscards;
        dom.playerTableaux = playerTableaux;
        dom.trashPile = trashPile;
        dom.defenceStatus = defenceStatus.clone();
        dom.buysLeftForCurrentPlayer = buysLeftForCurrentPlayer;
        dom.actionsLeftForCurrentPlayer = actionsLeftForCurrentPlayer;
        dom.spentSoFar = spentSoFar;
        dom.additionalSpendAvailable = additionalSpendAvailable;
        dom.delayedActions = new ArrayList<>(delayedActions);
    }
}
