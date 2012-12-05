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
package org.eclipse.dltk.core.tests.model;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.dltk.core.IDLTKLanguageToolkit;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.search.IDLTKSearchConstants;
import org.eclipse.dltk.core.search.IDLTKSearchScope;
import org.eclipse.dltk.core.search.SearchEngine;
import org.eclipse.dltk.core.search.SearchParticipant;
import org.eclipse.dltk.core.search.SearchPattern;
import org.eclipse.dltk.core.tests.IDLTKSearchConstantsForTests;
import org.eclipse.dltk.core.tests.ProjectSetup;

/**
 * Don't use this class for the new tests, instead implement them in JUnit4 way
 * using {@link ProjectSetup} annotated with {@link org.junit.ClassRule}.
 */
@Deprecated
public class AbstractSingleProjectSearchTests extends AbstractModelTests
		implements IDLTKSearchConstants, IDLTKSearchConstantsForTests {

	private final String scriptProjectName;

	protected AbstractSingleProjectSearchTests(String testPluginName,
			String testName, String scriptProjectName) {
		super(testPluginName, testName);
		this.scriptProjectName = scriptProjectName;
	}

	public void setUpSuite() throws Exception {
		super.setUpSuite();
		setUpScriptProject(scriptProjectName);
		waitUntilIndexesReady();
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		if (!getScriptProject().exists()) {
			setUpSuite();
		}
	}

	protected String getProjectName() {
		return scriptProjectName;
	}

	protected IScriptProject getScriptProject() {
		return getScriptProject(scriptProjectName);
	}

	public void tearDownSuite() throws Exception {
		deleteProject(scriptProjectName);
		super.tearDownSuite();
	}

	protected TestSearchResults search(String patternString, int searchFor,
			int limitTo) throws CoreException {
		return search(patternString, searchFor, limitTo, EXACT_RULE);
	}

	protected TestSearchResults search(IModelElement element, int limitTo)
			throws CoreException {
		final IDLTKSearchScope scope = SearchEngine
				.createSearchScope(getScriptProject(scriptProjectName));
		final SearchPattern pattern = SearchPattern.createPattern(element,
				limitTo);
		return search(pattern, scope);
	}

	protected TestSearchResults search(String patternString, int searchFor,
			int limitTo, int matchRule) throws CoreException {
		final IDLTKSearchScope scope = SearchEngine
				.createSearchScope(getScriptProject(scriptProjectName));
		return search(patternString, searchFor, limitTo, matchRule, scope);
	}

	protected TestSearchResults search(String patternString, int searchFor,
			int limitTo, int matchRule, final IDLTKSearchScope scope)
			throws CoreException {
		if (patternString.indexOf('*') != -1
				|| patternString.indexOf('?') != -1) {
			matchRule |= SearchPattern.R_PATTERN_MATCH;
		}
		final IDLTKLanguageToolkit toolkit = scope.getLanguageToolkit();
		final SearchPattern pattern = SearchPattern.createPattern(
				patternString, searchFor, limitTo, matchRule, toolkit);
		return search(pattern, scope);
	}

	private TestSearchResults search(SearchPattern pattern,
			IDLTKSearchScope scope) throws CoreException {
		assertNotNull("Pattern should not be null", pattern);
		final TestSearchResults results = new TestSearchResults();
		final SearchParticipant[] participants = new SearchParticipant[] { SearchEngine
				.getDefaultSearchParticipant() };
		new SearchEngine().search(pattern, participants, scope, results, null);
		return results;
	}

}
