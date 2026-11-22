package players.jsonBagPlayers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import utilities.Json;

import java.util.*;

/**
 * The optional structuring pass of JSON-Bag ({@code Tokenizer.tokenizeStructured}): it rewrites a game state's
 * JSON so that tokens say what a thing is and who owns it, rather than where a game happens to keep it. It relies
 * only on the conventions every game's record follows (see {@code core.AbstractGameStateRecord}): a component is an
 * object with a {@code componentID} and a {@code "class"}, a deck is a component with a {@code components} array,
 * and a field holding component IDs has a name ending in {@code ID} or {@code IDs}.
 * <ul>
 *     <li>Every deck, wherever it is, moves to a top-level {@code decks} object, under its name (with seat numbers
 *     removed) and owner, such as {@code "Player deck[owner=1]"}; its cards are grouped by name, so a card's tokens
 *     read {@code .decks.Player deck[owner=1].Copper.cost.0}. A deck's order is not kept.</li>
 *     <li>Other components stay where they are; their class becomes its simple name, under {@code kind}.</li>
 *     <li>A component ID names the component, not a feature: an {@code ...ID} field that refers to a component in
 *     the document is replaced by that component's name, and other IDs are dropped.</li>
 * </ul>
 * JSON without components is returned unchanged, so other JSON tokenizes as it would without the pass.
 */
public final class JsonStructurer {

    private JsonStructurer() {
    }

    public static JsonNode structure(JsonNode root) {
        Map<Integer, String> names = new HashMap<>();
        collectNames(root, names);
        if (names.isEmpty()) return root;
        ObjectNode decks = Json.object();
        JsonNode result = rewrite(root.deepCopy(), names, decks, null);
        if (decks.isEmpty()) return result;
        if (result instanceof ObjectNode object) {
            object.set("decks", decks);
            return object;
        }
        ObjectNode wrapper = Json.object();
        wrapper.set("state", result);
        wrapper.set("decks", decks);
        return wrapper;
    }

    private static boolean isComponent(JsonNode node) {
        return node.isObject() && node.has("componentID") && node.has("class");
    }

    private static boolean isDeck(JsonNode node) {
        return isComponent(node) && node.path("components").isArray();
    }

    private static void collectNames(JsonNode node, Map<Integer, String> names) {
        if (isComponent(node) && node.get("componentID").canConvertToInt())
            names.putIfAbsent(node.get("componentID").asInt(), node.path("componentName").asText(""));
        node.forEach(child -> collectNames(child, names));
    }

    private static JsonNode rewrite(JsonNode node, Map<Integer, String> names, ObjectNode decks, String fieldName) {
        if (node instanceof ArrayNode array) {
            ArrayNode result = Json.array();
            for (JsonNode item : array) {
                if (isDeck(item)) addDeck(item, names, decks, fieldName);
                else result.add(rewrite(item, names, decks, fieldName));
            }
            return result;
        }
        if (!(node instanceof ObjectNode object)) return node;

        ObjectNode result = Json.object();
        if (isComponent(object)) result.put("kind", simpleName(object.get("class").asText()));
        for (Iterator<Map.Entry<String, JsonNode>> it = object.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> e = it.next();
            String key = e.getKey();
            JsonNode value = e.getValue();
            if (isComponent(object) && (key.equals("class") || key.equals("componentID"))) continue;
            if (isReferenceKey(key)) {
                JsonNode resolved = resolve(value, names);
                if (resolved != null) result.set(referenceName(key), resolved);
                continue;
            }
            if (isDeck(value)) {
                addDeck(value, names, decks, key);
                continue;
            }
            result.set(key, rewrite(value, names, decks, key));
        }
        return result;
    }

    private static void addDeck(JsonNode deck, Map<Integer, String> names, ObjectNode decks, String fieldName) {
        String name = withoutNumbers(deck.path("componentName").asText(""));
        if (name.isEmpty()) name = fieldName == null ? simpleName(deck.get("class").asText()) : fieldName;
        int owner = deck.path("ownerId").asInt(-1);
        String key = owner >= 0 ? name + "[owner=" + owner + "]" : name;

        ObjectNode entry = decks.has(key) ? (ObjectNode) decks.get(key) : decks.putObject(key);
        entry.put("size", entry.path("size").asInt(0) + deck.get("components").size());
        for (JsonNode card : deck.get("components")) {
            JsonNode rewritten = rewrite(card, names, decks, null);
            String cardName = card.path("componentName").asText("");
            if (cardName.isEmpty()) cardName = rewritten.path("kind").asText("card");
            if (rewritten instanceof ObjectNode o) {
                o.remove("componentName");
                o.remove("kind");
            }
            (entry.has(cardName) ? (ArrayNode) entry.get(cardName) : entry.putArray(cardName)).add(rewritten);
        }
    }

    private static boolean isReferenceKey(String key) {
        return !key.equals("componentID") && (key.endsWith("ID") || key.endsWith("IDs"));
    }

    private static String referenceName(String key) {
        return key.endsWith("IDs") ? key.substring(0, key.length() - 3) : key.substring(0, key.length() - 2);
    }

    /** The names of the components the IDs refer to, or null if none is in the document. */
    private static JsonNode resolve(JsonNode value, Map<Integer, String> names) {
        if (value.isArray()) {
            ArrayNode result = Json.array();
            for (JsonNode v : value) {
                JsonNode r = resolve(v, names);
                if (r != null) result.add(r);
            }
            return result.isEmpty() ? null : result;
        }
        if (!value.canConvertToInt()) return null;
        String name = names.get(value.asInt());
        return name == null || name.isEmpty() ? null : TextNode.valueOf(name);
    }

    private static String simpleName(String className) {
        return className.substring(Math.max(className.lastIndexOf('.'), className.lastIndexOf('$')) + 1);
    }

    private static String withoutNumbers(String name) {
        return name.replaceAll("\\b\\d+\\b", "").replaceAll("\\s+", " ").trim();
    }
}
