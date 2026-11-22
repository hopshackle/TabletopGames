package games.dotsboxes;

import core.AbstractGameState;
import core.AbstractGameStateRecord;

import java.util.HashMap;
import java.util.Map;

/**
 * Cells and edges are written by name (their position), as keys of the owner maps; the board itself comes from
 * setup, and a restored state looks its cells and edges up there.
 */
public class DBGameStateRecord extends AbstractGameStateRecord {
    final int[] nCellsPerPlayer;
    final Map<String, Integer> cellToOwnerMap;  // Mapping from each cell to its owner, if complete
    final Map<String, Integer> edgeToOwnerMap;  // Mapping from each edge to its owner, if placed
    final boolean lastActionDidNotScore;

    public DBGameStateRecord(DBGameState gs) {
        super(gs);
        nCellsPerPlayer = gs.nCellsPerPlayer;
        cellToOwnerMap = byName(gs.cellToOwnerMap);
        edgeToOwnerMap = byName(gs.edgeToOwnerMap);
        lastActionDidNotScore = gs.lastActionDidNotScore;
    }

    @SuppressWarnings("unused") // for JSON
    private DBGameStateRecord() {
        nCellsPerPlayer = null;
        cellToOwnerMap = edgeToOwnerMap = null;
        lastActionDidNotScore = false;
    }

    private static Map<String, Integer> byName(Map<?, Integer> owners) {
        Map<String, Integer> result = new HashMap<>();
        owners.forEach((k, v) -> result.put(k.toString(), v));
        return result;
    }

    private static <T> HashMap<T, Integer> resolve(Map<String, Integer> owners, Iterable<T> all) {
        Map<String, T> byName = new HashMap<>();
        all.forEach(t -> byName.put(t.toString(), t));
        HashMap<T, Integer> result = new HashMap<>();
        owners.forEach((name, owner) -> {
            T t = byName.get(name);
            if (t == null) throw new IllegalArgumentException("No cell or edge " + name + " on this board");
            result.put(t, owner);
        });
        return result;
    }

    @Override
    protected void restoreGame(AbstractGameState state) {
        DBGameState db = (DBGameState) state;
        db.nCellsPerPlayer = nCellsPerPlayer.clone();
        db.cellToOwnerMap = resolve(cellToOwnerMap, db.cells);
        db.edgeToOwnerMap = resolve(edgeToOwnerMap, db.edges);
        db.lastActionDidNotScore = lastActionDidNotScore;
    }
}
