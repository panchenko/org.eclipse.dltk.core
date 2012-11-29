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

import static org.eclipse.dltk.core.SourceParserUtil.getCache;
import static org.hamcrest.CoreMatchers.not;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.IScriptFolder;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.core.SourceParserUtil;
import org.eclipse.dltk.core.tests.ProjectSetup;
import org.eclipse.dltk.core.tests.model.ModelTestsPlugin;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Before;
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
	public void moduleCreate() throws CoreException {
		final ISourceModule module = project.getSourceModule("src",
				"folder1/new.txt");
		assertNotNull(module);
		assertNotNull(SourceParserUtil.parse(module, null));
		assertThat(module, IS_CACHED);
		assertEquals(1, getCache().size());
		((IFile) module.getResource()).create(new ByteArrayInputStream(
				new byte[0]), false, null);
		// assertEquals(0, getCache().size());
		assertThat(module, not(IS_CACHED));
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
	public void moduleReplace() throws CoreException {
		ISourceModule module = project.getSourceModule("src", "folder1/a.txt");
		assertNotNull(module);
		assertTrue(module.exists());
		assertNotNull(SourceParserUtil.parse(module, null));
		assertThat(module, IS_CACHED);

		final IFile file = (IFile) module.getResource();
		project.getWorkspace().run(new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				file.delete(true, monitor);
				file.create(
						new ByteArrayInputStream(("//new content\n").getBytes()),
						IResource.NONE, monitor);
			}
		}, null);
		assertEquals(0, getCache().size());
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

	@Test
	public void folderDelete() throws ModelException {
		ISourceModule module = project.getSourceModule("src", "folder1/a.txt");
		assertNotNull(module);
		assertTrue(module.exists());
		assertNotNull(SourceParserUtil.parse(module, null));
		assertThat(module, IS_CACHED);

		((IScriptFolder) module.getAncestor(IModelElement.SCRIPT_FOLDER))
				.delete(true, null);
		assertEquals(0, getCache().size());
		assertFalse(module.exists());
		assertThat(module, not(IS_CACHED));
	}

	@Test
	public void projectDelete() throws CoreException {
		ISourceModule module = project.getSourceModule("src", "folder1/a.txt");
		assertNotNull(module);
		assertTrue(module.exists());
		assertNotNull(SourceParserUtil.parse(module, null));
		assertThat(module, IS_CACHED);

		project.get().delete(true, null);
		assertEquals(0, getCache().size());
		assertFalse(module.exists());
		assertThat(module, not(IS_CACHED));
	}

	@Test
	public void projectClose() throws CoreException {
		ISourceModule module = project.getSourceModule("src", "folder1/a.txt");
		assertNotNull(module);
		assertTrue(module.exists());
		assertNotNull(SourceParserUtil.parse(module, null));
		assertThat(module, IS_CACHED);

		project.get().close(null);
		assertEquals(0, getCache().size());
	}

	@Test
	public void overflow() throws CoreException {
		final IScriptFolder folder = project.getScriptFolder("src", "folder1");
		assertNotNull(folder);
		final int capacity = getCache().capacity();
		final List<ISourceModule> modules = new ArrayList<ISourceModule>();
		project.getWorkspace().run(new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				for (int i = 0; i < capacity; ++i) {
					modules.add(folder.createSourceModule("m" + i + ".txt", "",
							false, null));
				}
			}
		}, null);
		for (ISourceModule module : modules) {
			assertNotNull(SourceParserUtil.parse(module, null));
		}
		for (ISourceModule module : modules) {
			assertThat(module, IS_CACHED);
		}
		final ISourceModule a = project.getSourceModule("src", "folder1/a.txt");
		assertNotNull(a);
		assertTrue(a.exists());
		assertNotNull(SourceParserUtil.parse(a, null));
		assertThat(a, IS_CACHED);
		assertThat(modules.get(0), not(IS_CACHED));
	}

}
