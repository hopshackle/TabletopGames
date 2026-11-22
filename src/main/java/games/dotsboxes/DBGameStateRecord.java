package games.dotsboxes;

import core.AbstractGameState;
import core.AbstractGameStateRecord;

import java.util.HashMap;
import java.util.HashSet;

public class DBGameStateRecord extends AbstractGameStateRecord {
    // Mutable state:
    int[] nCellsPerPlayer;
    HashMap<DBCell, Integer> cellToOwnerMap;  // Mapping from each cell to its owner, if complete
    HashMap<DBEdge, Integer> edgeToOwnerMap;  // Mapping from each edge to its owner, if placed
    public DBGameStateRecord(DBGameState gs) {
        super(gs);
        nCellsPerPlayer = gs.nCellsPerPlayer;
        cellToOwnerMap = gs.cellToOwnerMap;  // Mapping from each cell to its owner, if complete
        edgeToOwnerMap = gs.edgeToOwnerMap;  // Mapping from each edge to its owner, if placed
    }
}
