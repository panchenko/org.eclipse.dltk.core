/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 
 *******************************************************************************/
package org.eclipse.dltk.core.search.matching2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A set of matches and possible matches, which need to be resolved.
 */
public abstract class MatchingNodeSet<E> implements IMatchingNodeSet<E>,
		Comparator<E> {
	/**
	 * Map of matching ast nodes that don't need to be resolved to their
	 * accuracy level. Each node is removed as it is reported.
	 */
	private Map<E, MatchLevel> matchingNodes = new HashMap<E, MatchLevel>();

	// sourceRange -> node
	private HashtableOfLong<E> matchingNodesKeys = new HashtableOfLong<E>();

	/**
	 * Set of possible matching ast nodes. They need to be resolved to determine
	 * if they really match the search pattern.
	 */
	private Set<E> possibleMatchingNodes = new HashSet<E>();

	// sourceRange -> node
	private HashtableOfLong<E> possibleMatchingNodesKeys = new HashtableOfLong<E>();

	public MatchLevel addMatch(E node, MatchLevel matchLevel) {
		switch (matchLevel) {
		case POSSIBLE_MATCH:
			addPossibleMatch(node);
			break;
		case INACCURATE_MATCH:
			addTrustedMatch(node, MatchLevel.INACCURATE_MATCH);
			break;
		case ACCURATE_MATCH:
			addTrustedMatch(node, MatchLevel.ACCURATE_MATCH);
			break;
		}
		return matchLevel;
	}

	public void addPossibleMatch(E node) {
		// remove existing node at same position from set
		// (case of recovery that created the same node several time
		// see http://bugs.eclipse.org/bugs/show_bug.cgi?id=29366)
		long key = computeNodeKey(node);
		E existing = this.possibleMatchingNodesKeys.get(key);
		if (existing != null && existing.getClass().equals(node.getClass()))
			this.possibleMatchingNodes.remove(existing);
		// add node to set
		this.possibleMatchingNodes.add(node);
		this.possibleMatchingNodesKeys.put(key, node);
	}

	private void addTrustedMatch(E node, MatchLevel level) {
		// remove existing node at same position from set
		// (case of recovery that created the same node several time
		// see http://bugs.eclipse.org/bugs/show_bug.cgi?id=29366)
		long key = computeNodeKey(node);
		E existing = this.matchingNodesKeys.get(key);
		if (existing != null && existing.getClass().equals(node.getClass()))
			this.matchingNodes.remove(existing);
		// map node to its accuracy level
		this.matchingNodes.put(node, level);
		this.matchingNodesKeys.put(key, node);
	}

	protected boolean hasPossibleNodes(int start, int end) {
		for (E node : possibleMatchingNodes) {
			if (checkRange(node, start, end)) {
				return true;
			}
		}
		for (E node : matchingNodes.keySet()) {
			if (checkRange(node, start, end)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the matching nodes that are in the given range in the source
	 * order.
	 */
	public List<E> matchingNodes(int start, int end) {
		List<E> nodes = null;
		for (E node : matchingNodes.keySet()) {
			if (checkRange(node, start, end)) {
				if (nodes == null)
					nodes = new ArrayList<E>();
				nodes.add(node);
			}
		}
		if (nodes == null)
			return Collections.emptyList();
		Collections.sort(nodes, this);
		return nodes;
	}

	/**
	 * Returns all the matching nodes
	 */
	public List<E> matchingNodes() {
		final List<E> nodes = new ArrayList<E>(matchingNodes.keySet());
		Collections.sort(nodes, this);
		return nodes;
	}

	public boolean removePossibleMatch(E node) {
		long key = computeNodeKey(node);
		E existing = this.possibleMatchingNodesKeys.get(key);
		if (existing == null)
			return false;
		this.possibleMatchingNodesKeys.put(key, null);
		return this.possibleMatchingNodes.remove(node);
	}

	protected abstract long computeNodeKey(E node);

	protected abstract boolean checkRange(E node, int start, int end);

	public MatchLevel removeTrustedMatch(E node) {
		long key = computeNodeKey(node);
		E existing = this.matchingNodesKeys.get(key);
		if (existing == null)
			return null;
		this.matchingNodesKeys.put(key, null);
		return this.matchingNodes.remove(node);
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("Exact matches:"); //$NON-NLS-1$
		List<E> nodes = new ArrayList<E>(matchingNodes.keySet());
		Collections.sort(nodes, this);
		for (E node : nodes) {
			result.append("\n\t"); //$NON-NLS-1$
			result.append(matchingNodes.get(node));
			result.append(' ');
			result.append(describeNode(node));
		}
		result.append("\nPossible matches:"); //$NON-NLS-1$
		nodes.clear();
		nodes.addAll(possibleMatchingNodes);
		Collections.sort(nodes, this);
		for (E node : nodes) {
			result.append("\nPOSSIBLE_MATCH: "); //$NON-NLS-1$
			result.append(describeNode(node));
		}
		return result.toString();
	}

	protected String describeNode(E node) {
		return node.toString();
	}

	public void clear() {
		matchingNodes.clear();
		matchingNodesKeys.clear();
		clearPossibleMatchingNodes();
		// possibleMatchingNodes.clear();
		// possibleMatchingNodesKeys.clear();
	}

	public void clearPossibleMatchingNodes() {
		possibleMatchingNodes.clear();
		possibleMatchingNodesKeys.clear();
	}

	public Collection<E> getPossibleMatchingNodes() {
		return possibleMatchingNodes;
	}

	public int countMatchingNodes() {
		return matchingNodes.size();
	}

	public int countPossibleMatchingNodes() {
		return possibleMatchingNodes.size();
	}

	/**
	 * @return
	 */
	public boolean isEmpty() {
		return matchingNodes.isEmpty() && possibleMatchingNodes.isEmpty();
	}

}
