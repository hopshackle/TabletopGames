package games.cantstop;

import core.AbstractGameState;
import core.AbstractGameStateRecord;
import core.components.Dice;

import java.util.List;
import java.util.Map;

public class CantStopGameStateRecord extends AbstractGameStateRecord {

    protected boolean[] completedColumns;
    protected int[][] playerMarkerPositions;
    protected List<Dice> dice;

    public CantStopGameStateRecord(CantStopGameState gs) {
        super(gs);
        completedColumns = gs.completedColumns;
        playerMarkerPositions = gs.playerMarkerPositions;
        dice = gs.dice;
    }
}
