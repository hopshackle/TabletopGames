package evaluation.listeners;

import core.AbstractGameState;
import core.Game;
import evaluation.metrics.Event;
import evaluation.records.SavedStates;
import games.GameType;
import games.connect4.Connect4GameState;
import gui.Frontend;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import players.jsonBagPlayers.Tokenizer;
import players.simple.RandomPlayer;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * The snapshotter saves each game to one JSON Lines file, at chosen events and before flagged actions, and the
 * saved states load back.
 */
public class GameStateSnapshotterTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private AbstractGameState cantStop;

    @Before
    public void setup() {
        GameType gt = GameType.CantStop;
        Game game = gt.createGameInstance(2, 42L, gt.createParameters(42L));
        game.reset(List.of(new RandomPlayer(new Random(1)), new RandomPlayer(new Random(2))));
        cantStop = game.getGameState();
    }

    private GameStateSnapshotter snapshotter(String... events) {
        GameStateSnapshotter snapshotter = events.length == 0 ? new GameStateSnapshotter() : new GameStateSnapshotter(events);
        assertTrue(snapshotter.setOutputDirectory(tmp.getRoot().getAbsolutePath()));
        return snapshotter;
    }

    @Test
    public void savesAConfiguredEventAsOneLineOfTheGamesFile() {
        GameStateSnapshotter snapshotter = snapshotter();
        snapshotter.onEvent(Event.createEvent(Event.GameEvent.TURN_OVER, cantStop, 1));

        File file = new File(tmp.getRoot(), "SavedStates/CantStop/G" + cantStop.getGameID() + ".jsonl");
        assertEquals(file, snapshotter.fileFor(cantStop));
        List<SavedStates.Entry> entries = SavedStates.entries(file);
        assertEquals(1, entries.size());
        assertEquals("TURN_OVER", entries.get(0).trigger());
        assertEquals(1, entries.get(0).player());
        assertEquals(cantStop.getGameTick(), entries.get(0).tick());
        assertEquals("CantStop", entries.get(0).state().get("gameType").asText());
        assertTrue(entries.get(0).state().has("completedColumns"));
    }

    @Test
    public void ignoresEventsThatWereNotChosen() {
        GameStateSnapshotter snapshotter = snapshotter("ROUND_OVER");
        snapshotter.onEvent(Event.createEvent(Event.GameEvent.TURN_OVER, cantStop));
        snapshotter.onEvent(Event.createEvent(Event.GameEvent.ACTION_CHOSEN, cantStop));
        assertFalse(snapshotter.fileFor(cantStop).exists());

        snapshotter.onEvent(Event.createEvent(Event.GameEvent.ROUND_OVER, cantStop));
        // GAME_OVER is always saved
        snapshotter.onEvent(Event.createEvent(Event.GameEvent.GAME_OVER, cantStop));
        List<SavedStates.Entry> entries = SavedStates.entries(snapshotter.fileFor(cantStop));
        assertEquals(List.of("ROUND_OVER", "GAME_OVER"), entries.stream().map(SavedStates.Entry::trigger).toList());
    }

    @Test
    public void refusesAGameThatCannotBeSaved() {
        GameType gt = GameType.TicTacToe;
        Game game = gt.createGameInstance(2, 7L);
        game.reset(List.of(new RandomPlayer(), new RandomPlayer()));
        try {
            snapshotter().onEvent(Event.createEvent(Event.GameEvent.TURN_OVER, game.getGameState()));
            fail("expected IllegalStateException for a game without a record");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("TicTacToe"));
            assertTrue(expected.getMessage().contains("GameStateRecordFactory"));
        }
    }

    @Test
    public void explainsItselfWhenNoDirectoryWasGiven() {
        try {
            new GameStateSnapshotter().onEvent(Event.createEvent(Event.GameEvent.GAME_OVER, cantStop));
            fail("expected IllegalStateException without an output directory");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("setOutputDirectory"));
        }
    }

    @Test
    public void aGamesFileFromAnEarlierRunIsReplaced() throws Exception {
        GameStateSnapshotter snapshotter = snapshotter();
        File file = snapshotter.fileFor(cantStop);
        assertTrue(file.getParentFile().mkdirs());
        Files.writeString(file.toPath(), "left over from an earlier run\n");

        snapshotter.onEvent(Event.createEvent(Event.GameEvent.TURN_OVER, cantStop));
        snapshotter.onEvent(Event.createEvent(Event.GameEvent.GAME_OVER, cantStop));
        assertEquals(2, SavedStates.entries(file).size());
    }

    @Test
    public void savesAWholeGameToOneFileThatJsonBagCanRead() {
        GameStateSnapshotter snapshotter = snapshotter();
        Game.runOne(GameType.CantStop, null, List.of(new RandomPlayer(new Random(3)), new RandomPlayer(new Random(4))),
                99L, false, List.of(snapshotter), null, 0);

        File[] files = new File(tmp.getRoot(), "SavedStates/CantStop").listFiles();
        assertNotNull(files);
        assertEquals(1, files.length);
        List<SavedStates.Entry> entries = SavedStates.entries(files[0]);
        assertTrue("expected a trajectory, found " + entries.size(), entries.size() >= 2);
        assertEquals("GAME_OVER", entries.get(entries.size() - 1).trigger());
        for (int i = 1; i < entries.size(); i++)
            assertTrue(entries.get(i).tick() >= entries.get(i - 1).tick());
        for (SavedStates.Entry entry : entries)
            assertFalse(Tokenizer.tokenize(entry.state()).isEmpty());
    }

    /** Flags every action of the first player to be saved, as an adviser does. */
    private static class FlagFirstPlayer implements IGameListener {
        private Game game;

        @Override
        public void onEvent(Event event) {
            if (event.type == Event.GameEvent.ACTION_CHOSEN && event.playerID == 0) event.action.setSaveGame(true);
        }

        @Override
        public void report() {
        }

        @Override
        public void setGame(Game game) {
            this.game = game;
        }

        @Override
        public Game getGame() {
            return game;
        }
    }

    @Test
    public void flaggedActionsAreSavedByTheGameAndLoadBack() {
        Game game = GameType.Connect4.createGameInstance(2, 5L);
        game.setSavedStatesDirectory(new File(tmp.getRoot(), "SavedStates").getPath());
        game.addListener(new FlagFirstPlayer());
        game.reset(List.of(new RandomPlayer(new Random(5)), new RandomPlayer(new Random(6))));
        game.run();

        File file = new File(tmp.getRoot(), "SavedStates/Connect4/G" + game.getGameState().getGameID() + ".jsonl");
        List<SavedStates.Entry> entries = SavedStates.entries(file);
        assertFalse(entries.isEmpty());
        for (SavedStates.Entry entry : entries) {
            assertEquals(GameStateSnapshotter.ACTION_TRIGGER, entry.trigger());
            assertEquals(0, entry.player());
        }
        SavedStates.Entry first = entries.get(0);
        AbstractGameState state = SavedStates.load(file, first.tick());
        assertTrue(state instanceof Connect4GameState);
        assertEquals(first.tick(), state.getGameTick());

        Frontend.LoadedGameState loaded = Frontend.loadGameStateFromFile(file);
        assertEquals(GameType.Connect4, loaded.gameType());
        assertEquals(entries.get(entries.size() - 1).tick(), loaded.state().getGameTick());
    }
}
