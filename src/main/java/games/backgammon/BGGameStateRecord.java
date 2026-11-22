package games.backgammon;

import core.AbstractGameState;
import core.AbstractGameStateRecord;
import core.components.Dice;
import core.components.Token;

import java.util.ArrayList;
import java.util.List;

/** The record of a Backgammon or XII Scripta state, which share {@link BGGameState}. */
public class BGGameStateRecord extends AbstractGameStateRecord {
    final List<List<Token>> counters; // the pieces on each point
    final int[][] playerTrackMapping;
    final int[] piecesBorneOff;
    final Dice[] dice;
    final int[] availableDiceValues;
    final boolean[] diceUsed;
    final int[] blots;
    final List<Token> movedThisTurn; // pieces also in counters: read back as the same objects
    final int[] loadedDiceUsages;

    public BGGameStateRecord(BGGameState gs) {
        super(gs);
        counters = gs.counters;
        playerTrackMapping = gs.playerTrackMapping;
        piecesBorneOff = gs.piecesBorneOff;
        dice = gs.dice;
        availableDiceValues = gs.availableDiceValues;
        diceUsed = gs.diceUsed;
        blots = gs.blots;
        movedThisTurn = gs.movedThisTurn;
        loadedDiceUsages = gs.loadedDiceUsages;
    }

    @SuppressWarnings("unused") // for JSON
    private BGGameStateRecord() {
        counters = null;
        playerTrackMapping = null;
        piecesBorneOff = availableDiceValues = blots = loadedDiceUsages = null;
        dice = null;
        diceUsed = null;
        movedThisTurn = null;
    }

    // Fields that are null in the state (no dice rolled yet) are not written, and read back as null
    @Override
    protected void restoreGame(AbstractGameState state) {
        BGGameState bg = (BGGameState) state;
        bg.counters = new ArrayList<>();
        for (List<Token> point : counters) bg.counters.add(new ArrayList<>(point));
        bg.playerTrackMapping = new int[playerTrackMapping.length][];
        for (int i = 0; i < playerTrackMapping.length; i++) bg.playerTrackMapping[i] = playerTrackMapping[i].clone();
        bg.piecesBorneOff = piecesBorneOff == null ? null : piecesBorneOff.clone();
        bg.dice = dice == null ? null : dice.clone();
        bg.availableDiceValues = availableDiceValues == null ? null : availableDiceValues.clone();
        bg.diceUsed = diceUsed == null ? null : diceUsed.clone();
        bg.blots = blots == null ? null : blots.clone();
        bg.movedThisTurn = movedThisTurn == null ? new ArrayList<>() : new ArrayList<>(movedThisTurn);
        bg.loadedDiceUsages = loadedDiceUsages == null ? null : loadedDiceUsages.clone();
    }
}
