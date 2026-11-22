package core.components;

import core.CoreConstants;

public class Edge extends Component {

    public Edge() {
        super(CoreConstants.ComponentType.BOARD_NODE, "Edge");
        setOwnerId(-1);
    }
    /** An edge with a given ID, as when a saved board is restored. */
    public Edge(int ownerID, int id) {
        super(CoreConstants.ComponentType.BOARD_NODE, "Edge", id);
        setOwnerId(ownerID);
    }

    @Override
    public String toString() {
        // prints references
        return "Edge (" + ownerId + ")";
    }

    public Edge copy(){
        return new Edge(ownerId, componentID);
    }

    @Override
    public int hashCode() {
        // not the owner: it changes (a road is built) while the edge is a key in its nodes' maps
        return componentID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof Edge edge) {
            return ownerId == edge.ownerId && componentID == edge.componentID;
        }
        return false;
    }
}
