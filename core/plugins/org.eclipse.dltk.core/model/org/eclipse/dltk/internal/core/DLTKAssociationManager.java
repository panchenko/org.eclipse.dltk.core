/*******************************************************************************
 * Copyright (c) 2008 xored software, Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     xored software, Inc. - initial API and Implementation (Alex Panchenko)
 *******************************************************************************/
package org.eclipse.dltk.internal.core;

import java.util.Set;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.dltk.compiler.CharOperation;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.DLTKLanguageManager;
import org.eclipse.dltk.core.IDLTKAssociationManager;

public class DLTKAssociationManager implements IDLTKAssociationManager {

	private final String natureId;
	private final String qualifier;

	public DLTKAssociationManager(String natureId, String qualifier) {
		this.natureId = natureId;
		this.qualifier = qualifier;
	}

	private char[][] cachedPatterns = null;

	public boolean isAssociatedWith(String name) {
		char[][] patterns;
		synchronized (this) {
			if (cachedPatterns == null) {
				final IEclipsePreferences prefs = getEclipsePreferences();
				initPatterns(prefs.get(DLTKCore.LANGUAGE_FILENAME_ASSOCIATIONS,
						new DefaultScope().getNode(qualifier).get(
								DLTKCore.LANGUAGE_FILENAME_ASSOCIATIONS, null)));
			}
			patterns = cachedPatterns;
		}
		if (patterns.length != 0) {
			for (int i = 0; i < patterns.length; ++i) {
				final char[] pattern = patterns[i];
				if (match(pattern, name)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * @param value
	 */
	private void initPatterns(String value) {
		final Set<String> patterns = DLTKLanguageManager
				.loadFilenameAssociations(this.natureId);
		if (value != null && value.length() != 0 || !patterns.isEmpty()) {
			char[][] patterns1 = value != null && value.length() != 0 ? CharOperation
					.splitOn(DLTKCore.LANGUAGE_FILENAME_ASSOCIATION_SEPARATOR,
							value.toCharArray())
					: null;
			char[][] patterns2 = !patterns.isEmpty() ? CharOperation
					.stringArrayToCharCharArray(patterns
							.toArray(new String[patterns.size()])) : null;
			cachedPatterns = CharOperation.arrayConcat(patterns1, patterns2);
		} else {
			cachedPatterns = CharOperation.NO_CHAR_CHAR;
		}
	}

	private static final boolean match(char[] pattern, String name) {
		final int patternEnd = pattern.length;
		final int nameEnd = name.length();
		int iPattern = 0; // patternStart;
		int iName = 0; // nameStart

		/* check first segment */
		char patternChar = 0;
		while ((iPattern < patternEnd)
				&& (patternChar = pattern[iPattern]) != '*') {
			if (iName == nameEnd)
				return false;
			if (patternChar != name.charAt(iName) && patternChar != '?') {
				return false;
			}
			iName++;
			iPattern++;
		}
		/* check sequence of star+segment */
		int segmentStart;
		if (patternChar == '*') {
			segmentStart = ++iPattern; // skip star
		} else {
			segmentStart = 0; // force iName check
		}
		int prefixStart = iName;
		checkSegment: while (iName < nameEnd) {
			if (iPattern == patternEnd) {
				iPattern = segmentStart; // mismatch - restart current
				// segment
				iName = ++prefixStart;
				continue checkSegment;
			}
			/* segment is ending */
			if ((patternChar = pattern[iPattern]) == '*') {
				segmentStart = ++iPattern; // skip start
				if (segmentStart == patternEnd) {
					return true;
				}
				prefixStart = iName;
				continue checkSegment;
			}
			/* check current name character */
			if (Character.toLowerCase(name.charAt(iName)) != patternChar
					&& patternChar != '?') {
				iPattern = segmentStart; // mismatch - restart current
				// segment
				iName = ++prefixStart;
				continue checkSegment;
			}
			iName++;
			iPattern++;
		}

		return (segmentStart == patternEnd)
				|| (iName == nameEnd && iPattern == patternEnd)
				|| (iPattern == patternEnd - 1 && pattern[iPattern] == '*');
	}

	private final Object preferencesLock = new Object();

	private IEclipsePreferences preferences = null;

	private IEclipsePreferences getEclipsePreferences() {
		synchronized (preferencesLock) {
			if (preferences != null) {
				return preferences;
			}
		}
		final IEclipsePreferences eclipsePreferences = InstanceScope.INSTANCE
				.getNode(qualifier);
		synchronized (preferencesLock) {
			this.preferences = eclipsePreferences;
		}
		// Listen to node removal from parent in order to reset cache (see bug
		// 68993)
		IEclipsePreferences.INodeChangeListener nodeListener = new IEclipsePreferences.INodeChangeListener() {
			public void added(IEclipsePreferences.NodeChangeEvent event) {
				// do nothing
			}

			public void removed(IEclipsePreferences.NodeChangeEvent event) {
				if (event.getChild() == eclipsePreferences) {
					synchronized (preferencesLock) {
						DLTKAssociationManager.this.preferences = null;
					}
				}
			}
		};
		((IEclipsePreferences) eclipsePreferences.parent())
				.addNodeChangeListener(nodeListener);
		// Listen to preference changes
		IEclipsePreferences.IPreferenceChangeListener preferenceListener = new IEclipsePreferences.IPreferenceChangeListener() {
			public void preferenceChange(
					IEclipsePreferences.PreferenceChangeEvent event) {
				synchronized (this) {
					initPatterns((String) event.getNewValue());
				}
			}
		};
		eclipsePreferences.addPreferenceChangeListener(preferenceListener);
		return eclipsePreferences;
	}

}
