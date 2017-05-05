package edu.byu.ece.rapidSmith.design.subsite;

import edu.byu.ece.rapidSmith.device.Wire;
import edu.byu.ece.rapidSmith.device.Connection;

/**
 * A class that provides default RouteTree functionality.
 * It is only needed because the recursive generic parent tree requires a type.
 */
public class RouteTree extends AbstractRouteTree<RouteTree> {
    /**
     * Sets the wire object in a new RouteTree
     *
     * @param wire the Wire to route
     */
    public RouteTree(Wire wire) {
        super(wire);
    }

    /**
     * Sets the wire and connection objects in a new RouteTree
     *
     * @param wire the Wire to route
     * @param connection the first Connection
     */
    public RouteTree(Wire wire, Connection connection) {
        super(wire, connection);
    }

    /**
     * Simple method that returns a new instance of itself.
     * This method must be overridden to extend {@link AbstractRouteTree}.
     *
     * @param wire the Wire to route
     * @param connection the connection for the new RouteTree to represent
     * @return a new RouteTree object of the extended type
     */
    @Override
    protected RouteTree newInstance(Wire wire, Connection connection) {
        RouteTree tree = new RouteTree(wire);
		tree.setConnection(connection);
		return tree;
    }
}