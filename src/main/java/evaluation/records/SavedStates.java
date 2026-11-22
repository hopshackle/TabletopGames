package evaluation.records;

import com.fasterxml.jackson.databind.JsonNode;
import core.AbstractGameState;
import core.AbstractGameStateRecord;
import games.GameType;
import utilities.Json;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads saved game states. A saved state is the JSON of a game state's {@link AbstractGameStateRecord}, identified by
 * its {@code "class"}.
 * <p>
 * States are saved one game per file, as JSON Lines ({@code G<gameID>.jsonl}, written by
 * {@link evaluation.listeners.GameStateSnapshotter}): each line is an {@link Entry} with the tick, the player, what
 * triggered the snapshot, and the state. A {@code .json} file holding a single state is read as one entry.
 */
public final class SavedStates {

    private SavedStates() {
    }

    /**
     * One saved state.
     *
     * @param tick    the game tick of the state
     * @param player  the player about to act, or -1 if not known
     * @param trigger what caused the save: an event name such as {@code TURN_OVER}, or {@code ACTION} for an
     *                action flagged to be saved
     * @param state   the state's JSON
     */
    public record Entry(int tick, int player, String trigger, JsonNode state) {
        public AbstractGameState toState() {
            return SavedStates.toState(state);
        }

        public GameType gameType() {
            return SavedStates.gameType(state);
        }
    }

    /** True if the state can be saved: its game has a record. */
    public static boolean canSave(AbstractGameState state) {
        return GameStateRecordFactory.hasRecord(state);
    }

    /** The state's JSON: its record. */
    public static JsonNode toJson(AbstractGameState state) {
        if (!canSave(state))
            throw new IllegalArgumentException("A " + state.getGameType() + " state cannot be saved: the game has "
                    + "no record in GameStateRecordFactory");
        return Json.toTree(GameStateRecordFactory.createRecord(state));
    }

    /** The state that the JSON describes. */
    public static AbstractGameState toState(JsonNode json) {
        checkIsRecord(json);
        return AbstractGameStateRecord.read(json.toString()).toState();
    }

    /** The game the JSON is a state of, as its record names it. */
    public static GameType gameType(JsonNode json) {
        checkIsRecord(json);
        return GameType.valueOf(json.get("gameType").asText());
    }

    private static void checkIsRecord(JsonNode json) {
        String name = json.path("class").asText(null);
        if (name == null) throw new IllegalArgumentException("A saved state needs a \"class\"");
        try {
            if (!AbstractGameStateRecord.class.isAssignableFrom(Class.forName(name)))
                throw new IllegalArgumentException(name + " is not a game state record");
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unknown class " + name + " in saved state", e);
        }
    }

    /** The entries in a {@code .jsonl} file, in order, or the single state in a {@code .json} file. */
    public static List<Entry> entries(File file) {
        try {
            List<Entry> entries = new ArrayList<>();
            if (file.getName().endsWith(".jsonl")) {
                for (String line : Files.readAllLines(file.toPath())) {
                    if (line.isBlank()) continue;
                    JsonNode node = Json.parse(line);
                    entries.add(new Entry(node.get("tick").asInt(), node.path("player").asInt(-1),
                            node.path("trigger").asText(null), node.get("state")));
                }
            } else {
                JsonNode state = Json.mapper().readTree(file);
                entries.add(new Entry(state.path("tick").asInt(), -1, null, state));
            }
            return entries;
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read saved states from " + file, e);
        }
    }

    /** The last state in the file. */
    public static AbstractGameState load(File file) {
        List<Entry> entries = entries(file);
        if (entries.isEmpty()) throw new IllegalArgumentException(file + " holds no saved state");
        return entries.get(entries.size() - 1).toState();
    }

    /** The state at the given tick (the last one saved at that tick, if there are several). */
    public static AbstractGameState load(File file, int tick) {
        List<Entry> entries = entries(file);
        for (int i = entries.size() - 1; i >= 0; i--)
            if (entries.get(i).tick() == tick) return entries.get(i).toState();
        throw new IllegalArgumentException(file + " holds no state at tick " + tick);
    }

    /** Loads {@code path}, or {@code path#tick} for the state at a given tick. */
    public static AbstractGameState load(String spec) {
        int hash = spec.lastIndexOf('#');
        if (hash < 0) return load(new File(spec));
        return load(new File(spec.substring(0, hash)), Integer.parseInt(spec.substring(hash + 1)));
    }
}
