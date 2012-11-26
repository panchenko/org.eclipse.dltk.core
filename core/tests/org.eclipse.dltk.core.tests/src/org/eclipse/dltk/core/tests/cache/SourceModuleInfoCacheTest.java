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
package org.eclipse.dltk.core.tests.cache;

import static org.hamcrest.CoreMatchers.not;

import java.io.ByteArrayInputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.dltk.annotations.Internal;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.IScriptFolder;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.ISourceModuleInfoCache;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.core.SourceParserUtil;
import org.eclipse.dltk.core.tests.ProjectSetup;
import org.eclipse.dltk.core.tests.model.ModelTestsPlugin;
import org.eclipse.dltk.internal.core.ModelManager;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class SourceModuleInfoCacheTest extends Assert {

	@Rule
	public final ProjectSetup project = new ProjectSetup(
			ModelTestsPlugin.WORKSPACE, "cache");

	@Before
	public void clearCache() {
		getCache().clear();
	}

	@Internal
	static ISourceModuleInfoCache getCache() {
		return ModelManager.getModelManager().getSourceModuleInfoCache();
	}

	private static final Matcher<ISourceModule> IS_CACHED = new BaseMatcher<ISourceModule>() {
		public void describeTo(Description description) {
			description.appendText("ISourceModule contained in AST cache");
		}

		public boolean matches(Object item) {
			return !getCache().get((ISourceModule) item).isEmpty();
		}
	};

	@Test
	public void cacheLoaded() throws CoreException {
		ISourceModule module = project.getSourceModule("src", "folder1/a.txt");
		assertNotNull(module);
		assertTrue(module.exists());
		assertNotNull(SourceParserUtil.parse(module, null));
		assertThat(module, IS_CACHED);
	}

	@Test
	public void moduleModify() throws CoreException {
		ISourceModule module = project.getSourceModule("src", "folder1/a.txt");
		assertNotNull(module);
		assertTrue(module.exists());
		assertNotNull(SourceParserUtil.parse(module, null));
		assertThat(module, IS_CACHED);

		final IFile file = (IFile) module.getResource();
		final String contents = project.getFileContentsAsString(file);
		file.setContents(
				new ByteArrayInputStream((contents + "//change\n").getBytes()),
				IResource.NONE, null);
		assertThat(module, not(IS_CACHED));
	}

	@Test
	public void moduleDelete() throws ModelException {
		ISourceModule module = project.getSourceModule("src", "folder1/a.txt");
		assertNotNull(module);
		assertTrue(module.exists());
		assertNotNull(SourceParserUtil.parse(module, null));
		assertThat(module, IS_CACHED);

		module.delete(true, null);
		assertFalse(module.exists());
		assertThat(module, not(IS_CACHED));
	}

	@Ignore
	@Test
	public void folderDelete() throws ModelException {
		ISourceModule module = project.getSourceModule("src", "folder1/a.txt");
		assertNotNull(module);
		assertTrue(module.exists());
		assertNotNull(SourceParserUtil.parse(module, null));
		assertThat(module, IS_CACHED);

		((IScriptFolder) module.getAncestor(IModelElement.SCRIPT_FOLDER))
				.delete(true, null);
		assertFalse(module.exists());
		assertThat(module, not(IS_CACHED));
	}

	@Ignore
	@Test
	public void projectDelete() throws CoreException {
		ISourceModule module = project.getSourceModule("src", "folder1/a.txt");
		assertNotNull(module);
		assertTrue(module.exists());
		assertNotNull(SourceParserUtil.parse(module, null));
		assertThat(module, IS_CACHED);

		project.get().delete(true, null);
		assertFalse(module.exists());
		assertThat(module, not(IS_CACHED));
	}

}
