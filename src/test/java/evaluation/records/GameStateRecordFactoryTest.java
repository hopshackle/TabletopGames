package evaluation.records;

import core.AbstractGameState;
import core.AbstractGameStateRecord;
import games.GameType;
import games.cantstop.CantStopGameStateRecord;
import games.catan.CatanGameStateRecord;
import games.connect4.Connect4GameStateRecord;
import games.dominion.DominionGameStateRecord;
import games.dotsboxes.DBGameStateRecord;
import games.seasaltpaper.SSPGameStateRecord;
import games.uno.UnoGameStateRecord;
import games.wonders7.Wonders7GameStateRecord;
import org.junit.Test;
import players.jsonBagPlayers.Tokenizer;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link GameStateRecordFactory} and for tokenizing a game state through its
 * record. The record is serialised from its fields by {@link utilities.Json}, so these tests also
 * guard against field-name clashes in the game state hierarchy (the mapper rejects a class that declares
 * two fields with the same JSON name).
 */
public class GameStateRecordFactoryTest {

    /** Game types supported by JSON-Bag, with a valid player count for each. */
    private static Map<GameType, Integer> supportedGames() {
        Map<GameType, Integer> games = new LinkedHashMap<>();
        games.put(GameType.CantStop, 2);
        games.put(GameType.Catan, 3);
        games.put(GameType.Connect4, 2);
        games.put(GameType.Dominion, 2);
        games.put(GameType.DominionSizeDistortion, 2);
        games.put(GameType.DominionImprovements, 2);
        games.put(GameType.DotsAndBoxes, 2);
        games.put(GameType.SeaSaltPaper, 2);
        games.put(GameType.Uno, 2);
        games.put(GameType.Wonders7, 3);
        return games;
    }

    @Test
    public void hasRecordIsTrueForEverySupportedGame() {
        supportedGames().keySet().forEach(gt ->
                assertTrue(gt + " should have a JSON-Bag record", GameStateRecordFactory.hasRecord(gt)));
    }

    @Test
    public void hasRecordIsFalseForUnsupportedGame() {
        assertFalse(GameStateRecordFactory.hasRecord(GameType.Poker));
        assertFalse(GameStateRecordFactory.hasRecord(GameType.Poker.createGameInstance(2, 7L).getGameState()));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void createRecordFailsWithAClearErrorForUnsupportedGame() {
        GameStateRecordFactory.createRecord(GameType.Poker.createGameInstance(2, 7L).getGameState());
    }

    @Test
    public void createRecordReturnsTheMatchingRecordType() {
        Map<GameType, Class<? extends AbstractGameStateRecord>> expected = new LinkedHashMap<>();
        expected.put(GameType.CantStop, CantStopGameStateRecord.class);
        expected.put(GameType.Catan, CatanGameStateRecord.class);
        expected.put(GameType.Connect4, Connect4GameStateRecord.class);
        expected.put(GameType.Dominion, DominionGameStateRecord.class);
        expected.put(GameType.DotsAndBoxes, DBGameStateRecord.class);
        expected.put(GameType.SeaSaltPaper, SSPGameStateRecord.class);
        expected.put(GameType.Uno, UnoGameStateRecord.class);
        expected.put(GameType.Wonders7, Wonders7GameStateRecord.class);

        expected.forEach((gt, recordClass) -> {
            AbstractGameState gs = gt.createGameInstance(supportedGames().get(gt), 99L).getGameState();
            AbstractGameStateRecord record = GameStateRecordFactory.createRecord(gs);
            assertEquals(recordClass, record.getClass());
            assertEquals(gt, record.gameType);
            assertEquals(gs.getNPlayers(), record.nPlayers);
        });
    }

    @Test
    public void everySupportedGameStateTokenizesToANonEmptyBag() {
        supportedGames().forEach((gt, nPlayers) -> {
            AbstractGameState gs = gt.createGameInstance(nPlayers, 2024L).getGameState();
            Map<String, Integer> bag = Tokenizer.tokenize(gs);
            assertFalse(gt + " should produce tokens", bag.isEmpty());
            assertTrue(gt + " should record its game type", bag.keySet().stream().anyMatch(k -> k.contains("gameType")));
        });
    }

    @Test
    public void tokenizedGameStateIsDeterministicForAGivenSeed() {
        AbstractGameState first = GameType.SeaSaltPaper.createGameInstance(2, 77L).getGameState();
        AbstractGameState second = GameType.SeaSaltPaper.createGameInstance(2, 77L).getGameState();

        assertEquals(Tokenizer.tokenize(first), Tokenizer.tokenize(second));
    }
}
