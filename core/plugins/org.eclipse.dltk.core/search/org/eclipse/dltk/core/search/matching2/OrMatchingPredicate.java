/*******************************************************************************
 * Copyright (c) 2010 xored software, Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     xored software, Inc. - initial API and Implementation (Alex Panchenko)
 *******************************************************************************/
package org.eclipse.dltk.core.search.matching2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class OrMatchingPredicate<E> implements IMatchingPredicate<E> {

	private List<IMatchingPredicate<E>> predicates = new ArrayList<IMatchingPredicate<E>>();

	public void addPredicate(IMatchingPredicate<E> predicate) {
		predicates.add(predicate);
	}

	public MatchLevel match(E node) {
		for (IMatchingPredicate<E> predicate : predicates) {
			final MatchLevel level = predicate.match(node);
			if (level != null) {
				return level;
			}
		}
		return null;
	}

	public MatchLevel resolvePotentialMatch(E node) {
		for (IMatchingPredicate<E> predicate : predicates) {
			final MatchLevel level = predicate.resolvePotentialMatch(node);
			if (level != null) {
				return level;
			}
		}
		return null;
	}

	public IMatchingPredicate<E> optimize() {
		final Queue<IMatchingPredicate<E>> queue = new LinkedList<IMatchingPredicate<E>>(
				predicates);
		for (IMatchingPredicate<E> predicate; (predicate = queue.poll()) != null;) {
			for (Iterator<IMatchingPredicate<E>> i = predicates.iterator(); i
					.hasNext();) {
				final IMatchingPredicate<E> next = i.next();
				if (predicate != next && predicate.contains(next)) {
					i.remove();
					queue.remove(next);
				}
			}
		}
		if (predicates.isEmpty()) {
			return new FalseMatchingPredicate<E>();
		} else if (predicates.size() == 1) {
			return predicates.get(0);
		} else {
			return this;
		}
	}

	public boolean contains(IMatchingPredicate<E> predicate) {
		// TODO Auto-generated method stub
		return false;
	}

}
