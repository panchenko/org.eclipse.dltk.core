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
package org.eclipse.dltk.core.tests.search;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.compiler.util.Util;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IBuildpathEntry;
import org.eclipse.dltk.core.IMethod;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.IModelElementVisitor;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.IType;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.core.WorkingCopyOwner;
import org.eclipse.dltk.core.environment.EnvironmentPathUtils;
import org.eclipse.dltk.core.internal.environment.LocalEnvironment;
import org.eclipse.dltk.core.search.IDLTKSearchConstants;
import org.eclipse.dltk.core.search.IDLTKSearchScope;
import org.eclipse.dltk.core.search.SearchEngine;
import org.eclipse.dltk.core.search.SearchPattern;
import org.eclipse.dltk.core.search.TypeNameMatch;
import org.eclipse.dltk.core.search.TypeNameMatchRequestor;
import org.eclipse.dltk.core.tests.ProjectSetup;
import org.eclipse.dltk.core.tests.model.ModelTestsPlugin;
import org.eclipse.dltk.internal.core.BuildpathEntry;
import org.eclipse.dltk.internal.core.search.DLTKSearchScope;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test for bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=387751
 * <p>
 * Tests for {@link DLTKSearchScope#projectFragment(String)} method with scope
 * containing external source modules are performed indirectly via
 * {@link SearchEngine#searchAllTypeNames()} method, which also uses it.
 * </p>
 */
public class Bug387751Test extends Assert {

	@Rule
	public final ProjectSetup project = new ProjectSetup(
			ModelTestsPlugin.WORKSPACE, "bug387751");

	@Rule
	public final TemporaryFolder temp = new TemporaryFolder();

	/**
	 * Precondition: validates the model of the test file
	 */
	@Test
	public void validateTestModuleContents() throws ModelException {
		final ISourceModule module = project.getSourceModule("", "test.txt");
		assertNotNull(module);
		assertTrue(module.exists());
		final IModelElement[] types = module.getChildren();
		assertEquals(1, types.length);
		final IType type = (IType) types[0];
		assertEquals("Foo", type.getElementName());
		final IModelElement[] methods = type.getChildren();
		assertEquals(1, methods.length);
		final IMethod method = (IMethod) methods[0];
		assertEquals("bar", method.getElementName());
		assertEquals(0, method.getChildren().length);
	}

	/**
	 * Precondition: validates that {@link SearchEngine#searchAllTypeNames()}
	 * works
	 */
	@Test
	public void searchProject() throws IOException, CoreException {
		final List<IType> types = searchAllTypeNames(
				SearchEngine.createSearchScope(project.getScriptProject()),
				"Foo");
		assertEquals(1, types.size());
		assertEquals(project.getSourceModule("", "test.txt"), types.get(0)
				.getSourceModule());
	}

	/**
	 * Validates that search works in external folders when scope is created for
	 * the project
	 */
	@Test
	public void searchExternalLibraryProjectScope() throws IOException,
			CoreException {
		final IFile file = project.getFile("test.txt");
		addExternalLibraryFromFile(file, file.getName());
		file.delete(true, null);

		final List<ISourceModule> modules = listModules();
		assertEquals(1, modules.size());
		final ISourceModule module = modules.get(0);

		final List<IType> types = searchAllTypeNames(
				SearchEngine.createSearchScope(project.getScriptProject()),
				"Foo");
		assertEquals(1, types.size());
		assertEquals(module, types.get(0).getSourceModule());
	}

	/**
	 * Validates that search works in external folders when scope is created for
	 * the external module
	 */
	@Test
	public void searchExternalLibraryModuleScope() throws IOException,
			CoreException {
		final IFile file = project.getFile("test.txt");
		addExternalLibraryFromFile(file, file.getName());
		file.delete(true, null);

		final List<ISourceModule> modules = listModules();
		assertEquals(1, modules.size());
		final ISourceModule module = modules.get(0);

		final List<IType> types = searchAllTypeNames(
				SearchEngine.createSearchScope(module), "Foo");
		assertEquals(1, types.size());
		assertEquals(module, types.get(0).getSourceModule());
	}

	/**
	 * Validates that search works in external folders when scope is created for
	 * the project - more complicated setup with 2 entries and exclude patterns.
	 */
	@Test
	public void searchExternalLibraryModuleScopeWithExcludes()
			throws IOException, CoreException {
		final IFile file = project.getFile("test.txt");
		temp.newFolder("tests");
		addExternalLibraryFromFile(file, "tests/" + file.getName());
		file.delete(true, null);
		addBuildpathEntry(project.getScriptProject(), DLTKCore.newLibraryEntry(
				getFullPath(temp.getRoot()), BuildpathEntry.NO_ACCESS_RULES,
				BuildpathEntry.NO_EXTRA_ATTRIBUTES, BuildpathEntry.INCLUDE_ALL,
				new IPath[] { new Path("tests/") }, false /* not exported */,
				true));

		final List<ISourceModule> modules = listModules();
		assertEquals(1, modules.size());
		final ISourceModule module = modules.get(0);

		final List<IType> types = searchAllTypeNames(
				SearchEngine.createSearchScope(module), "Foo");
		assertEquals(1, types.size());
		assertEquals(module, types.get(0).getSourceModule());
	}

	private List<IType> searchAllTypeNames(final IDLTKSearchScope scope,
			final String name) throws ModelException {
		final List<IType> types = new ArrayList<IType>();
		new SearchEngine((WorkingCopyOwner) null).searchAllTypeNames(null, 0,
				name.toCharArray(), SearchPattern.R_EXACT_MATCH
						| SearchPattern.R_CASE_SENSITIVE,
				IDLTKSearchConstants.TYPE, scope, new TypeNameMatchRequestor() {
					@Override
					public void acceptTypeNameMatch(TypeNameMatch match) {
						types.add(match.getType());
					}
				}, IDLTKSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, null);
		return types;
	}

	private List<ISourceModule> listModules() throws ModelException {
		final List<ISourceModule> modules = new ArrayList<ISourceModule>();
		project.getScriptProject().accept(new IModelElementVisitor() {
			public boolean visit(IModelElement element) {
				if (element instanceof ISourceModule) {
					modules.add((ISourceModule) element);
				}
				return element.getElementType() < IModelElement.SOURCE_MODULE;
			}
		});
		return modules;
	}

	private void addExternalLibraryFromFile(IFile file, String filename)
			throws IOException, CoreException {
		final File externalFile = new File(temp.getRoot(), filename);
		final InputStream input = file.getContents();
		try {
			Util.copy(externalFile, input);
		} finally {
			try {
				input.close();
			} catch (IOException e) {
				// ignore
			}
		}
		addBuildpathEntry(project.getScriptProject(),
				DLTKCore.newExtLibraryEntry(getFullPath(externalFile
						.getParentFile())));
	}

	private IPath getFullPath(File file) {
		return EnvironmentPathUtils.getFullPath(LocalEnvironment.getInstance(),
				new Path(file.getAbsolutePath()));
	}

	private void addBuildpathEntry(IScriptProject scriptProject,
			IBuildpathEntry entry) throws ModelException {
		final List<IBuildpathEntry> buildpath = new ArrayList<IBuildpathEntry>();
		buildpath.add(entry);
		Collections.addAll(buildpath, scriptProject.getRawBuildpath());
		scriptProject.setRawBuildpath(
				buildpath.toArray(new IBuildpathEntry[buildpath.size()]), null);
	}
}
