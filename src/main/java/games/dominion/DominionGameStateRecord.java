package games.dominion;

import core.AbstractGameState;
import core.AbstractGameStateRecord;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;

import java.util.HashMap;
import java.util.Map;

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
    public DominionGameStateRecord(DominionGameState gs) {
        super(gs);
        cardsIncludedInGame = gs.cardsIncludedInGame;
        playerHands = gs.playerHands;
        playerDrawPiles = gs.playerDrawPiles;
        playerDiscards = gs.playerDiscards;
        playerTableaux = gs.playerTableaux;
        trashPile = gs.trashPile;
        defenceStatus = gs.defenceStatus;
    }
}
