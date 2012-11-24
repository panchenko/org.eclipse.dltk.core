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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.dltk.internal.core.util.Util;
import org.junit.Assert;

// TODO waitUntilIndexesReady
// TODO DISABLE_INDEXER
// TODO BUILD
public class ProjectSetup extends AbstractProjectSetup {

	public static void create(ProjectSetup... projects) throws Throwable {
		for (ProjectSetup project : projects) {
			project.before();
		}
	}

	public static void delete(ProjectSetup... projects) {
		for (ProjectSetup project : projects) {
			project.after();
		}
	}

	private final IWorkspaceSetup workspaceSetup;
	private final String projectName;
	private BundledProjectSetup.Helper helper;
	private IProject project;

	public ProjectSetup(IWorkspaceSetup workspaceSetup, String projectName) {
		this.workspaceSetup = workspaceSetup;
		this.projectName = projectName;
	}

	@Override
	protected void before() throws Throwable {
		workspaceSetup.before();
		helper = new BundledProjectSetup.Helper(workspaceSetup.getBundleName());
		project = helper.setUpProject(projectName);
	}

	@Override
	protected void after() {
		if (helper != null) {
			try {
				helper.deleteProject(projectName);
			} catch (CoreException e) {
				e.printStackTrace();
			}
			helper = null;
		}
		project = null;
		workspaceSetup.after();
	}

	public String getProjectName() {
		return projectName;
	}

	/**
	 * Returns this project as IProject
	 */
	@Override
	public IProject get() {
		Assert.assertNotNull(
				"ProjectSetup " + projectName + " not initialized", project);
		return project;
	}

	public String getFileContentsAsString(String name) throws CoreException {
		return getFileContentsAsString(getFile(name));
	}

	public String getFileContentsAsString(final IFile file)
			throws CoreException {
		return new String(Util.getResourceContentsAsCharArray(file));
	}
}
