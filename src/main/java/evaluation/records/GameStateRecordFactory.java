package evaluation.records;

import core.AbstractGameState;
import core.AbstractGameStateRecord;
import games.GameType;

import games.cantstop.CantStopGameState;
import games.cantstop.CantStopGameStateRecord;
import games.catan.CatanGameState;
import games.catan.CatanGameStateRecord;
import games.connect4.Connect4GameState;
import games.connect4.Connect4GameStateRecord;
import games.dominion.DominionGameState;
import games.dominion.DominionGameStateRecord;
import games.dotsboxes.DBGameState;
import games.dotsboxes.DBGameStateRecord;
import games.seasaltpaper.SeaSaltPaperGameState;
import games.seasaltpaper.SSPGameStateRecord;
import games.uno.UnoGameState;
import games.uno.UnoGameStateRecord;
import games.wonders7.Wonders7GameState;
import games.wonders7.Wonders7GameStateRecord;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Factory class to convert AbstractGameState to its corresponding AbstractGameStateRecord.
 * Not all game states have a corresponding record - an UnsupportedOperationException is thrown
 * if no record exists for the given game state.
 */
public class GameStateRecordFactory {

    private static final Map<GameType, Function<AbstractGameState, AbstractGameStateRecord>> recordFactories = new HashMap<>();

    static {
        recordFactories.put(GameType.CantStop, gs -> new CantStopGameStateRecord((CantStopGameState) gs));
        recordFactories.put(GameType.Catan, gs -> new CatanGameStateRecord((CatanGameState) gs));
        recordFactories.put(GameType.Connect4, gs -> new Connect4GameStateRecord((Connect4GameState) gs));
        recordFactories.put(GameType.Dominion, gs -> new DominionGameStateRecord((DominionGameState) gs));
        recordFactories.put(GameType.DominionSizeDistortion, gs -> new DominionGameStateRecord((DominionGameState) gs));
        recordFactories.put(GameType.DominionImprovements, gs -> new DominionGameStateRecord((DominionGameState) gs));
        recordFactories.put(GameType.DotsAndBoxes, gs -> new DBGameStateRecord((DBGameState) gs));
        recordFactories.put(GameType.SeaSaltPaper, gs -> new SSPGameStateRecord((SeaSaltPaperGameState) gs));
        recordFactories.put(GameType.Uno, gs -> new UnoGameStateRecord((UnoGameState) gs));
        recordFactories.put(GameType.Wonders7, gs -> new Wonders7GameStateRecord((Wonders7GameState) gs));
    }

    /**
     * Creates an AbstractGameStateRecord from the given AbstractGameState.
     *
     * @param gs the game state to convert
     * @return the corresponding record
     * @throws UnsupportedOperationException if no record exists for this game type
     */
    public static AbstractGameStateRecord createRecord(AbstractGameState gs) {
        GameType gameType = gs.getGameType();
        Function<AbstractGameState, AbstractGameStateRecord> factory = recordFactories.get(gameType);
        
        if (factory == null) {
            throw new UnsupportedOperationException(
                "No AbstractGameStateRecord available for game type: " + gameType.name() + 
                ". Not all games have a corresponding record implementation.");
        }
        
        return factory.apply(gs);
    }

    /**
     * Checks if a record exists for the given game type.
     *
     * @param gameType the game type to check
     * @return true if a record exists, false otherwise
     */
    public static boolean hasRecord(GameType gameType) {
        return recordFactories.containsKey(gameType);
    }

    /**
     * Checks if a record exists for the given game state.
     *
     * @param gs the game state to check
     * @return true if a record exists, false otherwise
     */
    public static boolean hasRecord(AbstractGameState gs) {
        return hasRecord(gs.getGameType());
    }
}
