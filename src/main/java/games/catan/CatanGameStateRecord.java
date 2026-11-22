package games.catan;

import core.AbstractGameState;
import core.AbstractGameStateRecord;
import core.actions.AbstractAction;
import core.components.BoardNodeWithEdges;
import core.components.Counter;
import core.components.Deck;
import core.components.Edge;
import core.components.GraphBoardWithEdges;
import games.catan.actions.build.BuyAction;
import games.catan.components.Building;
import games.catan.components.CatanCard;
import games.catan.components.CatanTile;

import java.util.*;

public class CatanGameStateRecord extends AbstractGameStateRecord {
    protected CatanTile[][] board;
    protected Graph catanGraph;
    protected int[] scores; // score for each player
    protected int[] victoryPoints; // secret points from victory cards
    protected int[] knights, roadLengths; // knight count and road length for each player
    protected List<Map<CatanParameters.Resource, Counter>> exchangeRates; // exchange rate with bank for each resource
    protected int largestArmyOwner; // playerID of the player currently holding the largest army
    protected int longestRoadOwner; // playerID of the player currently holding the longest road
    protected int longestRoadLength, largestArmySize;
    int rollValue;

    List<Map<CatanParameters.Resource, Counter>> playerResources;
    List<Map<BuyAction.BuyType, Counter>> playerTokens;
    List<Deck<CatanCard>> playerDevCards;
    Map<CatanParameters.Resource, Counter> resourcePool;
    Deck<CatanCard> devCards;
    boolean developmentCardPlayed; // Tracks whether a player has played a development card this turn

    AbstractAction tradeOffer;
    int negotiationStepsCount;
    int nTradesThisTurn;

    /**
     * The settlements and roads: each node (a {@link Building}) with its edges, in the node's own order. A node or
     * edge is named by its component ID, under keys ending in {@code ID}, which JSON-Bag does not tokenize; who
     * owns it is under {@code owner}.
     */
    record Graph(int componentID, String name, List<Node> nodes) {
    }

    record Node(int componentID, int owner, Building.Type buildingType, CatanParameters.Resource harbour,
                List<Link> links) {
    }

    record Link(int edgeID, int owner, int neighbourID) {
    }

    public CatanGameStateRecord(CatanGameState gs) {
        super(gs);
        board = gs.board;
        catanGraph = graph(gs.catanGraph);
        scores = gs.scores; // score for each player
        victoryPoints = gs.victoryPoints; // secret points from victory cards
        knights = gs.knights;
        roadLengths = gs.roadLengths; // knight count and road length for each player
        exchangeRates = gs.exchangeRates; // exchange rate with bank for each resource
        largestArmyOwner = gs.largestArmyOwner; // playerID of the player currently holding the largest army
        longestRoadOwner = gs.longestRoadOwner; // playerID of the player currently holding the longest road
        longestRoadLength = gs.longestRoadLength;
        largestArmySize = gs.largestArmySize;
        rollValue = gs.rollValue;

        playerResources = gs.playerResources;
        playerTokens = gs.playerTokens;
        playerDevCards = gs.playerDevCards;
        resourcePool = gs.resourcePool;
        devCards = gs.devCards;
        developmentCardPlayed = gs.developmentCardPlayed;

        tradeOffer = gs.tradeOffer;
        negotiationStepsCount = gs.negotiationStepsCount;
        nTradesThisTurn = gs.nTradesThisTurn;
    }

    @SuppressWarnings("unused") // for JSON
    private CatanGameStateRecord() {
    }

    private static Graph graph(GraphBoardWithEdges board) {
        if (board == null) return null;
        List<Node> nodes = new ArrayList<>();
        for (BoardNodeWithEdges node : board.getBoardNodes()) {
            Building building = (Building) node;
            List<Link> links = new ArrayList<>();
            node.getNeighbourEdgeMapping().forEach((edge, neighbour) ->
                    links.add(new Link(edge.getComponentID(), edge.getOwnerId(), neighbour.getComponentID())));
            nodes.add(new Node(node.getComponentID(), node.getOwnerId(), building.getBuildingType(),
                    building.getHarbour(), links));
        }
        return new Graph(board.getComponentID(), board.getComponentName(), nodes);
    }

    private static GraphBoardWithEdges board(Graph graph) {
        GraphBoardWithEdges board = new GraphBoardWithEdges(graph.name(), graph.componentID());
        Map<Integer, Building> nodes = new LinkedHashMap<>();
        for (Node n : graph.nodes()) {
            Building building = new Building(n.owner(), n.componentID());
            building.setBuildingType(n.buildingType());
            building.setHarbour(n.harbour());
            nodes.put(n.componentID(), building);
            board.addBoardNode(building);
        }
        // Each node's links go in its own order, which is the order its neighbours are iterated in
        Map<Integer, Edge> edges = new HashMap<>();
        for (Node n : graph.nodes()) {
            Map<Edge, BoardNodeWithEdges> mapping = nodes.get(n.componentID()).getNeighbourEdgeMapping();
            for (Link link : n.links()) {
                Edge edge = edges.computeIfAbsent(link.edgeID(), id -> new Edge(link.owner(), id));
                mapping.put(edge, nodes.get(link.neighbourID()));
            }
        }
        return board;
    }

    @Override
    protected void restoreGame(AbstractGameState state) {
        CatanGameState catan = (CatanGameState) state;
        catan.board = board;
        catan.catanGraph = board(catanGraph);
        catan.scores = scores.clone();
        catan.victoryPoints = victoryPoints.clone();
        catan.knights = knights.clone();
        catan.roadLengths = roadLengths.clone();
        catan.exchangeRates = new ArrayList<>(exchangeRates);
        catan.largestArmyOwner = largestArmyOwner;
        catan.longestRoadOwner = longestRoadOwner;
        catan.longestRoadLength = longestRoadLength;
        catan.largestArmySize = largestArmySize;
        catan.rollValue = rollValue;
        catan.playerResources = new ArrayList<>(playerResources);
        catan.playerTokens = new ArrayList<>(playerTokens);
        catan.playerDevCards = new ArrayList<>(playerDevCards);
        catan.resourcePool = resourcePool;
        catan.devCards = devCards;
        catan.developmentCardPlayed = developmentCardPlayed;
        catan.tradeOffer = tradeOffer;
        catan.negotiationStepsCount = negotiationStepsCount;
        catan.nTradesThisTurn = nTradesThisTurn;
    }
}
