package evaluation.listeners;

import com.fasterxml.jackson.databind.node.ObjectNode;
import core.AbstractGameState;
import core.Game;
import core.interfaces.IGameEvent;
import evaluation.metrics.Event;
import evaluation.records.GameStateRecordFactory;
import evaluation.records.SavedStates;
import utilities.Json;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;

/**
 * Saves game states, one file per game: {@code <directory>/<GameType>/G<gameID>.jsonl}, one JSON line per state
 * (see {@link SavedStates.Entry}). The state is the true state, written as its record (or, for a game without one
 * yet, its {@code IToJSON} form).
 * <p>
 * It saves on two triggers, both into the same file:
 * <ul>
 *     <li>As a listener, at chosen events. A configuration only needs the class, since the runners
 *     ({@code RunGames}, {@code ExpertIteration}, tournaments) give it their {@code destDir}, and states go to
 *     {@code <destDir>/SavedStates}:
 *     <pre>{@code { "class": "evaluation.listeners.GameStateSnapshotter" }}</pre>
 *     Events default to {@code TURN_OVER} plus {@code GAME_OVER}. To choose others, pass their names:
 *     <pre>{@code { "class": "evaluation.listeners.GameStateSnapshotter", "args": [ ["ROUND_OVER", "TURN_OVER"] ] }}</pre>
 *     {@code GAME_OVER} is always included. A game that cannot be saved fails on the first event.</li>
 *     <li>From {@link Game}, before an action flagged with {@code setSaveGame(true)} (by an adviser, the MCTS decision
 *     recorder or MCTS itself), into the game's saved-state directory, with trigger {@code ACTION}.</li>
 * </ul>
 * The first state saved for a game in a run replaces any file of the same name from an earlier run.
 */
public class GameStateSnapshotter implements IGameListener {

    /** The directory under a runner's {@code destDir} that states are saved to. */
    public static final String DIRECTORY = "SavedStates";
    public static final String ACTION_TRIGGER = "ACTION";

    private static final Set<IGameEvent> DEFAULT_EVENTS =
            Collections.unmodifiableSet(EnumSet.of(Event.GameEvent.TURN_OVER, Event.GameEvent.GAME_OVER));

    // Files started in this run, by any snapshotter: later states are appended to them
    private static final Set<File> started = new HashSet<>();

    private final Set<IGameEvent> events;
    private Game game;
    private File directory;

    public GameStateSnapshotter() {
        this.events = new LinkedHashSet<>(DEFAULT_EVENTS);
    }

    public GameStateSnapshotter(String[] eventNames) {
        Set<IGameEvent> chosen = new LinkedHashSet<>();
        for (String name : eventNames) {
            chosen.add(Event.GameEvent.valueOf(name.trim().toUpperCase()));
        }
        chosen.add(Event.GameEvent.GAME_OVER);
        this.events = chosen;
    }

    /** A snapshotter that saves under the given directory and listens to no events. */
    public static GameStateSnapshotter into(String directory) {
        GameStateSnapshotter snapshotter = new GameStateSnapshotter(new String[0]);
        snapshotter.directory = new File(directory);
        snapshotter.events.clear();
        return snapshotter;
    }

    @Override
    public boolean setOutputDirectory(String... nestedDirectories) {
        directory = new File(String.join(File.separator, nestedDirectories), DIRECTORY);
        return true;
    }

    @Override
    public void setGame(Game game) {
        this.game = game;
    }

    @Override
    public Game getGame() {
        return game;
    }

    @Override
    public void onEvent(Event event) {
        if (event == null || event.state == null || !events.contains(event.type)) {
            return;
        }
        save(event.state, event.type.name(), event.playerID);
    }

    /** The file the game's states are saved to. */
    public File fileFor(AbstractGameState state) {
        return new File(new File(directory, state.getGameType().name()), "G" + state.getGameID() + ".jsonl");
    }

    /**
     * Appends the state to its game's file.
     *
     * @param trigger what caused the save: an event name, or {@link #ACTION_TRIGGER}
     * @param player  the player about to act, or -1
     */
    public void save(AbstractGameState state, String trigger, int player) {
        if (directory == null) {
            throw new IllegalStateException("GameStateSnapshotter has no output directory. Run it through "
                    + "RunGames (which sets one from destDir), or call setOutputDirectory on it first.");
        }
        if (!SavedStates.canSave(state)) {
            throw new IllegalStateException("GameStateSnapshotter cannot save " + state.getGameType()
                    + ": no record is registered for it in " + GameStateRecordFactory.class.getSimpleName()
                    + " and its state does not implement IToJSON.");
        }
        ObjectNode line = Json.object();
        line.put("tick", state.getGameTick());
        line.put("player", player);
        line.put("trigger", trigger);
        line.set("state", SavedStates.toJson(state));

        File file = fileFor(state);
        boolean append;
        synchronized (started) {
            append = !started.add(file);
        }
        if (!append && !file.getParentFile().isDirectory() && !file.getParentFile().mkdirs()) {
            throw new UncheckedIOException(new IOException("Cannot create directory " + file.getParentFile()));
        }
        try (FileWriter writer = new FileWriter(file, append)) {
            writer.write(line.toString());
            writer.write(System.lineSeparator());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save game state to " + file, e);
        }
    }

    @Override
    public void report() {
        // nothing to aggregate: each state is saved as the game runs
    }

    @Override
    public String toString() {
        return "GameStateSnapshotter" + Arrays.toString(events.toArray());
    }
}
