/*
 * Copyright (c) 2016 Brigham Young University
 *
 * This file is part of the BYU RapidSmith Tools.
 *
 * BYU RapidSmith Tools is free software: you may redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * BYU RapidSmith Tools is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * A copy of the GNU General Public License is included with the BYU
 * RapidSmith Tools. It can be found at doc/LICENSE.GPL3.TXT. You may
 * also get a copy of the license at <http://www.gnu.org/licenses/>.
 */

package edu.byu.ece.rapidSmith.design.subsite;

import edu.byu.ece.rapidSmith.device.PIP;
import edu.byu.ece.rapidSmith.device.BelPin;
import edu.byu.ece.rapidSmith.device.Connection;
import edu.byu.ece.rapidSmith.device.SitePin;
import edu.byu.ece.rapidSmith.device.Wire;
import edu.byu.ece.rapidSmith.util.Exceptions;
import edu.byu.ece.rapidSmith.examples.aStarRouter.AStarRouter;

import java.util.*;

/**
 * The Abstract parent tree structure that represents the routing between {@link Connection}s.
 * Each new Connection is held by a different AbstractRouteTree node. These nodes are then
 * connected together to represent the routing.
 *
 * This class is extendable via recursive generics. To see an example of a class that extends
 * AbstractRouteTree, please view {@link RouteTree} or {@link AStarRouter.AStarRouteTree}.
 */
public abstract class AbstractRouteTree <RouteTreeT extends AbstractRouteTree> implements Iterable<RouteTreeT> {
	/** The parent tree of the specified instance */
	private RouteTreeT sourceTree;
	/** The wire object that all connected trees share */
	private final Wire wire;
	/** The connection that this RouteTree represents */
	private Connection connection;
	/** The child trees of the specified instance */
	private final Collection<RouteTreeT> sinkTrees = new ArrayList<>(1);

	/**
	 * Sets the wire object in a new RouteTree
	 * 
	 * @param wire the Wire to route
	 */
	public AbstractRouteTree(Wire wire) {
		this.wire = wire;
	}

	/**
	 * Sets the wire and connection objects in a new RouteTree
	 * 
	 * @param wire the Wire to route
	 * @param connection the first Connection
	 */
	public AbstractRouteTree(Wire wire, Connection connection) {
		this.wire = wire;
		this.connection = connection;
	}

	/**
	 * Simple method that returns a new instance of itself.
	 * This method must be overridden when extending {@link AbstractRouteTree}.
	 * 
	 * @param wire the Wire to route
	 * @param connection the connection for the new RouteTree to represent
	 * @return a new RouteTree object of the extended type
	 */
	protected abstract RouteTreeT newInstance(Wire wire, Connection connection);

	/**
	 * Gets the {@link Wire} that this RouteTree structure shares
	 * 
	 * @return Wire of this RouteTree instance
	 */
	public Wire getWire() {
		return wire;
	}

	/**
	 * Gets the {@link Connection} that this RouteTree instance represents
	 * 
	 * @return Connection of this specific instance
	 */
	public Connection getConnection() {
		return connection;
	}

	/**
	 * Changes the {@link Connection} that this RouteTree instance represents
	 * 
	 * @param connection the {@link Connection} to use for this instance
	 */
	protected void setConnection(Connection connection) {
		this.connection = connection;
	}

	/**
	 * Gets the parent tree of the specified instance
	 * 
	 * @return the parent tree of the extended type
	 */
	public RouteTreeT getSourceTree() {
		return sourceTree;
	}

	/**
	 * Gets the root tree of the overall RouteTree structure
	 * 
	 * @return the root tree of the extended type
	 */
	public RouteTreeT getFirstSource() {
		if (isSourced())
			return (RouteTreeT)sourceTree.getFirstSource();
		else
			return (RouteTreeT)this;
	}

	/**
	 * Adds a child tree to this node
	 * 
	 * @param sinktree the child tree to add
	 */
	protected void addSinkTree(RouteTreeT sinktree) {
		sinkTrees.add(sinktree);
	}

	/**
	 * Check if the specified instance is a branch or leaf node 
	 * 
	 * @return true if there is a parent, false if root instance
	 */
	public boolean isSourced() {
		return sourceTree != null;
	}

	/**
	 * Sets the parent node of the specified instance
	 * 
	 * @param sourceTree the new parent tree to use
	 */
	protected void setSourceTree(RouteTreeT sourceTree) {
		this.sourceTree = sourceTree;
	}

	public Collection<RouteTreeT> getSinkTrees() {
		return sinkTrees;
	}
	
	/**
	 * Returns true if the RouteTree object is a leaf (i.e. it has no children). 
	 * For a fully routed net, a leaf tree should connect to either a SitePin
	 * or BelPin.
	 * 
	 * @return true if no children, false otherwise
	 */
	public boolean isLeaf() {
		return sinkTrees.size() == 0;
	}
	
	/**
	 * Returns the SitePin connected to the wire of the RouteTree. If no SitePin
	 * object is connected, null is returned.
	 * 
	 * @return the SitePin connected to the RouteTree's {@link Wire}. null if none connected.
	 */
	public SitePin getConnectingSitePin() {
		Collection<Connection> pinConnections = wire.getPinConnections();
		return (pinConnections.isEmpty()) ? null : pinConnections.iterator().next().getSitePin(); 
	}
	
	/**
	 * Returns the BelPin connected to the wire of the RouteTree. If no BelPin
	 * object is connected, null is returned.
	 * 
	 * @return the BelPin connected to the RouteTree's {@link Wire}. null if none connected.
	 */
	public BelPin getConnectingBelPin() {
		Collection<Connection> terminalConnections = wire.getTerminals();
		return terminalConnections.isEmpty() ? null : terminalConnections.iterator().next().getBelPin();
	}

	/**
	 * Add a new child RouteTree to this instance
	 * 
	 * @param c the {@link Connection} to use when creating the child RouteTree
	 * @return the child RouteTree of the extended type
	 */
	public RouteTreeT addConnection(Connection c) {
		RouteTreeT endTree = newInstance(c.getSinkWire(), c);
		endTree.setSourceTree(this);
		sinkTrees.add(endTree);
		return endTree;
	}

	/**
	 * Add a child RouteTree to this instance. An exception is thrown if the child RouteTree is already sourced,
	 * or the Connection's wire doesn't match the child RouteTree's Wire.
	 * 
	 * @param c the {@link Connection} to use when creating the child RouteTree
	 * @param sink the RouteTree to add as a child
	 * @return the same RouteTree that was passed in but with a different {@link Connection}
	 */
	public RouteTreeT addConnection(Connection c, RouteTreeT sink) {
		if (sink.getSourceTree() != null)
			throw new Exceptions.DesignAssemblyException("Sink tree already sourced");
		if (!c.getSinkWire().equals(sink.getWire()))
			throw new Exceptions.DesignAssemblyException("Connection does not match sink tree");

		sinkTrees.add(sink);
		sink.setSourceTree(this);
		sink.setConnection(c);
		return sink;
	}

	/**
	 * Removes any child RouteTree that matches the parameter. Doesn't check grandchildren.
	 * 
	 * @param c remove any child matching this Connection
	 */
	public void removeConnection(Connection c) {
		for (Iterator<RouteTreeT> it = sinkTrees.iterator(); it.hasNext(); ) {
			RouteTreeT sink = it.next();
			if (sink.getConnection().equals(c)) {
				sink.setSourceTree(null);
				it.remove();
			}
		}
	}

	/**
	 * Removes any child or grandchild RouteTree that matches the parameter.
	 * 
	 * @param c remove any child or grandchild with this {@link Connection}
	 */
	public void removeConnectionRecursive(Connection c) {
		// Depth first (although it hardly matters since all nodes will be checked)
		for (Iterator<RouteTreeT> it = sinkTrees.iterator(); it.hasNext(); ) {
			RouteTreeT sink = it.next();
			if (sink.getConnection().equals(c)) {
				sink.setSourceTree(null);
				it.remove();
			} else {
				sink.removeConnectionRecursive(c);
			}
		}
	}
 
	/**
	 * Scan all connected RouteTrees and return a List of all {@link PIP}s
	 * 
	 * @return a list of all connected PIPs
	 */
	public List<PIP> getAllPips() {
		return getFirstSource().getAllPips(new ArrayList<>());
	}

	/**
	 * Recursively scan all child RouteTrees and return a List of all {@link PIP}s.
	 * This method is generally only called from the root RouteTree
	 *
	 * @param pips the incomplete list of PIPs to add to when another PIP is found
	 * @return the list of PIPs that was passed in with any newly found PIPs added
	 */
	protected List<PIP> getAllPips(List<PIP> pips) {
		for (RouteTreeT rt : sinkTrees) {
			if (rt.getConnection().isPip())
				pips.add(rt.getConnection().getPip());
			rt.getAllPips(pips);
		}
		return pips;
	}

	/**
	 * Copy this RouteTree into a completely separate structure.
	 *
	 * @return the replica structure
	 */
	public RouteTreeT deepCopy() {
		RouteTreeT copy = newInstance(wire, connection);
		sinkTrees.forEach(rt ->{
				RouteTreeT newtree = (RouteTreeT)rt.deepCopy();
				newtree.setSourceTree(copy);
				copy.addSinkTree(newtree);
		});
		return copy;
	}

	/**
	 * Remove all RouteTrees that aren't strongly connected to the RouteTree specified by the parameter.
	 *
	 * @param terminal the RouteTree to check for. All other non-connected trees will be removed.
	 * @return true if this RouteTree is connected to a terminal, false otherwise
	 */
	public boolean prune(RouteTreeT terminal) {
		Set<RouteTreeT> toPrune = new HashSet<>();
		toPrune.add(terminal);
		return prune(toPrune);
	}

	/**
	 * Remove all RouteTrees that aren't strongly connected to one of the RouteTrees specified in the given Set
	 *
	 * @param terminals the Set of RouteTrees to check for. All other non-connected trees will be removed.
	 * @return true if this RouteTree is connected to a terminal, false otherwise
	 */
	public boolean prune(Set<RouteTreeT> terminals) {
		return pruneChildren(terminals);
	}

	/**
	 * Helper function to remove unused RouteTrees
	 *
	 * @param terminals the set of RouteTrees to check for. All other non-connected trees will be removed.
	 * @return true if this RouteTree is connected to a terminal, false otherwise
	 */
	protected boolean pruneChildren(Set<RouteTreeT> terminals) {
		// prune children before handling current instance
		sinkTrees.removeIf(rt -> !rt.pruneChildren(terminals));
		// only keep this tree if it still has a child tree left (after pruning) or if it is a terminal itself
		return !sinkTrees.isEmpty() || terminals.contains(this);
	}

	/**
	 * Returns a preorder iterator for this tree structure. Same as {@link #preorderIterator}
	 *
	 * @return an Iterator implementing the {@link Iterable} interface
	 */
	@Override
	public Iterator<RouteTreeT> iterator() {
		return preorderIterator();
	}

	/**
	 * Returns a preorder iterator for this tree structure. Same as {@link #iterator}
	 *
	 * @return an Iterator implementing the {@link Iterable} interface
	 */
	public Iterator<RouteTreeT> preorderIterator() {
		return new PreorderIterator((RouteTreeT)this);
	}

	/**
	 * The sub-class that implements the {@link Iterator} interface
	 */
	private class PreorderIterator implements Iterator<RouteTreeT> {
		/** use a stack for preorder iteration */
		private final Stack<RouteTreeT> stack;

		/** initialize the stack to the given RouteTree
		 *
		 * @param initial the RouteTree to visit last
		 */
		PreorderIterator(RouteTreeT initial) {
			this.stack = new Stack<>();
			this.stack.push(initial);
		}

		/**
		 * Check if there is another RouteTree left to iterate over
		 *
		 * @return true if more RouteTrees left, false otherwise
		 */
		@Override
		public boolean hasNext() {
			return !stack.isEmpty();
		}

		/**
		 * Visit the current RouteTree and prepare to visit child trees of that RouteTree
		 *
		 * @return the next RouteTree to visit
		 */
		@Override
		public RouteTreeT next() {
			if (!hasNext())
				throw new NoSuchElementException();
			RouteTreeT tree = stack.pop();
			stack.addAll(tree.getSinkTrees());
			return tree;
		}
	}

	/**
	 * The AbstractRouteTree hash code is the same as the Connection it represents.
	 *
	 * @return a pseudo unique integer for this instance
	 */
	@Override
	public int hashCode() {
		return Objects.hash(connection);
	}
}
