package evaluation.jsonBag;

import com.fasterxml.jackson.databind.node.ObjectNode;
import evaluation.records.SavedStates;
import players.jsonBagPlayers.Tokenizer;
import utilities.Json;
import utilities.Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.*;

/**
 * Tokenizes saved trajectories, so prototypes and feature lists can be built from bags made exactly as the
 * JSON-Bag player makes them, with or without the structuring pass.
 * <p>
 * Arguments (key=value):
 * <ul>
 *     <li>{@code input} - a game's saved states ({@code G<gameID>.jsonl}, as written by
 *     {@link evaluation.listeners.GameStateSnapshotter}), or a directory searched for them.</li>
 *     <li>{@code output} - the JSON Lines file to write: one line per state, with the game file, tick, player,
 *     trigger and bag.</li>
 *     <li>{@code structured} - {@code true} to tokenize after the structuring pass (default {@code false}).</li>
 * </ul>
 */
public class TrajectoryBags {

    public static void main(String[] args) {
        String input = Utils.getArg(args, "input", "");
        String output = Utils.getArg(args, "output", "");
        boolean structured = Utils.getArg(args, "structured", false);
        if (input.isEmpty() || output.isEmpty()) {
            System.out.println("Usage: input=<file-or-dir> output=<file.jsonl> [structured=false]");
            return;
        }
        int written = write(gameFiles(new File(input)), new File(output), structured);
        System.out.println("Wrote " + written + " bags to " + output);
    }

    /** The {@code .jsonl} files at or under the path, in name order. */
    static List<File> gameFiles(File path) {
        if (path.isFile()) return List.of(path);
        try (var files = Files.walk(path.toPath())) {
            return files.map(java.nio.file.Path::toFile)
                    .filter(f -> f.isFile() && f.getName().endsWith(".jsonl"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Writes one line per saved state and returns how many. */
    static int write(List<File> gameFiles, File output, boolean structured) {
        int count = 0;
        try (BufferedWriter writer = Files.newBufferedWriter(output.toPath())) {
            for (File gameFile : gameFiles) {
                for (SavedStates.Entry entry : SavedStates.entries(gameFile)) {
                    Map<String, Integer> bag = structured
                            ? Tokenizer.tokenizeStructured(entry.state(), null, false)
                            : Tokenizer.tokenize(entry.state(), "", true, "both", true, false, false, false, null, false);
                    ObjectNode line = Json.object();
                    line.put("game", gameFile.getPath());
                    line.put("tick", entry.tick());
                    line.put("player", entry.player());
                    if (entry.trigger() != null) line.put("trigger", entry.trigger());
                    line.set("bag", Json.toTree(new TreeMap<>(bag)));
                    writer.write(line.toString());
                    writer.newLine();
                    count++;
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write " + output, e);
        }
        return count;
    }
}
