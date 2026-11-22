package games.cantstop;

import core.AbstractGameState;
import core.AbstractGameStateRecord;
import core.components.Dice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CantStopGameStateRecord extends AbstractGameStateRecord {

    protected final boolean[] completedColumns;
    protected final int[][] playerMarkerPositions;
    protected final Map<Integer, Integer> temporaryMarkerPositions;
    protected final List<Dice> dice;

    public CantStopGameStateRecord(CantStopGameState gs) {
        super(gs);
        completedColumns = gs.completedColumns;
        playerMarkerPositions = gs.playerMarkerPositions;
        temporaryMarkerPositions = gs.temporaryMarkerPositions;
        dice = gs.dice;
    }

    @SuppressWarnings("unused") // for JSON
    private CantStopGameStateRecord() {
        completedColumns = null;
        playerMarkerPositions = null;
        temporaryMarkerPositions = null;
        dice = null;
    }

    @Override
    protected void restoreGame(AbstractGameState state) {
        CantStopGameState cs = (CantStopGameState) state;
        cs.completedColumns = completedColumns.clone();
        cs.playerMarkerPositions = new int[playerMarkerPositions.length][];
        for (int p = 0; p < playerMarkerPositions.length; p++)
            cs.playerMarkerPositions[p] = playerMarkerPositions[p].clone();
        cs.temporaryMarkerPositions = new HashMap<>(temporaryMarkerPositions);
        cs.dice = new ArrayList<>();
        for (Dice d : dice) cs.dice.add(d.copy());
    }
}
