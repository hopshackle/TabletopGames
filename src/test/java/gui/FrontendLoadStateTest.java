package gui;

import core.AbstractGameState;
import core.CoreConstants;
import evaluation.optimisation.TunableParameters;
import games.GameType;
import games.XIIScripta.XIIParameters;
import games.backgammon.BGForwardModel;
import games.backgammon.BGGameState;
import games.backgammon.BGParameters;
import gui.Frontend.LoadedGameState;
import com.fasterxml.jackson.databind.node.ObjectNode;
import evaluation.records.SavedStates;
import org.junit.Test;
import utilities.Json;

import java.io.File;
import java.nio.file.Files;

import static org.junit.Assert.*;

/**
 * Tests for {@link Frontend#loadGameStateFromFile(File)}, the logic behind the GUI's
 * "Load game state" file chooser. Each test round-trips a state through its record,
 * writes it to a temporary file, and reloads it - self-contained, so no external fixtures
 * are required.
 */
public class FrontendLoadStateTest {

    private File writeStateToTempFile(AbstractGameState state) throws Exception {
        File file = File.createTempFile("tagState", ".json");
        file.deleteOnExit();
        Files.writeString(file.toPath(), Json.prettyWriter().writeValueAsString(SavedStates.toJson(state)));
        return file;
    }

    @Test
    public void loadsBackgammonState() throws Exception {
        BGGameState state = new BGGameState(new BGParameters(), 2);
        BGForwardModel fm = new BGForwardModel();
        fm.setup(state);
        state.rollDice();
        state.setGameStatus(CoreConstants.GameResult.GAME_ONGOING);

        File file = writeStateToTempFile(state);
        LoadedGameState loaded = Frontend.loadGameStateFromFile(file);

        assertEquals(GameType.Backgammon, loaded.gameType());
        assertEquals(2, loaded.nPlayers());
        assertTrue(loaded.state() instanceof BGGameState);
        assertEquals(state, loaded.state());
    }

    @Test
    public void disambiguatesXIIScriptaByParametersClass() throws Exception {
        // XII Scripta shares BGGameState with Backgammon, so the game type can only be told apart
        // by the parameters class (BGGameState._getGameType() always reports Backgammon).
        BGParameters params = new XIIParameters();
        BGGameState state = (BGGameState) GameType.XIIScripta.createGameState(params, 2);
        BGForwardModel fm = new BGForwardModel();
        fm.setup(state);
        state.rollDice();

        File file = writeStateToTempFile(state);
        LoadedGameState loaded = Frontend.loadGameStateFromFile(file);

        assertEquals(GameType.XIIScripta, loaded.gameType());
        assertEquals(2, loaded.nPlayers());
        assertEquals(state, loaded.state());
    }

    @Test
    public void parametersAreReadFromFile() throws Exception {
        BGGameState state = new BGGameState(new BGParameters(), 2);
        new BGForwardModel().setup(state);

        File file = writeStateToTempFile(state);
        LoadedGameState loaded = Frontend.loadGameStateFromFile(file);

        // The loaded parameters should match the originals value-for-value, which is what the GUI
        // copies into its parameter controls.
        TunableParameters original = (TunableParameters) state.getGameParameters();
        TunableParameters loadedParams = (TunableParameters) loaded.state().getGameParameters();
        for (Object paramName : original.getParameterNames()) {
            String p = (String) paramName;
            assertEquals("parameter " + p, original.getParameterValue(p), loadedParams.getParameterValue(p));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownGameClass() throws Exception {
        BGGameState state = new BGGameState(new BGParameters(), 2);
        new BGForwardModel().setup(state);
        ObjectNode json = (ObjectNode) SavedStates.toJson(state);
        json.put("class", "games.nonexistent.NoSuchGameState");
        File file = File.createTempFile("tagBadState", ".json");
        file.deleteOnExit();
        Files.writeString(file.toPath(), json.toString());

        Frontend.loadGameStateFromFile(file);
    }
}
