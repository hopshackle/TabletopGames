package evaluation.jsonBag;

import com.fasterxml.jackson.databind.JsonNode;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import evaluation.listeners.GameStateSnapshotter;
import evaluation.records.SavedStates;
import games.GameType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import players.jsonBagPlayers.JSONBagHeuristic;
import players.jsonBagPlayers.Tokenizer;
import players.simple.RandomPlayer;
import utilities.Json;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

import static org.junit.Assert.*;

public class TrajectoryBagsTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File playUno() {
        GameStateSnapshotter snapshotter = new GameStateSnapshotter();
        snapshotter.setOutputDirectory(tmp.getRoot().getPath());
        List<AbstractPlayer> players = List.of(new RandomPlayer(new Random(1)), new RandomPlayer(new Random(2)));
        Game.runOne(GameType.Uno, null, players, 11L, false, List.of(snapshotter), null, 0);
        return new File(tmp.getRoot(), "SavedStates");
    }

    @Test
    public void everySavedStateBecomesABagInEitherMode() throws Exception {
        File savedStates = playUno();
        List<File> games = TrajectoryBags.gameFiles(savedStates);
        assertEquals(1, games.size());
        int states = SavedStates.entries(games.get(0)).size();

        File plain = tmp.newFile("plain.jsonl"), structured = tmp.newFile("structured.jsonl");
        assertEquals(states, TrajectoryBags.write(games, plain, false));
        assertEquals(states, TrajectoryBags.write(games, structured, true));

        JsonNode plainBag = Json.parse(Files.readAllLines(plain.toPath()).get(0)).get("bag");
        JsonNode structuredBag = Json.parse(Files.readAllLines(structured.toPath()).get(0)).get("bag");
        assertTrue(plainBag.fieldNames().next().startsWith("."));
        assertTrue(structuredBag.has(".decks.Player deck[owner=0].size"));
        assertFalse(plainBag.has(".decks.Player deck[owner=0].size"));
    }

    @Test
    public void theHeuristicTokenizesAsItsPrototypesWereBuilt() {
        Game game = GameType.Uno.createGameInstance(2, 4L);
        game.reset(List.of(new RandomPlayer(new Random(1)), new RandomPlayer(new Random(2))));
        AbstractGameState state = game.getGameState();
        Map<String, Integer> structuredBag = Tokenizer.tokenizeStructured(state, null, false);

        JSONBagHeuristic heuristic = new JSONBagHeuristic(List.of(structuredBag, structuredBag));
        heuristic.setStructured(true);
        heuristic.resetCurrentBag(state);
        double value = heuristic.evaluateState(state, 0);
        assertEquals("identical prototypes give equal values", value, heuristic.copy().evaluateState(state, 1), 1e-9);
    }
}
