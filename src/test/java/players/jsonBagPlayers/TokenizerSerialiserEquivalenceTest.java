package players.jsonBagPlayers;

import com.google.gson.Gson;
import core.AbstractGameState;
import core.AbstractGameStateRecord;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import evaluation.records.GameStateRecordFactory;
import games.GameType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import players.simple.RandomPlayer;
import utilities.Json;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

/**
 * JSON-Bag tokens must not depend on the JSON library. For each game with a record, a mid-game
 * record is serialised by Gson (the library the bags were built with) and by {@link Json}, in the
 * same process, and the two token maps must be identical apart from the {@code "class"} type ids that
 * {@link Json} adds to components, and the fields every record has that Gson cannot write as Jackson does
 * (the parameters are a Jackson tree; the actions in progress carry type ids). Comparing two serialisations of one object
 * avoids cross-run noise: component IDs and some game content vary between separately launched JVMs.
 * <p>
 * This test is the one place Gson may still be imported (see {@code JsonLibraryRulesTest}).
 */
@RunWith(Parameterized.class)
public class TokenizerSerialiserEquivalenceTest {

    /** Identity and derived values that must never become tokens; see {@link Json}. */
    static final Pattern IDENTITY_TOKEN = Pattern.compile(
            "(^|[.\\[])(componentID|ownerId|numProperties|visibilityMode|gridValues|@id)([.\\[]|$)");

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> games() {
        return Arrays.stream(GameType.values())
                .filter(GameStateRecordFactory::hasRecord)
                .map(g -> new Object[]{g})
                .toList();
    }

    private final GameType gameType;

    public TokenizerSerialiserEquivalenceTest(GameType gameType) {
        this.gameType = gameType;
    }

    private AbstractGameStateRecord midGameRecord() {
        int nPlayers = Math.max(2, gameType.getMinPlayers());
        Game game = gameType.createGameInstance(nPlayers, 42L);
        List<AbstractPlayer> players = IntStream.range(0, nPlayers).mapToObj(i -> (AbstractPlayer) new RandomPlayer()).toList();
        game.reset(players);
        AbstractGameState state = game.getGameState();
        Random rnd = new Random(7);
        for (int i = 0; i < 40 && state.isNotTerminal(); i++) {
            List<AbstractAction> actions = game.getForwardModel().computeAvailableActions(state);
            game.getForwardModel().next(state, actions.get(rnd.nextInt(actions.size())));
        }
        return GameStateRecordFactory.createRecord(state);
    }

    @Test
    public void gsonAndJacksonGiveIdenticalTokens() throws Exception {
        AbstractGameStateRecord record = midGameRecord();
        Map<String, Integer> viaGson = withoutTypeIds(Tokenizer.tokenize(new Gson().toJson(record)));
        Map<String, Integer> viaJackson = withoutTypeIds(Tokenizer.tokenize(Json.mapper().writeValueAsString(record)));
        assertFalse(viaGson.isEmpty());

        Set<String> onlyGson = new TreeSet<>(viaGson.keySet());
        onlyGson.removeAll(viaJackson.keySet());
        Set<String> onlyJackson = new TreeSet<>(viaJackson.keySet());
        onlyJackson.removeAll(viaGson.keySet());
        assertTrue("only in Gson: " + first(onlyGson) + "\nonly in Jackson: " + first(onlyJackson),
                onlyGson.isEmpty() && onlyJackson.isEmpty());
        assertEquals(viaGson, viaJackson);
    }

    @Test
    public void objectAndStringEntryPointsAgree() throws Exception {
        AbstractGameStateRecord record = midGameRecord();
        assertEquals(Tokenizer.tokenize(Json.mapper().writeValueAsString(record), "", true, "both", true,
                        false, false, false, null, false),
                Tokenizer.tokenize(record));
    }

    @Test
    public void noIdentityTokens() {
        Map<String, Integer> bag = Tokenizer.tokenize(midGameRecord());
        List<String> identity = bag.keySet().stream().filter(t -> IDENTITY_TOKEN.matcher(t).find()).sorted().toList();
        assertTrue("identity tokens: " + first(identity), identity.isEmpty());
    }

    private static final Set<String> RECORD_ONLY_FIELDS = Set.of("gameParameters", "actionsInProgress");

    /** The tokens that come neither from a {@code "class"} type id nor from {@link #RECORD_ONLY_FIELDS}. */
    static Map<String, Integer> withoutTypeIds(Map<String, Integer> bag) {
        Map<String, Integer> result = new HashMap<>(bag);
        result.keySet().removeIf(t -> {
            List<String> segments = Arrays.asList(t.split("[.\\[\\]]"));
            return segments.contains("class") || (segments.size() > 1 && RECORD_ONLY_FIELDS.contains(segments.get(1)));
        });
        return result;
    }

    private static String first(Collection<String> tokens) {
        return tokens.stream().limit(10).toList() + (tokens.size() > 10 ? " ... (" + tokens.size() + ")" : "");
    }
}
