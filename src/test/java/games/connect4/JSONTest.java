package games.connect4;

import core.CoreConstants;
import core.actions.AbstractAction;
import core.actions.SetGridValueAction;
import games.GameType;
import com.fasterxml.jackson.databind.JsonNode;
import core.AbstractGameState;
import core.AbstractGameStateRecord;
import evaluation.records.GameStateRecordFactory;
import evaluation.records.SavedStates;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import utilities.Json;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.*;

/** A Connect4 state is saved as its {@link Connect4GameStateRecord} and loaded back unchanged. */
public class JSONTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private Connect4ForwardModel fm = new Connect4ForwardModel();

    /**
     * Plays the given columns in order, alternating players, returning the resulting state.
     */
    private Connect4GameState playGame(Connect4GameParameters params, int... columns) {
        Connect4GameState state = (Connect4GameState) GameType.Connect4.createGameState(params, 2);
        fm.setup(state);
        for (int col : columns) {
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            AbstractAction chosen = actions.get(0);
            for (AbstractAction a : actions) {
                if (a instanceof SetGridValueAction sg && sg.getX() == col) {
                    chosen = a;
                    break;
                }
            }
            fm.next(state, chosen);
        }
        return state;
    }

    @Test
    public void testJSON() {
        Connect4GameParameters params = new Connect4GameParameters();
        Connect4GameState state = playGame(params, 0, 1, 2, 3, 0, 1);

        state.setFirstPlayer(1);
        state.setTurnOwner(0);
        state.setGameStatus(CoreConstants.GameResult.GAME_ONGOING);

        JsonNode json = SavedStates.toJson(state);
        assertEquals(Connect4GameStateRecord.class.getName(), json.get("class").asText());
        assertEquals("Connect4", json.get("gameType").asText());
        assertEquals(1, json.get("firstPlayer").asInt());
        assertEquals(0, json.get("turnOwner").asInt());
        assertEquals(params.width, json.get("gridBoard").get("width").asInt());
        assertEquals(params.height, json.get("gridBoard").get("height").asInt());

        // Restore into a freshly set up state, as the loader does
        Connect4GameState newState = (Connect4GameState) GameType.Connect4.createGameState(params, 2);
        fm.setup(newState);
        AbstractGameStateRecord.read(json.toString()).restore(newState);

        assertEquals(state.getFirstPlayer(), newState.getFirstPlayer());
        assertEquals(state.getTurnOwner(), newState.getTurnOwner());
        assertEquals(state.getGameStatus(), newState.getGameStatus());
        assertEquals(state.getGameTick(), newState.getGameTick());
        assertEquals(state.getGridBoard(), newState.getGridBoard());
        assertEquals(state, newState);
    }

    @Test
    public void testJSONLoadFromFile() throws Exception {
        Connect4GameParameters params = new Connect4GameParameters();
        Connect4GameState state = playGame(params, 3, 3, 4, 2, 5, 1);
        state.setGameStatus(CoreConstants.GameResult.GAME_ONGOING);

        File file = tmp.newFile("connect4State.json");
        Files.writeString(file.toPath(), GameStateRecordFactory.createRecord(state).toJson());
        AbstractGameState loadedState = SavedStates.load(file);

        assertEquals(state, loadedState);
        // identity of the board (componentID) and every cell must survive the round-trip
        assertEquals(state.getGridBoard().getComponentID(), ((Connect4GameState) loadedState).getGridBoard().getComponentID());
    }

    @Test
    public void parametersAreReadFromFile() throws Exception {
        // Use non-default parameters to confirm they are serialized and restored rather than the defaults
        Connect4GameParameters params = new Connect4GameParameters();
        params.setParameterValue("width", 8);
        params.setParameterValue("height", 7);
        params.setParameterValue("winCount", 5);

        Connect4GameState state = playGame(params, 0, 1, 0, 1);

        File file = tmp.newFile("connect4Params.json");
        Files.writeString(file.toPath(), Json.prettyWriter().writeValueAsString(SavedStates.toJson(state)));
        AbstractGameState loadedState = SavedStates.load(file);
        Connect4GameParameters loadedParams = (Connect4GameParameters) loadedState.getGameParameters();

        assertEquals(8, loadedParams.width);
        assertEquals(7, loadedParams.height);
        assertEquals(5, loadedParams.winCount);
        assertEquals(state, loadedState);
    }
}
