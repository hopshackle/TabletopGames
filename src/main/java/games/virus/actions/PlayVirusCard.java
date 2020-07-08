package games.virus.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DrawCard;
import games.virus.VirusGameState;
import games.virus.cards.VirusCard;
import games.virus.components.VirusBody;

import java.util.Objects;
import java.util.Random;

public class PlayVirusCard extends DrawCard {
    public int bodyId;

    public PlayVirusCard(int deckFrom, int deckTo, int fromIndex, int bodyId) {
        super(deckFrom, deckTo, fromIndex);
        this.bodyId = bodyId;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        VirusGameState vgs = (VirusGameState) gs;
        int playerId = vgs.getCurrentPlayer();
        super.execute(gs);

        VirusCard card = (VirusCard) getCard(gs);
        VirusBody body = (VirusBody) vgs.getComponentById(bodyId);

        if (vgs.getDrawDeck().getSize() == 0)
            discardToDraw(vgs);

        VirusCard newCard = vgs.getDrawDeck().draw();
        vgs.getPlayerDecks().get(playerId).add(newCard);

        return true;
    }

    public VirusBody getBody(AbstractGameState vgs) {
        return (VirusBody) vgs.getComponentById(bodyId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        PlayVirusCard that = (PlayVirusCard) o;
        return bodyId == that.bodyId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), bodyId);
    }

    @Override
    public void printToConsole() {
        System.out.println("Play virus card");
    }

    // Move all cards from discard deck to draw one and shuffle
    // TODO: check it
    public void discardToDraw(VirusGameState vgs) {
        while (vgs.getDiscardDeck().getSize()>0) {
            VirusCard card = vgs.getDiscardDeck().pick();
            vgs.getDrawDeck().add(card);
        }
        vgs.getDrawDeck().shuffle(new Random(vgs.getGameParameters().getRandomSeed()));
    }

    @Override
    public AbstractAction copy() {
        return new PlayVirusCard(deckFrom, deckTo, fromIndex, bodyId);
    }
}