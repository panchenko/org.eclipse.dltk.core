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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IDLTKLanguageToolkit;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.IProjectFragment;
import org.eclipse.dltk.core.IScriptFolder;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.core.index2.search.ISearchEngine.SearchFor;
import org.eclipse.dltk.core.search.IDLTKSearchConstants;
import org.eclipse.dltk.core.search.IDLTKSearchScope;
import org.eclipse.dltk.core.search.SearchEngine;
import org.eclipse.dltk.core.search.SearchParticipant;
import org.eclipse.dltk.core.search.SearchPattern;
import org.eclipse.dltk.core.tests.model.TestSearchResults;
import org.eclipse.dltk.internal.core.util.Util;
import org.junit.Assert;
import org.junit.rules.ExternalResource;

public abstract class AbstractProjectSetup extends ExternalResource {

	public abstract IProject get();

	/**
	 * Returns this project as {@link IScriptProject}
	 */
	public IScriptProject getScriptProject() {
		return DLTKCore.create(get());
	}

	/**
	 * Returns the specified file from this project.
	 */
	public IFile getFile(String name) {
		return get().getFile(name);
	}

	/**
	 * Returns the specified file from this project.
	 */
	public IFile getFile(IPath path) {
		return get().getFile(path);
	}

	/**
	 * Returns the specified package fragment root in this project, or
	 * <code>null</code> if it does not exist. If relative, the rootPath must be
	 * specified as a project relative path. The empty path refers to the
	 * package fragment root that is the project folder iteslf. If absolute, the
	 * rootPath refers to either an external zip, or a resource internal to the
	 * workspace
	 */
	public IProjectFragment getProjectFragment(String fragmentPath)
			throws ModelException {
		final IScriptProject project = getScriptProject();
		if (project == null) {
			return null;
		}
		final IPath path = new Path(fragmentPath);
		if (path.isAbsolute()) {
			final IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace()
					.getRoot();
			final IResource resource = workspaceRoot.findMember(path);
			if (resource == null) {
				return null;
			}
			// resource in the workspace
			return project.getProjectFragment(resource);
		} else {
			final IProjectFragment[] roots = project.getProjectFragments();
			if (roots == null || roots.length == 0) {
				return null;
			}
			for (IProjectFragment root : roots) {
				if (root.getUnderlyingResource().getProjectRelativePath()
						.equals(path)) {
					return root;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the specified script folder in this project and the given
	 * fragment, or <code>null</code> if it does not exist. The rootPath must be
	 * specified as a project relative path. The empty path refers to the
	 * default package fragment.
	 */
	public IScriptFolder getScriptFolder(String fragmentPath, String path)
			throws ModelException {
		return getScriptFolder(fragmentPath, new Path(path));
	}

	/**
	 * Returns the specified script folder in this project and the given
	 * fragment, or <code>null</code> if it does not exist. The rootPath must be
	 * specified as a project relative path. The empty path refers to the
	 * default package fragment.
	 */
	public IScriptFolder getScriptFolder(String fragmentPath, IPath path)
			throws ModelException {
		final IProjectFragment root = getProjectFragment(fragmentPath);
		if (root == null) {
			return null;
		}
		return root.getScriptFolder(path);
	}

	/**
	 * Returns the specified source module in this project and the given root
	 * and folder or <code>null</code> if it does not exist.
	 */
	public ISourceModule getSourceModule(String rootPath, IPath path)
			throws ModelException {
		final IScriptFolder folder = getScriptFolder(rootPath,
				path.removeLastSegments(1));
		if (folder == null) {
			return null;
		}
		return folder.getSourceModule(path.lastSegment());
	}

	/**
	 * Returns the specified source module in this project and the given root
	 * and folder or <code>null</code> if it does not exist.
	 */
	public ISourceModule getSourceModule(String rootPath, String path)
			throws ModelException {
		return getSourceModule(rootPath, new Path(path));
	}

	public String getFileContentsAsString(String name) throws CoreException {
		return getFileContentsAsString(getFile(name));
	}

	public String getFileContentsAsString(IFile file) throws CoreException {
		return new String(Util.getResourceContentsAsCharArray(file));
	}

	/**
	 * Returns workspace this project belongs to.
	 */
	public IWorkspace getWorkspace() {
		return get().getWorkspace();
	}

	public TestSearchResults search(IModelElement element, SearchFor searchFor)
			throws CoreException {
		final SearchPattern pattern = SearchPattern.createPattern(element,
				convert(searchFor));
		return search(pattern, createSearchScope());
	}

	public IDLTKSearchScope createSearchScope() {
		return SearchEngine.createSearchScope(getScriptProject());
	}

	private static int convert(SearchFor searchFor) {
		switch (searchFor) {
		case DECLARATIONS:
			return IDLTKSearchConstants.DECLARATIONS;
		case REFERENCES:
			return IDLTKSearchConstants.REFERENCES;
		default:
			return IDLTKSearchConstants.ALL_OCCURRENCES;
		}
	}

	private TestSearchResults search(SearchPattern pattern,
			IDLTKSearchScope scope) throws CoreException {
		Assert.assertNotNull("Pattern should not be null", pattern);
		final TestSearchResults results = new TestSearchResults();
		final SearchParticipant[] participants = new SearchParticipant[] { SearchEngine
				.getDefaultSearchParticipant() };
		new SearchEngine().search(pattern, participants, scope, results, null);
		return results;
	}

	/**
	 * @param patternString
	 *            the given pattern
	 * @param searchFor
	 *            determines the nature of the searched elements
	 *            <ul>
	 *            <li>{@link IDLTKSearchConstants#CLASS}: only look for classes</li>
	 *            <li>{@link IDLTKSearchConstants#INTERFACE}: only look for
	 *            interfaces</li>
	 *            <li>{@link IDLTKSearchConstants#ENUM}: only look for
	 *            enumeration</li>
	 *            <li>{@link IDLTKSearchConstants#ANNOTATION_TYPE}: only look
	 *            for annotation type</li>
	 *            <li>{@link IDLTKSearchConstants#CLASS_AND_ENUM}: only look for
	 *            classes and enumerations</li>
	 *            <li>{@link IDLTKSearchConstants#CLASS_AND_INTERFACE}: only
	 *            look for classes and interfaces</li>
	 *            <li>{@link IDLTKSearchConstants#TYPE}: look for all types (ie.
	 *            classes, interfaces, enum and annotation types)</li>
	 *            <li>{@link IDLTKSearchConstants#FIELD}: look for fields</li>
	 *            <li>{@link IDLTKSearchConstants#METHOD}: look for methods</li>
	 *            <li>{@link IDLTKSearchConstants#CONSTRUCTOR}: look for
	 *            constructors</li>
	 *            <li>{@link IDLTKSearchConstants#PACKAGE}: look for packages</li>
	 *            </ul>
	 * @param limitTo
	 *            determines the nature of the expected matches
	 *            <ul>
	 *            <li>{@link IDLTKSearchConstants#DECLARATIONS}: will search
	 *            declarations matching with the corresponding element. In case
	 *            the element is a method, declarations of matching methods in
	 *            subtypes will also be found, allowing to find declarations of
	 *            abstract methods, etc.<br>
	 *            Note that additional flags
	 *            {@link IDLTKSearchConstants#IGNORE_DECLARING_TYPE} and
	 *            {@link IDLTKSearchConstants#IGNORE_RETURN_TYPE} are ignored
	 *            for string patterns. This is due to the fact that client may
	 *            omit to define them in string pattern to have same behavior.</li>
	 *            <li>{@link IDLTKSearchConstants#REFERENCES}: will search
	 *            references to the given element.</li>
	 *            <li>{@link IDLTKSearchConstants#ALL_OCCURRENCES}: will search
	 *            for either declarations or references as specified above.</li>
	 *            <li>{@link IDLTKSearchConstants#IMPLEMENTORS}: for types, will
	 *            find all types which directly implement/extend a given
	 *            interface. Note that types may be only classes or only
	 *            interfaces if {@link IDLTKSearchConstants#CLASS } or
	 *            {@link IDLTKSearchConstants#INTERFACE} is respectively used
	 *            instead of {@link IDLTKSearchConstants#TYPE}.</li>
	 *            </ul>
	 */
	public TestSearchResults search(String patternString, int searchFor,
			int limitTo) throws CoreException {
		return search(patternString, searchFor, limitTo,
				IDLTKSearchConstantsForTests.EXACT_RULE);
	}

	/**
	 * @param patternString
	 *            the given pattern
	 * @param searchFor
	 *            determines the nature of the searched elements
	 *            <ul>
	 *            <li>{@link IDLTKSearchConstants#CLASS}: only look for classes</li>
	 *            <li>{@link IDLTKSearchConstants#INTERFACE}: only look for
	 *            interfaces</li>
	 *            <li>{@link IDLTKSearchConstants#ENUM}: only look for
	 *            enumeration</li>
	 *            <li>{@link IDLTKSearchConstants#ANNOTATION_TYPE}: only look
	 *            for annotation type</li>
	 *            <li>{@link IDLTKSearchConstants#CLASS_AND_ENUM}: only look for
	 *            classes and enumerations</li>
	 *            <li>{@link IDLTKSearchConstants#CLASS_AND_INTERFACE}: only
	 *            look for classes and interfaces</li>
	 *            <li>{@link IDLTKSearchConstants#TYPE}: look for all types (ie.
	 *            classes, interfaces, enum and annotation types)</li>
	 *            <li>{@link IDLTKSearchConstants#FIELD}: look for fields</li>
	 *            <li>{@link IDLTKSearchConstants#METHOD}: look for methods</li>
	 *            <li>{@link IDLTKSearchConstants#CONSTRUCTOR}: look for
	 *            constructors</li>
	 *            <li>{@link IDLTKSearchConstants#PACKAGE}: look for packages</li>
	 *            </ul>
	 * @param limitTo
	 *            determines the nature of the expected matches
	 *            <ul>
	 *            <li>{@link IDLTKSearchConstants#DECLARATIONS}: will search
	 *            declarations matching with the corresponding element. In case
	 *            the element is a method, declarations of matching methods in
	 *            subtypes will also be found, allowing to find declarations of
	 *            abstract methods, etc.<br>
	 *            Note that additional flags
	 *            {@link IDLTKSearchConstants#IGNORE_DECLARING_TYPE} and
	 *            {@link IDLTKSearchConstants#IGNORE_RETURN_TYPE} are ignored
	 *            for string patterns. This is due to the fact that client may
	 *            omit to define them in string pattern to have same behavior.</li>
	 *            <li>{@link IDLTKSearchConstants#REFERENCES}: will search
	 *            references to the given element.</li>
	 *            <li>{@link IDLTKSearchConstants#ALL_OCCURRENCES}: will search
	 *            for either declarations or references as specified above.</li>
	 *            <li>{@link IDLTKSearchConstants#IMPLEMENTORS}: for types, will
	 *            find all types which directly implement/extend a given
	 *            interface. Note that types may be only classes or only
	 *            interfaces if {@link IDLTKSearchConstants#CLASS } or
	 *            {@link IDLTKSearchConstants#INTERFACE} is respectively used
	 *            instead of {@link IDLTKSearchConstants#TYPE}.</li>
	 *            </ul>
	 * @param matchRule
	 *            one of {@link #R_EXACT_MATCH}, {@link #R_PREFIX_MATCH},
	 *            {@link #R_PATTERN_MATCH}, {@link #R_REGEXP_MATCH},
	 *            {@link #R_CAMELCASE_MATCH} combined with one of following
	 *            values: {@link #R_CASE_SENSITIVE}, {@link #R_ERASURE_MATCH} or
	 *            {@link #R_EQUIVALENT_MATCH}. e.g. {@link #R_EXACT_MATCH} |
	 *            {@link #R_CASE_SENSITIVE} if an exact and case sensitive match
	 *            is requested, {@link #R_PREFIX_MATCH} if a prefix non case
	 *            sensitive match is requested or {@link #R_EXACT_MATCH} |
	 *            {@link #R_ERASURE_MATCH} if a non case sensitive and erasure
	 *            match is requested.<br>
	 *            Note that {@link #R_ERASURE_MATCH} or
	 *            {@link #R_EQUIVALENT_MATCH} have no effect on non-generic
	 *            types/methods search.<br>
	 *            Note also that default behavior for generic types/methods
	 *            search is to find exact matches.
	 */
	protected TestSearchResults search(String patternString, int searchFor,
			int limitTo, int matchRule) throws CoreException {
		final IDLTKSearchScope scope = createSearchScope();
		return search(patternString, searchFor, limitTo, matchRule, scope);
	}

	/**
	 * @param patternString
	 *            the given pattern
	 * @param searchFor
	 *            determines the nature of the searched elements
	 *            <ul>
	 *            <li>{@link IDLTKSearchConstants#CLASS}: only look for classes</li>
	 *            <li>{@link IDLTKSearchConstants#INTERFACE}: only look for
	 *            interfaces</li>
	 *            <li>{@link IDLTKSearchConstants#ENUM}: only look for
	 *            enumeration</li>
	 *            <li>{@link IDLTKSearchConstants#ANNOTATION_TYPE}: only look
	 *            for annotation type</li>
	 *            <li>{@link IDLTKSearchConstants#CLASS_AND_ENUM}: only look for
	 *            classes and enumerations</li>
	 *            <li>{@link IDLTKSearchConstants#CLASS_AND_INTERFACE}: only
	 *            look for classes and interfaces</li>
	 *            <li>{@link IDLTKSearchConstants#TYPE}: look for all types (ie.
	 *            classes, interfaces, enum and annotation types)</li>
	 *            <li>{@link IDLTKSearchConstants#FIELD}: look for fields</li>
	 *            <li>{@link IDLTKSearchConstants#METHOD}: look for methods</li>
	 *            <li>{@link IDLTKSearchConstants#CONSTRUCTOR}: look for
	 *            constructors</li>
	 *            <li>{@link IDLTKSearchConstants#PACKAGE}: look for packages</li>
	 *            </ul>
	 * @param limitTo
	 *            determines the nature of the expected matches
	 *            <ul>
	 *            <li>{@link IDLTKSearchConstants#DECLARATIONS}: will search
	 *            declarations matching with the corresponding element. In case
	 *            the element is a method, declarations of matching methods in
	 *            subtypes will also be found, allowing to find declarations of
	 *            abstract methods, etc.<br>
	 *            Note that additional flags
	 *            {@link IDLTKSearchConstants#IGNORE_DECLARING_TYPE} and
	 *            {@link IDLTKSearchConstants#IGNORE_RETURN_TYPE} are ignored
	 *            for string patterns. This is due to the fact that client may
	 *            omit to define them in string pattern to have same behavior.</li>
	 *            <li>{@link IDLTKSearchConstants#REFERENCES}: will search
	 *            references to the given element.</li>
	 *            <li>{@link IDLTKSearchConstants#ALL_OCCURRENCES}: will search
	 *            for either declarations or references as specified above.</li>
	 *            <li>{@link IDLTKSearchConstants#IMPLEMENTORS}: for types, will
	 *            find all types which directly implement/extend a given
	 *            interface. Note that types may be only classes or only
	 *            interfaces if {@link IDLTKSearchConstants#CLASS } or
	 *            {@link IDLTKSearchConstants#INTERFACE} is respectively used
	 *            instead of {@link IDLTKSearchConstants#TYPE}.</li>
	 *            </ul>
	 * @param matchRule
	 *            one of {@link #R_EXACT_MATCH}, {@link #R_PREFIX_MATCH},
	 *            {@link #R_PATTERN_MATCH}, {@link #R_REGEXP_MATCH},
	 *            {@link #R_CAMELCASE_MATCH} combined with one of following
	 *            values: {@link #R_CASE_SENSITIVE}, {@link #R_ERASURE_MATCH} or
	 *            {@link #R_EQUIVALENT_MATCH}. e.g. {@link #R_EXACT_MATCH} |
	 *            {@link #R_CASE_SENSITIVE} if an exact and case sensitive match
	 *            is requested, {@link #R_PREFIX_MATCH} if a prefix non case
	 *            sensitive match is requested or {@link #R_EXACT_MATCH} |
	 *            {@link #R_ERASURE_MATCH} if a non case sensitive and erasure
	 *            match is requested.<br>
	 *            Note that {@link #R_ERASURE_MATCH} or
	 *            {@link #R_EQUIVALENT_MATCH} have no effect on non-generic
	 *            types/methods search.<br>
	 *            Note also that default behavior for generic types/methods
	 *            search is to find exact matches.
	 * @param scope
	 */
	public TestSearchResults search(String patternString, int searchFor,
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

}
