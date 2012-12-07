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

import org.eclipse.dltk.core.search.SearchMatch;

public enum MatchLevel {
	// /**
	// * 0%
	// */
	// IMPOSSIBLE_MATCH,

	/**
	 * 50%
	 */
	POSSIBLE_MATCH,

	/**
	 * 75%
	 */
	INACCURATE_MATCH,

	/**
	 * 100%
	 */
	ACCURATE_MATCH;

	public int toSearchMatchAccuracy() {
		switch (this) {
		case ACCURATE_MATCH:
			return SearchMatch.A_ACCURATE;
		case INACCURATE_MATCH:
			return SearchMatch.A_INACCURATE;
		default:
			return -1;
		}
	}
}
