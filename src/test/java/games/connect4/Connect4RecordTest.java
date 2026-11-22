package games.connect4;

import core.AbstractGameState;
import core.AbstractGameStateRecord;
import core.AbstractForwardModel;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import evaluation.records.GameStateRecordFactory;
import games.GameType;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * A Connect4 state written as a record and read back is equal to the original, and plays on identically.
 */
public class Connect4RecordTest {

    private static Game playedGame(int moves, long seed) {
        Game game = GameType.Connect4.createGameInstance(2, seed);
        game.reset(List.of(new RandomPlayer(new Random(1)), new RandomPlayer(new Random(2))));
        AbstractGameState state = game.getGameState();
        Random rnd = new Random(seed);
        for (int i = 0; i < moves && state.isNotTerminal(); i++) {
            List<AbstractAction> actions = game.getForwardModel().computeAvailableActions(state);
            game.getForwardModel().next(state, actions.get(rnd.nextInt(actions.size())));
        }
        return game;
    }

    private static AbstractGameState roundTrip(AbstractGameState state) {
        String json = GameStateRecordFactory.createRecord(state).toJson();
        return AbstractGameStateRecord.read(json).toState();
    }

    @Test
    public void aMidGameStateRoundTrips() {
        AbstractGameState original = playedGame(9, 42).getGameState();
        AbstractGameState restored = roundTrip(original);
        assertEquals(original, restored);
        Connect4GameState c4 = (Connect4GameState) original, back = (Connect4GameState) restored;
        for (int x = 0; x < c4.gridBoard.getWidth(); x++)
            for (int y = 0; y < c4.gridBoard.getHeight(); y++)
                assertEquals(c4.getPlayerAt(x, y), back.getPlayerAt(x, y));
    }

    @Test
    public void aFinishedGameRoundTripsWithItsWinningLine() {
        AbstractGameState original = playedGame(100, 7).getGameState();
        assertFalse(original.isNotTerminal());
        Connect4GameState back = (Connect4GameState) roundTrip(original);
        assertEquals(original, back);
        assertEquals(((Connect4GameState) original).winnerCells, back.winnerCells);
        assertArrayEquals(original.getPlayerResults(), back.getPlayerResults());
    }

    @Test
    public void theRestoredStatePlaysOnIdentically() {
        Game game = playedGame(6, 3);
        AbstractGameState original = game.getGameState();
        AbstractGameState restored = roundTrip(original);
        AbstractForwardModel fm = game.getForwardModel();
        Random rnd = new Random(11);
        while (original.isNotTerminal()) {
            List<AbstractAction> actions = fm.computeAvailableActions(original);
            assertEquals(actions, fm.computeAvailableActions(restored));
            AbstractAction chosen = actions.get(rnd.nextInt(actions.size()));
            fm.next(original, chosen.copy());
            fm.next(restored, chosen.copy());
            assertEquals(original, restored);
        }
        assertEquals(CoreConstants.GameResult.GAME_END, restored.getGameStatus());
    }

    @Test
    public void tunableParametersAreRestored() {
        Connect4GameParameters params = new Connect4GameParameters();
        params.setParameterValue("width", 8);
        Game game = GameType.Connect4.createGameInstance(2, 5L, params);
        AbstractGameState back = roundTrip(game.getGameState());
        assertEquals(game.getGameState().getGameParameters(), back.getGameParameters());
        assertEquals(8, ((Connect4GameState) back).gridBoard.getWidth());
    }

    @Test
    public void componentsCreatedAfterLoadingGetNewIds() {
        AbstractGameState original = playedGame(4, 9).getGameState();
        Connect4GameState back = (Connect4GameState) roundTrip(original);
        int boardId = back.gridBoard.getComponentID();
        assertTrue(new core.components.Token("t").getComponentID() > boardId);
    }
}
