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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.dltk.internal.core.ModelManager;
import org.eclipse.dltk.utils.TextUtils;
import org.junit.Assert;

/**
 * The ProjectSetup Rule provides convenient way of creating workspace project
 * based on prepared template in JUnit4 tests:
 * 
 * <pre>
 * &#064;ClassRule
 * public static final ProjectSetup PROJECT = new ProjectSetup(....);
 * </pre>
 * 
 * In JUnit versions before 4.9 &#064;ClassRule was not available, so instead of
 * it, {@link #create(ProjectSetup...)} and {@link #delete(ProjectSetup...)}
 * methods should be called manually from the methods annotated with
 * &#064;BeforeClas and &#064;AfterClass.
 */
public class ProjectSetup extends AbstractProjectSetup {

	public static enum Option {
		BUILD, INDEXER_DISABLED, WAIT_INDEXES_READY
	}

	/**
	 * This method implements workaround when using this class in JUnit before
	 * 4.9 (without &#64;ClassRule) and should be called from the method
	 * annotated with &#64;BeforeClass
	 */
	public static void create(ProjectSetup... projects) throws Throwable {
		for (ProjectSetup project : projects) {
			project.before();
		}
	}

	/**
	 * This method implements workaround when using this class in JUnit before
	 * 4.9 (without &#64;ClassRule) and should be called from the method
	 * annotated with &#64;AfterClass
	 */
	public static void delete(ProjectSetup... projects) {
		for (ProjectSetup project : projects) {
			project.after();
		}
	}

	private final IWorkspaceSetup workspaceSetup;
	private final String projectName;
	private BundledProjectSetup.Helper helper;
	private IProject project;
	private final Set<Option> options;

	public ProjectSetup(IWorkspaceSetup workspaceSetup, String projectName) {
		this.workspaceSetup = workspaceSetup;
		this.projectName = projectName;
		this.options = EnumSet.noneOf(Option.class);
	}

	public ProjectSetup(IWorkspaceSetup workspaceSetup, String projectName,
			Option option, Option... restOptions) {
		this.workspaceSetup = workspaceSetup;
		this.projectName = projectName;
		this.options = EnumSet.of(option, restOptions);
		if (options.contains(Option.INDEXER_DISABLED)
				&& options.contains(Option.WAIT_INDEXES_READY)) {
			throw new IllegalStateException("Conflicting options: "
					+ TextUtils.join(Arrays.asList(Option.INDEXER_DISABLED,
							Option.WAIT_INDEXES_READY), ","));
		}
	}

	protected boolean isVerbose() {
		return false;
	}

	@Override
	protected void before() throws Throwable {
		workspaceSetup.before();
		if (options.contains(Option.INDEXER_DISABLED)) {
			ModelManager.getModelManager().getIndexManager().disable();
		}
		helper = new BundledProjectSetup.Helper(workspaceSetup.getBundleName());
		project = helper.setUpProject(projectName);
		if (options.contains(Option.BUILD)) {
			final long start = System.currentTimeMillis();
			get().build(IncrementalProjectBuilder.FULL_BUILD, null);
			if (isVerbose()) {
				System.out.println((System.currentTimeMillis() - start)
						+ " ms to build " + projectName + " project");
			}
		}
		if (options.contains(Option.WAIT_INDEXES_READY)) {
			ModelManager.getModelManager().getIndexManager().waitUntilReady();
		}
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
		if (options.contains(Option.INDEXER_DISABLED)) {
			ModelManager.getModelManager().getIndexManager().enable();
		}
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

}
