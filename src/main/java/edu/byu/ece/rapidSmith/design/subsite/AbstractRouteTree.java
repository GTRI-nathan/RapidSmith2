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

import java.util.*;

/**
 *
 */
public abstract class AbstractRouteTree <RouteTreeT extends AbstractRouteTree> implements Iterable<RouteTreeT> {
	private RouteTreeT sourceTree; // Do I want bidirectional checks?
	private final Wire wire;
	private Connection connection;
	private final Collection<RouteTreeT> sinkTrees = new ArrayList<>(1);

	public AbstractRouteTree(Wire wire) {
		this.wire = wire;
	}

	public AbstractRouteTree(Wire wire, Connection connection) {
		this.wire = wire;
		this.connection = connection;
	}

	protected abstract RouteTreeT newInstance(Wire wire, Connection connection);

	public Wire getWire() {
		return wire;
	}

	public Connection getConnection() {
		return connection;
	}

	protected void setConnection(Connection connection) {
		this.connection = connection;
	}

	public RouteTreeT getSourceTree() {
		return sourceTree;
	}

	public RouteTreeT getFirstSource() {
		if (isSourced())
			return (RouteTreeT)sourceTree.getFirstSource();
		else
			return (RouteTreeT)this;
	}

	protected void addSinkTree(RouteTreeT sinktree) {
		sinkTrees.add(sinktree);
	}

	public boolean isSourced() {
		return sourceTree != null;
	}

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
	 */
	public boolean isLeaf() {
		return sinkTrees.size() == 0;
	}
	
	/**
	 * Returns the SitePin connected to the wire of the RouteTree. If no SitePin
	 * object is connected, null is returned.
	 */
	public SitePin getConnectingSitePin() {
		Collection<Connection> pinConnections = wire.getPinConnections();
		return (pinConnections.isEmpty()) ? null : pinConnections.iterator().next().getSitePin(); 
	}
	
	/**
	 * Returns the BelPin connected to the wire of the RouteTree. If no BelPin
	 * object is connected, null is returned.
	 */
	public BelPin getConnectingBelPin() {
		Collection<Connection> terminalConnections = wire.getTerminals();
		return terminalConnections.isEmpty() ? null : terminalConnections.iterator().next().getBelPin();
	}

	public RouteTreeT addConnection(Connection c) {
		RouteTreeT endTree = newInstance(c.getSinkWire(), c);
		endTree.setSourceTree(this);
		sinkTrees.add(endTree);
		return endTree;
	}

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

	public void removeConnection(Connection c) {
		for (Iterator<RouteTreeT> it = sinkTrees.iterator(); it.hasNext(); ) {
			RouteTreeT sink = it.next();
			if (sink.getConnection().equals(c)) {
				sink.setSourceTree(null);
				it.remove();
			}
		}
	}

	public List<PIP> getAllPips() {
		return getFirstSource().getAllPips(new ArrayList<>());
	}

	protected List<PIP> getAllPips(List<PIP> pips) {
		for (RouteTreeT rt : sinkTrees) {
			if (rt.getConnection().isPip())
				pips.add(rt.getConnection().getPip());
			rt.getAllPips(pips);
		}
		return pips;
	}

	public RouteTreeT deepCopy() {
		RouteTreeT copy = newInstance(wire, connection);
		sinkTrees.forEach(rt ->{
				RouteTreeT newtree = (RouteTreeT)rt.deepCopy();
				newtree.setSourceTree(copy);
				copy.addSinkTree(newtree);
		});
		return copy;
	}

	public boolean prune(RouteTreeT terminal) {
		Set<RouteTreeT> toPrune = new HashSet<>();
		toPrune.add(terminal);
		return prune(toPrune);
	}
	
	public boolean prune(Set<RouteTreeT> terminals) {
		return pruneChildren(terminals);
	}

	protected boolean pruneChildren(Set<RouteTreeT> terminals) {
		sinkTrees.removeIf(rt -> !rt.pruneChildren(terminals));
		return !sinkTrees.isEmpty() || terminals.contains(this);
	}
	
	@Override
	public Iterator<RouteTreeT> iterator() {
		return preorderIterator();
	}

	public Iterator<RouteTreeT> preorderIterator() {
		return new PreorderIterator((RouteTreeT)this);
	}

	private class PreorderIterator implements Iterator<RouteTreeT> {
		private final Stack<RouteTreeT> stack;

		PreorderIterator(RouteTreeT initial) {
			this.stack = new Stack<>();
			this.stack.push(initial);
		}

		@Override
		public boolean hasNext() {
			return !stack.isEmpty();
		}

		@Override
		public RouteTreeT next() {
			if (!hasNext())
				throw new NoSuchElementException();
			RouteTreeT tree = stack.pop();
			stack.addAll(tree.getSinkTrees());
			return tree;
		}
	}

	// Uses identity equals

	@Override
	public int hashCode() {
		return Objects.hash(connection);
	}
}
