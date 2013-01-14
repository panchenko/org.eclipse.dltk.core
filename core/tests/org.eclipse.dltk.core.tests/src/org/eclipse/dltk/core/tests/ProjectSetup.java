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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.dltk.core.tests.model.AbstractModelTests;
import org.eclipse.dltk.internal.core.ModelManager;
import org.eclipse.dltk.utils.TextUtils;
import org.eclipse.osgi.util.NLS;
import org.junit.Assert;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

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
		BUILD, INDEXER_DISABLED, WAIT_INDEXES_READY, VERBOSE, CLOSED
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
		if (hasOption(Option.INDEXER_DISABLED)
				&& hasOption(Option.WAIT_INDEXES_READY)) {
			throw new IllegalStateException("Conflicting options: "
					+ TextUtils.join(Arrays.asList(Option.INDEXER_DISABLED,
							Option.WAIT_INDEXES_READY), ","));
		}
	}

	protected boolean hasOption(Option option) {
		return options.contains(option);
	}

	@Override
	protected boolean isVerbose() {
		return hasOption(Option.VERBOSE);
	}

	@Override
	protected void before() throws Throwable {
		workspaceSetup.before();
		if (hasOption(Option.INDEXER_DISABLED)) {
			ModelManager.getModelManager().getIndexManager().disable();
		}
		project = createProject(getProjectName());
		if (hasOption(Option.BUILD)) {
			final long start = System.currentTimeMillis();
			buildProject();
			if (isVerbose()) {
				System.out.println((System.currentTimeMillis() - start)
						+ " ms for full build of " + getProjectName()
						+ " project");
			}
		}
		if (hasOption(Option.WAIT_INDEXES_READY)) {
			ModelManager.getModelManager().getIndexManager().waitUntilReady();
		}
	}

	protected IProject createProject(String workspaceProjectName)
			throws IOException, CoreException {
		final File source = getSourceDirectory();
		if (!source.isDirectory()) {
			throw new IllegalStateException(NLS.bind(
					"Source directory \"{0}\" doesn't exist", source));
		}
		final File target = getWorkspaceRoot().getLocation()
				.append(workspaceProjectName).toFile();
		FileUtil.copyDirectory(source, target);
		final IProject project = getWorkspaceRoot().getProject(
				workspaceProjectName);
		ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				project.create(null);
				if (!hasOption(Option.CLOSED)) {
					project.open(null);
				}
			}
		}, null);
		return project;
	}

	public File getSourceDirectory() {
		return new File(workspaceSetup.getSourceWorkspaceDirectory(),
				getSourceProjectName());
	}

	protected void buildProject() throws CoreException {
		get().build(IncrementalProjectBuilder.FULL_BUILD, null);
	}

	@Override
	protected void after() {
		if (project != null) {
			try {
				deleteProject(project);
			} catch (CoreException e) {
				e.printStackTrace();
			}
			project = null;
		}
		try {
			deleteProject(getWorkspaceRoot().getProject(getProjectName()));
		} catch (CoreException e) {
			e.printStackTrace();
		}
		if (hasOption(Option.INDEXER_DISABLED)) {
			ModelManager.getModelManager().getIndexManager().enable();
		}
		workspaceSetup.after();
	}

	private static void deleteProject(IProject project) throws CoreException {
		if (project.exists() && !project.isOpen()) {
			// force opening so that project can be deleted without
			// logging (see bug 23629)
			project.open(null);
		}
		AbstractModelTests.deleteResource(project);
	}

	/**
	 * Returns name of the project in the {@link #workspaceSetup source
	 * workspace}.
	 */
	public String getSourceProjectName() {
		return projectName;
	}

	/**
	 * Returns workspace name of the project.
	 */
	@Override
	public String getProjectName() {
		return projectName;
	}

	/**
	 * Returns this project as IProject
	 */
	@Override
	public IProject get() {
		Assert.assertNotNull("ProjectSetup " + getProjectName()
				+ " not initialized", project);
		return project;
	}

	/**
	 * Creates {@link RuleChain} initializing the specified rules in the
	 * specified order.
	 */
	public static RuleChain chainOf(TestRule first, TestRule second,
			TestRule... rest) {
		RuleChain chain = RuleChain.outerRule(first).around(second);
		for (TestRule rule : rest) {
			chain = chain.around(rule);
		}
		return chain;
	}

}
