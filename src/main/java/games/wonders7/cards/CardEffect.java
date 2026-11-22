package games.wonders7.cards;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import games.wonders7.Wonders7GameState;

/** Written to JSON with its class name under {@code "class"}. */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "class")
public interface CardEffect {
    void apply(Wonders7GameState state, int playerId);
}
