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

public interface IMatchingPredicate<E> {

	/**
	 * Tests matching of the specified node to this predicate
	 * 
	 * @param node
	 * @return match level or <code>null</code> if not matches
	 */
	MatchLevel match(E node);

	/**
	 * Answers whether this predicate completely contains another predicate.
	 * 
	 * @since 5.0
	 */
	public boolean contains(IMatchingPredicate<E> predicate);

	/**
	 * Resolves the potential matching after the node was enhanced with the
	 * additional information. This method is called for the nodes, for which
	 * {@link MatchLevel#POSSIBLE_MATCH} was previously returned from
	 * {@link #match(Object)}.
	 * 
	 * @since 5.0
	 */
	MatchLevel resolvePotentialMatch(E node);

}
