package edu.byu.ece.rapidSmith.design.subsite;

import edu.byu.ece.rapidSmith.device.Wire;
import edu.byu.ece.rapidSmith.device.Connection;

public class RouteTree extends AbstractRouteTree<RouteTree> {
    public RouteTree(Wire wire) {
        super(wire);
    }

    public RouteTree(Wire wire, Connection connection) {
        super(wire, connection);
    }

    @Override
    protected RouteTree newInstance(Wire wire, Connection connection) {
        RouteTree tree = new RouteTree(wire);
		tree.setConnection(connection);
		return tree;
    }
}