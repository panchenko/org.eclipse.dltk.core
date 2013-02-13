/*******************************************************************************
 * Copyright (c) 2012 NumberFour AG
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     NumberFour AG - initial API and Implementation (Alex Panchenko)
 *******************************************************************************/
package org.eclipse.dltk.core.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.dltk.compiler.problem.DefaultProblem;
import org.eclipse.dltk.compiler.problem.DefaultProblemIdentifier;
import org.eclipse.dltk.compiler.problem.IProblemIdentifier;
import org.eclipse.dltk.core.IScriptModelMarker;
import org.eclipse.dltk.internal.core.util.Util;
import org.eclipse.dltk.utils.CharArraySequence;
import org.eclipse.dltk.utils.TextUtils;
import org.junit.Assert;

/**
 * Static methods for problem marker validation in JUnit 4 tests. Can be used as
 * supertype of the test class.
 */
public class ProblemTestUtil extends Assert {

	/**
	 * Returns all the problem markers of the specified resource, the markers
	 * are order by the start offset.
	 */
	public static IMarker[] findProblems(IResource resource)
			throws CoreException {
		final IMarker[] problems = resource.findMarkers(
				DefaultProblem.MARKER_TYPE_PROBLEM, true,
				IResource.DEPTH_INFINITE);
		Arrays.sort(problems, new Comparator<IMarker>() {
			public int compare(IMarker o1, IMarker o2) {
				return o1.getAttribute(IMarker.CHAR_START, 0)
						- o2.getAttribute(IMarker.CHAR_START, 0);
			}
		});
		return problems;
	}

	/**
	 * Returns problem identifier from the specified problem marker.
	 */
	public static IProblemIdentifier getProblemId(final IMarker problem) {
		return DefaultProblemIdentifier.decode(problem.getAttribute(
				IScriptModelMarker.ID, null));
	}

	/**
	 * Returns message from the specified problem marker.
	 */
	public static String getMessage(final IMarker problem) {
		return problem.getAttribute(IMarker.MESSAGE, null);
	}

	/**
	 * Asserts that specified resource doesn't have any problem markers.
	 */
	public static void assertNoProblems(IResource resource)
			throws CoreException {
		final IMarker[] problems = findProblems(resource);
		assertEquals(resource.getName() + ":" + toString(problems), 0,
				problems.length);
	}

	/**
	 * Asserts the expected number of problems in the specified array.
	 */
	public static void assertProblemCount(int expectedCount, IMarker[] problems) {
		assertEquals(toString(problems), expectedCount, problems.length);
	}

	/**
	 * Returns string representation of the specified problem marker.
	 */
	public static String toString(IMarker marker) {
		final StringBuilder sb = new StringBuilder();
		sb.append(marker.getResource().getFullPath()).append(':');
		final IProblemIdentifier id = getProblemId(marker);
		if (id != null) {
			sb.append(id.name()).append(":");
		}
		final String message = marker.getAttribute(IMarker.MESSAGE, "?");
		sb.append(message);
		final int lineNumber = marker.getAttribute(IMarker.LINE_NUMBER, 0);
		if (lineNumber > 0) {
			sb.append(":").append(lineNumber);
		}
		return sb.toString();
	}

	/**
	 * @param markers
	 * @return messages values concatenated with ',', e.g
	 *         "message1,message2,message3"
	 */
	public static String toString(IMarker[] markers) {
		final List<String> text = new ArrayList<String>();
		for (IMarker marker : markers) {
			text.add(toString(marker));
		}
		return TextUtils.join(text, ", ");
	}

	/**
	 * Returns the text of the line referenced by the specified marker.
	 */
	public static String getLine(IMarker marker) throws CoreException {
		final IFile file = (IFile) marker.getResource();
		final int lineNumber = marker.getAttribute(IMarker.LINE_NUMBER, 0) - 1;
		assertTrue("Marker doesn't have line number", lineNumber >= 0);
		final String[] lines = TextUtils.splitLines(new CharArraySequence(Util
				.getResourceContentsAsCharArray(file)));
		return lines[lineNumber];
	}

	/**
	 * Returns project with the specified name.
	 */
	protected static IProject getProject(String name) {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(name);
	}

}
