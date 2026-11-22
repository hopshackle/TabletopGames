package games.connect4;

import core.AbstractGameState;
import core.AbstractGameStateRecord;
import core.components.BoardNode;
import core.components.GridBoard;
import utilities.Pair;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class Connect4GameStateRecord extends AbstractGameStateRecord {
    final GridBoard gridBoard;
    final LinkedList<Pair<Integer, Integer>> winnerCells;

    public Connect4GameStateRecord(Connect4GameState gs) {
        super(gs);
        gridBoard = gs.gridBoard;
        winnerCells = gs.winnerCells;
    }

    @SuppressWarnings("unused") // for JSON
    private Connect4GameStateRecord() {
        gridBoard = null;
        winnerCells = null;
    }

    /**
     * Player pieces are the shared {@link Connect4Constants#playerMapping} markers, found by name, as their IDs
     * depend on the order in which classes were loaded. Other cells are copied, keeping their IDs.
     */
    @Override
    protected void restoreGame(AbstractGameState state) {
        Connect4GameState c4 = (Connect4GameState) state;
        Map<Integer, BoardNode> copies = new HashMap<>();
        BoardNode[][] grid = new BoardNode[gridBoard.getHeight()][gridBoard.getWidth()];
        for (int y = 0; y < grid.length; y++) {
            for (int x = 0; x < grid[y].length; x++) {
                BoardNode cell = gridBoard.getElement(x, y);
                if (cell == null) continue;
                grid[y][x] = Connect4Constants.playerMapping.stream()
                        .filter(marker -> marker.getComponentName().equals(cell.getComponentName()))
                        .findFirst()
                        .orElseGet(() -> copies.computeIfAbsent(cell.getComponentID(), id -> cell.copy()));
            }
        }
        c4.gridBoard = new GridBoard(grid, gridBoard.getComponentID());
        c4.winnerCells = new LinkedList<>(winnerCells);
    }
}
