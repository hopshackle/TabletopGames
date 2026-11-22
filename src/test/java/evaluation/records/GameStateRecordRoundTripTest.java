package evaluation.records;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.AbstractGameStateRecord;
import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import players.simple.RandomPlayer;

import java.util.*;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

/**
 * The completeness check for every game with a record: a state written as its record and read back equals the
 * original, offers the same actions, and, given the same random numbers, plays on identically.
 */
@RunWith(Parameterized.class)
public class GameStateRecordRoundTripTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> games() {
        // -Drecords.games=CantStop,Uno runs only those games
        Set<String> only = new HashSet<>(Arrays.asList(System.getProperty("records.games", "").split(",")));
        only.remove("");
        return Arrays.stream(GameType.values())
                .filter(GameStateRecordFactory::hasRecord)
                .filter(g -> only.isEmpty() || only.contains(g.name()))
                .map(g -> new Object[]{g})
                .toList();
    }

    private final GameType gameType;

    public GameStateRecordRoundTripTest(GameType gameType) {
        this.gameType = gameType;
    }

    private Game game(long seed) {
        int nPlayers = Math.max(2, gameType.getMinPlayers());
        Game game = gameType.createGameInstance(nPlayers, seed);
        game.reset(IntStream.range(0, nPlayers).mapToObj(i -> (core.AbstractPlayer) new RandomPlayer(new Random(i))).toList());
        return game;
    }

    private static void play(Game game, int moves, Random rnd) {
        AbstractGameState state = game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        for (int i = 0; i < moves && state.isNotTerminal(); i++) {
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            fm.next(state, actions.get(rnd.nextInt(actions.size())).copy());
        }
    }

    private static AbstractGameState roundTrip(AbstractGameState state) {
        AbstractGameStateRecord record = GameStateRecordFactory.createRecord(state);
        return AbstractGameStateRecord.read(record.toJson()).toState();
    }

    private void checkRoundTrip(long seed, int moves) {
        Game game = game(seed);
        play(game, moves, new Random(seed));
        AbstractGameState original = game.getGameState();
        AbstractGameState restored = roundTrip(original);
        String where = gameType + " after " + moves + " moves (seed " + seed + ")";
        assertEquals(where + ": fields differ " + differences(original, restored), original, restored);

        AbstractForwardModel fm = game.getForwardModel();
        original.getRnd().setSeed(seed);
        restored.getRnd().setSeed(seed);
        Random choice = new Random(seed + 1);
        for (int step = 0; step < 30 && original.isNotTerminal(); step++) {
            List<AbstractAction> actions = fm.computeAvailableActions(original);
            assertEquals(where + ", step " + step + ": actions", actions, fm.computeAvailableActions(restored));
            AbstractAction chosen = actions.get(choice.nextInt(actions.size()));
            fm.next(original, chosen.copy());
            fm.next(restored, chosen.copy());
            assertEquals(where + ", step " + step + ": fields differ " + differences(original, restored), original, restored);
        }
    }

    // AbstractGameState fields that equals() deliberately leaves out
    private static final Set<String> NOT_COMPARED = Set.of("allComponents", "history", "historyText", "playerTimer",
            "rnd", "redeterminisationRnd", "listeners");

    /** The names of the state's fields whose values differ, to explain a failed comparison. */
    static List<String> differences(Object a, Object b) {
        List<String> result = new ArrayList<>();
        for (Class<?> c = a.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                if (NOT_COMPARED.contains(f.getName())) continue;
                try {
                    f.setAccessible(true);
                    if (!Objects.deepEquals(f.get(a), f.get(b)))
                        result.add(c.getSimpleName() + "." + f.getName() + (f.get(a) instanceof Collection<?> col
                                ? " " + col.stream().map(x -> x.getClass().getSimpleName()).toList() : ""));
                } catch (ReflectiveOperationException | RuntimeException e) {
                    result.add(c.getSimpleName() + "." + f.getName() + " (" + e.getClass().getSimpleName() + ")");
                }
            }
        }
        return result;
    }

    @Test
    public void aNewGameRoundTrips() {
        checkRoundTrip(17, 0);
    }

    @Test
    public void aGameInProgressRoundTrips() {
        checkRoundTrip(23, 15);
        checkRoundTrip(29, 60);
    }

    @Test
    public void aFinishedGameRoundTrips() {
        checkRoundTrip(31, 100_000);
    }
}
