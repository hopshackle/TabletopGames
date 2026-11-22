package games.backgammon;

import com.fasterxml.jackson.databind.JsonNode;
import core.AbstractGameState;
import core.AbstractGameStateRecord;
import core.CoreConstants;
import evaluation.records.SavedStates;
import games.GameType;
import games.XIIScripta.XIIParameters;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import utilities.Json;

import java.io.File;
import java.nio.file.Files;

import static org.junit.Assert.*;

/** A Backgammon or XII Scripta state is saved as its {@link BGGameStateRecord} and loaded back unchanged. */
public class JSONTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void testJSON() {
        BGParameters params = new BGParameters();
        BGGameState state = new BGGameState(params, 2);
        BGForwardModel fm = new BGForwardModel();
        fm.setup(state);
        state.rollDice();

        state.setFirstPlayer(1);
        state.setTurnOwner(0);
        state.setGameStatus(CoreConstants.GameResult.GAME_ONGOING);

        JsonNode json = SavedStates.toJson(state);
        assertEquals(BGGameStateRecord.class.getName(), json.get("class").asText());
        assertEquals("Backgammon", json.get("gameType").asText());
        assertEquals(1, json.get("firstPlayer").asInt());
        assertEquals(0, json.get("turnOwner").asInt());

        // Restore into a freshly set up state
        BGGameState newState = new BGGameState(params, 2);
        fm.setup(newState);
        AbstractGameStateRecord.read(json.toString()).restore(newState);

        assertEquals(state.getGameID(), newState.getGameID());
        assertEquals(state.getFirstPlayer(), newState.getFirstPlayer());
        assertEquals(state.getTurnOwner(), newState.getTurnOwner());
        assertEquals(state.getGameStatus(), newState.getGameStatus());
        assertEquals(state.getGameTick(), newState.getGameTick());
        assertEquals(state, newState);
    }

    @Test
    public void testJSONLoadFromFile() throws Exception {
        BGParameters params = new XIIParameters();
        BGGameState state = (BGGameState) GameType.XIIScripta.createGameState(params, 2);
        BGForwardModel fm = new BGForwardModel();
        fm.setup(state);
        state.rollDice();

        // Use reflection to call setGameID()
        java.lang.reflect.Method setGameIDMethod = AbstractGameState.class.getDeclaredMethod("setGameID", int.class);
        setGameIDMethod.setAccessible(true);
        setGameIDMethod.invoke(state, 1234);

        state.setFirstPlayer(1);
        state.setTurnOwner(0);
        state.setGameStatus(CoreConstants.GameResult.GAME_ONGOING);

        File file = tmp.newFile("bgState.json");
        Files.writeString(file.toPath(), Json.prettyWriter().writeValueAsString(SavedStates.toJson(state)));
        assertEquals("XII Scripta shares BGGameState, so it is named by its parameters",
                GameType.XIIScripta, SavedStates.entries(file).get(0).gameType());
        AbstractGameState loadedState = SavedStates.load(file);
        assertEquals(state, loadedState);
        assertEquals(1234, loadedState.getGameID());
    }
}
