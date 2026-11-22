package core;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import core.components.Component;
import core.interfaces.IExtendedSequence;
import core.interfaces.IGamePhase;
import evaluation.optimisation.TunableParameters;
import games.GameType;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import utilities.JSONUtils;
import utilities.Json;

import java.io.IOException;
import java.util.*;

/**
 * The JSON form of a game state: one per game, written and read by {@link Json}. It is what JSON-Bag tokenizes,
 * what trajectories and saved positions hold, and, for games whose record implements {@link #restoreGame},
 * what a state is loaded from.
 * <p>
 * This class holds what every game has: counters, status, phase, tunable parameters and the actions in
 * progress. A game's record adds the fields of its own state and, to be loadable, restores them in
 * {@link #restoreGame}. A record must hold everything needed to restore the state: a round trip through
 * {@link #toJson} and {@link #read} must give a state that {@code equals()} the original.
 * <p>
 * Restoring may hand the record's own objects (decks, cards) to the state, so a record is restored once, and
 * from {@link #read}, which gives each component one instance however often it appears.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "class")
public abstract class AbstractGameStateRecord {

    public final GameType gameType;
    public final int gameID;

    // Game tick, number of iterations of game loop
    public final int tick;

    public final int roundCounter, turnCounter, turnOwner, firstPlayer;
    public final int nPlayers;
    public final int nTeams;

    // Status of the game, and status for each player (in cooperative games, the game status is also each player's status)
    public final CoreConstants.GameResult gameStatus;
    public final CoreConstants.GameResult[] playerResults;

    // The phase, as its enum class and constant name
    public final String gamePhaseClass, gamePhase;

    // Tunable parameters as written by TunableParameters.instanceToJSON; absent for other parameters
    public final JsonNode gameParameters;

    // Bottom of the stack first
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "class")
    public final List<IExtendedSequence> actionsInProgress;

    protected AbstractGameStateRecord(AbstractGameState gs) {
        this.gameType = gameTypeOf(gs);
        this.gameID = gs.getGameID();

        this.tick = gs.getGameTick();
        this.roundCounter = gs.getRoundCounter();
        this.turnCounter = gs.getTurnCounter();
        this.turnOwner = gs.getTurnOwner();

        this.firstPlayer = gs.getFirstPlayer();
        this.nPlayers = gs.getNPlayers();
        this.nTeams = gs.getNTeams();

        this.gameStatus = gs.getGameStatus();
        this.playerResults = gs.getPlayerResults().clone();

        IGamePhase phase = gs.getGamePhase();
        this.gamePhaseClass = phase == null ? null : phase.getClass().getName();
        this.gamePhase = phase instanceof Enum<?> e ? e.name() : phase == null ? null : phase.toString();

        this.gameParameters = gs.getGameParameters() instanceof TunableParameters<?> tp
                ? Json.parse(tp.instanceToJSON(true, new HashMap<>()).toJSONString())
                : null;

        this.actionsInProgress = gs.actionsInProgress.isEmpty() ? null : new ArrayList<>(gs.actionsInProgress);
    }

    /** For JSON: the reader sets the fields from the document. */
    protected AbstractGameStateRecord() {
        gameType = null;
        gameID = 0;
        tick = roundCounter = turnCounter = turnOwner = firstPlayer = nPlayers = nTeams = 0;
        gameStatus = null;
        playerResults = null;
        gamePhaseClass = gamePhase = null;
        gameParameters = null;
        actionsInProgress = null;
    }

    /**
     * Restores the fields of the game's own state. Called by {@link #restore} on a state that has been set up
     * for this game, with this record's parameters and player count, after the common fields are restored.
     *
     * @throws UnsupportedOperationException if this game's states cannot be restored yet
     */
    protected void restoreGame(AbstractGameState state) {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " cannot restore a game state yet");
    }

    /** Applies this record to a state set up for the same game, parameters and player count. */
    public final void restore(AbstractGameState state) {
        if (gameTypeOf(state) != gameType || state.getNPlayers() != nPlayers)
            throw new IllegalArgumentException("A " + gameType + " record for " + nPlayers + " players cannot restore a "
                    + gameTypeOf(state) + " state for " + state.getNPlayers());
        state.restoreCoreFields(this);
        restoreGame(state);
    }

    /**
     * The game a state belongs to. Games that share a state class (Backgammon and XII Scripta, the Dominion variants)
     * report the first of them, so they are told apart by the class of their parameters.
     */
    public static GameType gameTypeOf(AbstractGameState gs) {
        GameType reported = gs.getGameType();
        Class<?> paramsClass = gs.getGameParameters() == null ? null : gs.getGameParameters().getClass();
        if (paramsClass == null || reported.getParameterClass() == paramsClass) return reported;
        for (GameType gt : GameType.values())
            if (gt.getGameStateClass() == gs.getClass() && gt.getParameterClass() == paramsClass) return gt;
        return reported;
    }

    /** The parameters this record was written with: the game's defaults, with the tunable values it holds. */
    public AbstractParameters parameters() {
        AbstractParameters params = gameType.createParameters(0);
        if (gameParameters != null) {
            JSONObject raw = (JSONObject) JSONValue.parse(gameParameters.toString());
            Object recordedClass = raw.get("class");
            if (recordedClass != null && !recordedClass.equals(params.getClass().getName()))
                return JSONUtils.loadClassFromJSON(raw);
            params = (AbstractParameters) TunableParameters.loadFromJSON((TunableParameters<?>) params, raw);
        }
        return params;
    }

    /** A new state, set up with this record's parameters and player count, then restored from this record. */
    public AbstractGameState toState() {
        AbstractParameters params = parameters();
        AbstractGameState state = gameType.createGameState(params, nPlayers);
        gameType.createForwardModel(params, nPlayers).setup(state);
        restore(state);
        return state;
    }

    IGamePhase phase() {
        if (gamePhaseClass == null) return null;
        try {
            Class<?> type = Class.forName(gamePhaseClass);
            if (type.isEnum()) {
                for (Object constant : type.getEnumConstants())
                    if (((Enum<?>) constant).name().equals(gamePhase)) return (IGamePhase) constant;
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unknown game phase class " + gamePhaseClass, e);
        }
        throw new IllegalArgumentException("Cannot restore game phase " + gamePhase + " of " + gamePhaseClass);
    }

    public String toJson() {
        try {
            return Json.mapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot write " + getClass().getSimpleName() + " to JSON", e);
        }
    }

    /**
     * Reads a record. A component that appears in several places is read once and shared, and components created
     * afterwards get IDs above those read.
     */
    public static AbstractGameStateRecord read(String json) {
        Map<Integer, Component> components = new HashMap<>();
        try {
            AbstractGameStateRecord record = Json.stateReader(components).readValue(json, AbstractGameStateRecord.class);
            components.keySet().stream().max(Integer::compare).ifPresent(Component::reserveIdsUpTo);
            return record;
        } catch (IOException e) {
            throw new IllegalArgumentException("Not a game state record: " + e.getMessage(), e);
        }
    }
}
