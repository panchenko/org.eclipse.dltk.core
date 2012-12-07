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
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.dltk.compiler.CharOperation;
import org.eclipse.dltk.core.search.SearchPattern;
import org.eclipse.dltk.internal.core.search.matching.DLTKSearchPattern;

public abstract class AbstractMatchingPredicate<E> implements
		IMatchingPredicate<E> {

	private final boolean isCaseSensitive;
	private final boolean isCamelCase;
	// private final boolean isErasureMatch;
	// private final boolean isEquivalentMatch;
	private final int matchMode;

	protected final char[] namePattern;

	public AbstractMatchingPredicate(SearchPattern pattern, char[] namePattern) {
		int matchRule = pattern.getMatchRule();
		this.isCaseSensitive = (matchRule & SearchPattern.R_CASE_SENSITIVE) != 0;
		this.isCamelCase = (matchRule & SearchPattern.R_CAMELCASE_MATCH) != 0;
		// this.isErasureMatch = (matchRule & SearchPattern.R_ERASURE_MATCH) !=
		// 0;
		// this.isEquivalentMatch = (matchRule &
		// SearchPattern.R_EQUIVALENT_MATCH) != 0;
		this.matchMode = matchRule & DLTKSearchPattern.MATCH_MODE_MASK;
		this.namePattern = namePattern;
	}

	protected MatchLevel matchName(String name) {
		return matchNameValue(namePattern, name.toCharArray());
	}

	private Pattern compiledPattern;

	protected MatchLevel matchNameValue(char[] pattern, char[] name) {
		if (pattern == null)
			return MatchLevel.ACCURATE_MATCH; // null is as if it was "*"
		if (name == null)
			return null; // cannot match null name
		if (name.length == 0) { // empty name
			if (pattern.length == 0) { // can only matches empty pattern
				return MatchLevel.ACCURATE_MATCH;
			}
			return null;
		} else if (pattern.length == 0) {
			return null; // need to have both name and pattern
			// length==0 to be accurate
		}
		boolean matchFirstChar = !this.isCaseSensitive || pattern[0] == name[0];
		boolean sameLength = pattern.length == name.length;
		boolean canBePrefix = name.length >= pattern.length;
		if (this.isCamelCase && matchFirstChar
				&& CharOperation.camelCaseMatch(pattern, name)) {
			return MatchLevel.POSSIBLE_MATCH;
		}
		switch (this.matchMode) {
		case SearchPattern.R_EXACT_MATCH:
			if (!this.isCamelCase) {
				if (sameLength
						&& matchFirstChar
						&& CharOperation.equals(pattern, name,
								this.isCaseSensitive)) {
					return MatchLevel.POSSIBLE_MATCH;
				}
				break;
			}
			// fall through next case to match as prefix if camel case
			// failed
		case SearchPattern.R_PREFIX_MATCH:
			if (canBePrefix
					&& matchFirstChar
					&& CharOperation.prefixEquals(pattern, name,
							this.isCaseSensitive)) {
				return MatchLevel.POSSIBLE_MATCH;
			}
			break;
		case SearchPattern.R_PATTERN_MATCH:
			if (!this.isCaseSensitive) {
				pattern = CharOperation.toLowerCase(pattern);
			}
			if (CharOperation.match(pattern, name, this.isCaseSensitive)) {
				return MatchLevel.POSSIBLE_MATCH;
			}
			break;
		case SearchPattern.R_REGEXP_MATCH:
			if (compiledPattern == null) {
				compiledPattern = Pattern.compile(new String(pattern),
						this.isCaseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
			}
			if (compiledPattern.matcher(new String(name)).matches()) {
				return MatchLevel.POSSIBLE_MATCH;
			}
			break;
		}
		return null;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName());
		sb.append("(");
		sb.append(namePattern != null ? new String(namePattern) : "*");
		final List<String> options = new ArrayList<String>();
		collectToStringOptions(options);
		if (!options.isEmpty()) {
			sb.append(":");
			boolean first = true;
			for (String option : options) {
				if (!first) {
					sb.append(',');
				}
				sb.append(option);
				first = false;
			}
		}
		sb.append(")");
		return sb.toString();
	}

	protected void collectToStringOptions(List<String> options) {
	}

	public MatchLevel resolvePotentialMatch(E node) {
		return MatchLevel.INACCURATE_MATCH;
	}

	public boolean contains(IMatchingPredicate<E> predicate) {
		return false;
	}

	/**
	 * Calls {@link #matchName(String)} and upgrades
	 * {@link MatchLevel#POSSIBLE_MATCH} to the specified value.
	 */
	protected MatchLevel matchName(String name, MatchLevel upgraded) {
		final MatchLevel level = matchName(name);
		return level == MatchLevel.POSSIBLE_MATCH ? upgraded : level;
	}

}
